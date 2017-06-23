package com.laserscorpion.redadalertas;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.laserscorpion.redadalertas.apachefix.ApacheResponseFactory;
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.Utilities;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Starts Tor, loads a URL, and stops Tor. A single-use deal. Provide the URL in the constructor
 * then call start().
 *
 * This avoids having to worry about whether we should keep Tor running in the background, or
 * running multiple instances by accident.
 * But it is SLOW. We (maybe) have to download directory information and (definitely) build
 * fresh circuits every time.
 *
 * ALSO it is not super robust. For example, when using TorCheckActivity, try to pause and resume
 * the app while still loading the URL the first time (or change the orientation).
 * The second Tor instance fails to start and everything gets messed up from then on.
 *
 * Perhaps we could consider making this reusable later, or splitting out the Tor handling and
 * the HTTP. For now, we don't expect to download data very often, and must trust ourselves to avoid
 * using it wrong.
 *
 * Note: assumes response will be in utf-8. If it's not, please retire your 90s web server.
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
    private URL url;
    private URLDataReceiver receiver;
    private boolean started = false;

     /**
      * After constructing this new URL loader, don't forget to call start()
     * @param context
     * @param url the one you want to load, you know?
     * @param receiver since all this networking is slow, you'll need to wait for your response, so
     *                 implement this to listen for it
     */
    TorURLLoader(Context context, URL url, URLDataReceiver receiver) {
        this.context = context;
        this.url = url;
        this.receiver = receiver;
        manager = new AndroidOnionProxyManager(context, fileStorageLocation);
        TorStartThread thread = new TorStartThread(this);
        thread.start();
        getVersion();
    }

    public void run() {
        try {
            String request = createRequest(url);
            try {
                waitForTor();
            } catch (SocketException e) {
                throw new Exception(context.getString(R.string.tor_start_error), e);
            }

            try {
                SSLSocket socket = connectToServerViaTor(url);
                sendRequest(socket, request);
                String result = readStream(socket);
                HttpResponse response = ApacheResponseFactory.parse(result);
                handleResponse(response);
            } catch (SocketException e) { // connectToServerWithTor()
                throw new Exception(context.getString(R.string.server_connection_error), e);
            } catch (IOException e) { // readStream()
                throw new Exception(context.getString(R.string.network_download_error), e);
            }
        } catch (final Exception e) {
            receiver.requestComplete(false, e.getMessage());
        }

        try {
            manager.stop();
        } catch (IOException e) {
            // this is bad. if we can't stop it now, we can't re-start again later
            Log.e(TAG, "Uh oh, can't stop Tor.");
            e.printStackTrace();
        }
    }

    private void handleResponse(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 200) {
            Log.d(TAG, "Received an HTTP status code we don't care about: " + statusCode);
            return;
        } else if (statusCode >= 200 && statusCode < 300) {
            try {
                String responseBody = EntityUtils.toString(response.getEntity());
                receiver.requestComplete(true, responseBody);
            } catch (IOException e) {
                Log.e(TAG, "IOExceptions are extremely nonspecific. The request completed but some unknown thing failed. Have fun!");
                e.printStackTrace();
                receiver.requestComplete(false, null);
            }
        } else if (statusCode >= 300 && statusCode < 400) {
            // !!!!!!!!!!!!!!!!!!!!!!!!!
            // !!!!!!!!!!!!!!!!!!!!!!!!!
            // todo we probably do want to handle redirects!
            // !!!!!!!!!!!!!!!!!!!!!!!!!
            // !!!!!!!!!!!!!!!!!!!!!!!!!
        } else {
            Log.e(TAG, "HTTP request failed! Status: " + statusCode);
            receiver.requestComplete(false, null);
        }

    }

    /**
     * temporary, just to test...actually wait, maybe this is all we actually need ugh
     * can we really get away with only writing this much HTTP?
     */
    private String createRequest(URL url) {
        String request = "GET /" + url.getFile() + " HTTP/1.1"  + CRLF;
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
        } catch (IOException e) {
            // todo why would this happen?
            // is there any scenario in which the socket would be closed
            e.printStackTrace();
            return;
        }
    }

    private String readStream(Socket socket) throws IOException {
        InputStreamReader stream = null;
        try {
            stream = new InputStreamReader(socket.getInputStream(), CHARSET);
        } catch (IOException e) {
            // todo is this actually possible since we just set up this socket?
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(stream);
        String result = "";
        char buf[] = new char[100];
        int read = reader.read(buf, 0, buf.length);
        while (read >= 0) {
            String justRead = new String(buf, 0, read);
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
        while (!started) {
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
                try {
                    if (manager.isRunning())
                        started = true;
                    else
                        started = manager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
                    break;
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
                parent.notify();
            }
        }
    }

}
