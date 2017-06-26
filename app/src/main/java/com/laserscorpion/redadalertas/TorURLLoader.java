package com.laserscorpion.redadalertas;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.laserscorpion.redadalertas.apachefix.ApacheResponseFactory;
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.Utilities;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Starts Tor, loads a URL, and stops Tor. A single-use deal. Provide the URL in the constructor
 * then call start(). Call cancel() if the calling URLDataReceiver need to die before the request is
 * complete.
 *
 * This avoids having to worry about whether we should keep Tor running in the background and maintaining
 * an active control connection to it via Thali.
 * But it is SLOW. We (maybe) have to download directory information and (definitely) build
 * fresh circuits every time.
 *
 * ALSO it is not super robust. Stopping and starting Tor seems like a slightly dicey operation. Trying
 * to start a new instance while one is running will fail, so we need to have a pretty good idea that
 * old calls are done before making a new request.
 * It might be safer to avoid starting and stopping Tor over and over if we need to use it frequently.
 * Perhaps we could consider making this reusable later, or splitting out the Tor handling and
 * the HTTP bits. This could be a good place for a Service, if we expect to use Tor very often.
 * For now, we don't expect to download data very often, and must trust ourselves to avoid
 * using it wrong.
 *
 * Note: assumes response will be in utf-8. If it's not, please retire your 90s web server (actually
 * don't, I want to visit it).
 */
public class TorURLLoader extends Thread {
    private static final String APP_NAME = "com.laserscorpion.redadalertas";
    private static final String CRLF = "\r\n";
    private static final String CHARSET = "utf-8"; // the only one you may ever use for anything ever again, says me
    private String version = "*";
    private String fileStorageLocation = "torfiles";
    private Context context;
    private static final String TAG = "TorURLLoader";
    private AndroidOnionProxyManager manager;
    private int redirectsRemaining = 10;
    private URL originalUrl;
    private URL previousUrl;
    private URL url; // the one we are currently fetching
    private URLDataReceiver receiver;
    private boolean started = false;
    private boolean starting = false;
    private SSLSocket socket;

     /**
      * After constructing this new URL loader, don't forget to call start()
     * @param context
     * @param url the one you want to load, you know?
     * @param receiver since all this networking is slow, you'll need to wait for your response, so
     *                 implement this to listen for it
     */
    TorURLLoader(Context context, URL url, URLDataReceiver receiver) {
        this.context = context;
        this.originalUrl = url;
        this.url = url;
        this.previousUrl = url;
        this.receiver = receiver;
        manager = new AndroidOnionProxyManager(context, fileStorageLocation);
        starting = true;
        TorStartThread thread = new TorStartThread(this);
        thread.start();
        getVersion();
    }

    public void run() {
        try {

            try {
                waitForTor();
            } catch (SocketException e) {
                throw new Exception(context.getString(R.string.tor_start_error), e);
            }

            try {
                socket = connectToServerViaTor(url);
                createSendAndHandleRequest();
            } catch (SocketTimeoutException e) {
                throw new Exception(context.getString(R.string.timeout_error), e);
            } catch (SocketException e) { // connectToServerViaTor()
                throw new Exception(context.getString(R.string.server_connection_error), e);
            } catch (HttpException e) {
                throw new Exception(context.getString(R.string.http_error), e);
            } catch (IOException e) { // createSendAndHandleRequest() / readStream()
                throw new Exception(context.getString(R.string.network_download_error), e);
            }
        } catch (final Exception e) {
            e. printStackTrace();
            receiver.requestComplete(false, e.getMessage());
        }

        stopTor();
    }

    private void createSendAndHandleRequest() throws SocketTimeoutException, HttpException, IOException {
        String request = createRequest(url);
        sendRequest(socket, request);
        String result = readStream(socket);
        HttpResponse response = ApacheResponseFactory.parse(result);
        handleResponse(response);
    }

    private void handleResponse(HttpResponse response) throws IOException, HttpException {
        int statusCode = response.getStatusLine().getStatusCode();
        Log.d(TAG, "Received an HTTP response: " + statusCode);
        if (statusCode < 200) {
            Log.d(TAG, "Received an HTTP status code we don't care about: " + statusCode);
            return;
        } else if (statusCode >= 200 && statusCode < 300) {
            try {
                String responseBody = EntityUtils.toString(response.getEntity());
                receiver.requestComplete(true, responseBody); // this is the normal case
            } catch (IOException e) {
                throw new IOException("IOExceptions are extremely nonspecific in this context. The request completed but some unknown thing failed. Have fun!", e);
            }
        } else if (statusCode >= 300 && statusCode < 400) {
            if (statusCode != org.apache.http.HttpStatus.SC_USE_PROXY) {
                handleRedirect(response);
            } else
                throw new HttpException("ignoring proxy instruction, who knows what is going on");
        } else {
            throw new HttpException("HTTP request failed! Status: " + statusCode);
        }
    }

