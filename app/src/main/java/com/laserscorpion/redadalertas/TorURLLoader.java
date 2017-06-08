package com.laserscorpion.redadalertas;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.util.Log;

import com.laserscorpion.redadalertas.apachefix.RequestPrinter;
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.Utilities;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.impl.DefaultHttpRequestFactory;

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

public class TorURLLoader extends Thread {
    private static final String APP_NAME = "com.laserscorpion.redadalertas";
    private static final String CRLF = "\r\n";
    private String version = "*";
    private String fileStorageLocation = "torfiles";
    private Context context;
    private static final String TAG = "TorURLLoader";
    private AndroidOnionProxyManager manager;
    private URL url;
    private URLDataReceiver receiver;
    private boolean started = false;

    /**
     * Starts Tor, loads a URL, and stops Tor. A single-use deal.
     *
     * This avoids having to worry about
     * whether we should keep Tor running in the background, or running multiple instances by accident.
     * But it is SLOW. We have to download directory information and build fresh circuits every time.
     *
     * ALSO it is not super robust. For example, when using TorCheckActivity, pause and resume the app
     * while still loading the URL the first time. The second Tor instance fails to start and everything
     * gets messed up from then on.
     *
     * Perhaps we could consider making this reusable later, or splitting out the Tor handling and
     * the HTTP. For now, we don't expect to download data very often, and must trust ourselves to avoid
     * using it wrong.
     * @param context
     * @param url
     * @param receiver since all this networking is slow, you'll need to wait for your response, so listen here
     */
    TorURLLoader(Context context, URL url, URLDataReceiver receiver) {
        this.context = context;
        manager = new AndroidOnionProxyManager(context, fileStorageLocation);
        TorStartThread thread = new TorStartThread(this);
        thread.start();
        getVersion();
        this.url = url;
        this.receiver = receiver;
    }

    public void loadURL() throws SocketException {

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
                receiver.requestComplete(true, result);
            } catch (SocketException e) { // connectToServerWithTor()
                throw new Exception(context.getString(R.string.server_connection_error), e);
            } catch (IOException e) { // readStream()
                throw new Exception(context.getString(R.string.network_download_error), e);
            }
        } catch (Exception e) {
            receiver.requestComplete(false, e.getMessage());
        }

    }

    /**
     * temporary, just to test...actually wait, maybe this is all we actually need ugh
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
            Log.e("RequestPrinter","Did we change the package name?");
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
            writer = new OutputStreamWriter(socket.getOutputStream(), "utf-8");
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
            stream = new InputStreamReader(socket.getInputStream(), "utf-8");
        } catch (IOException e) {
            // todo is this actually possible since we just set up this socket?
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(stream);
        String result = "";
        String line = null;
        do {
            line = reader.readLine();
            result += line;
        } while(line != null);

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