    /**
     * Note that this is ultimately called recursively, but terminates when redirectsRemaining == 0
     * @param response
     * @throws IOException sometimes? Hopefully not
     */
    private void handleRedirect(HttpResponse response) throws IOException, HttpException {
        Log.d(TAG, "Handling redirect");
        if (redirectsRemaining > 0) {
            redirectsRemaining--;
            Header location = response.getFirstHeader(HttpHeaders.LOCATION);
            if (location == null)
                throw new HttpException("Missing redirect");
            URL redirect = new URL(location.getValue());
            if (!url.equals(redirect) && !previousUrl.equals(redirect) && !originalUrl.equals(redirect)) {
                previousUrl = url;
                url = redirect;
                socket.close();
                socket = connectToServerViaTor(url);
                createSendAndHandleRequest();
            } else
                throw new HttpException("Circular redirect");
        } else
            throw new HttpException("Too many redirects");

    }

    private void stopTor() {
        Log.d(TAG, "stopping Tor, hope you're done!");
        try {
            manager.stop();
        } catch (IOException e) {
            // this is bad. if we can't stop it now, we can't re-start again later
            Log.e(TAG, "Uh oh, can't stop Tor.");
            e.printStackTrace();
        }
    }

    /**
     * temporary, just to test...actually wait, maybe this is all we actually need ugh
     * can we really get away with only writing this much HTTP?
     */
    private String createRequest(URL url) {
        /* Using HTTP 1.0 here to simplify detecting the end of the response, since we are doing this
            ourselves, not via some fancy HTTP library. If you switch to HTTP 1.1, you will need to change
            readStream() to understand when a single reply is done, since it can't just detect that the
            stream is closed. */
        // todo handle lack of '/' in file, if any
        String request = "GET " + url.getFile() + " HTTP/1.0"  + CRLF;
        request += "Host: " + url.getHost() + CRLF;
        request += "User-Agent: " + context.getString(R.string.user_agent) + " " + version + CRLF;
        request += CRLF;
        return request;
    }

    private void getVersion() {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(APP_NAME, 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG,"Did we change the package name?");
            e.printStackTrace();
        }
    }

    /*private HttpRequest createRequest(URL url) {
        HttpRequestFactory factory = new DefaultHttpRequestFactory();
        try {
            return factory.newHttpRequest("GET", url.toString());
        } catch (MethodNotSupportedException e) {
            return null; // never called
        }
    }*/

    private void sendRequest(SSLSocket socket, String request) {
        try {
            OutputStreamWriter writer;
            writer = new OutputStreamWriter(socket.getOutputStream(), CHARSET);
            writer.write(request);
            writer.flush();
            Log.d(TAG, "Sent request: " + request);
        } catch (IOException e) {
            // todo why would this happen?
            // is there any scenario in which the socket would be closed
            e.printStackTrace();
            return;
        }
    }

    /**
     * Must call this if a request is outstanding and the listener needs to die.
     */
    public void cancel() {
        stopTor();
    }

    private String readStream(Socket socket) throws SocketTimeoutException, IOException {
        InputStreamReader stream = new InputStreamReader(socket.getInputStream(), CHARSET);
        BufferedReader reader = new BufferedReader(stream);
        String result = "";
        char buf[] = new char[100];
        int read = reader.read(buf, 0, buf.length);
        while (read >= 0) {
            String justRead = new String(buf, 0, read);
            //Log.d(TAG, "read: " + justRead);
            result += justRead;
            read = reader.read(buf, 0, buf.length);
        }

        return result;
    }

    private SSLSocket connectToServerViaTor(URL url) throws SocketException {
        Socket socket;
        int port;
        try {
            port = manager.getIPv4LocalHostSocksPort();
            Log.d(TAG, "Found SOCKS port: " + port);
        } catch (IOException e) {
            throw new SocketException("can't find the SOCKS port!");
        }
        try {
            socket = Utilities.socks4aSocketConnection(url.getHost(), 443, "127.0.0.1", port);
        } catch (IOException e) {
            throw new ConnectException("can't open SOCKS 4a socket");
        }

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try {
            return (SSLSocket) factory.createSocket(socket, url.getHost(), 443, true);
        } catch (IOException e) {
            throw new ConnectException("can't convert socket to SSL");
        }
    }

    private synchronized void waitForTor() throws SocketException {
        while (starting) {
            try {
                wait();
                break;
            } catch (InterruptedException e) { }
        }
        if (!started)
            throw new SocketException("Couldn't connect to Tor.");
    }

    /**
     * We could also think about using a thread pool. Java has some such kind of thing. And then there's
     * always AsyncTask, but I don't really like its interface.
     */
    private class TorStartThread extends Thread {
        private static final int totalSecondsPerTorStartup = 4 * 60;
        private static final int totalTriesPerTorStartup = 5;
        private TorURLLoader parent;

        public TorStartThread(TorURLLoader parent) {
            this.parent = parent;
        }

        public void run() {
            boolean failedOnce = false;

            while (true) {
                if (!haveInternetConnection())
                    break;
                try {
                    synchronized (parent) {
                        if (manager.isRunning())
                            started = true;
                        else
                            started = manager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
                        break;
                    }
                } catch (InterruptedException e) {
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    if (failedOnce) // try twice, give up if IOException a second time
                        break;
                    failedOnce = true;
                }
            }
            synchronized (parent) {
                starting = false;
                parent.notify();
            }
        }

        private boolean haveInternetConnection() {
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

}
