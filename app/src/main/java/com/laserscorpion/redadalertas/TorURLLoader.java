package com.laserscorpion.redadalertas;

import android.content.Context;
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

public class TorURLLoader {
    private String fileStorageLocation = "torfiles";
    private Context context;
    private static final String TAG = "TorURLLoader";
    private AndroidOnionProxyManager manager;
    private boolean started = false;



    TorURLLoader(Context context) {
        this.context = context;
        manager = new AndroidOnionProxyManager(context, fileStorageLocation);
        TorStartThread thread = new TorStartThread(this);
        thread.start();
    }

    public void loadURL(URL url, URLDataReceiver receiver) throws SocketException {
        HttpRequest request = createRequest(url);
        Log.d(TAG, "***************");
        Log.d(TAG, new RequestPrinter(context).print(request));
        Log.d(TAG, "***************");

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX); // FIXME
        waitForTor();
        SSLSocket socket = connectToServerViaTor(url);

        sendRequest(socket, request);
        String result = readStream(socket);
        Log.d(TAG, result);
        receiver.requestComplete(true, result);
    }

    private HttpRequest createRequest(URL url) {
        HttpRequestFactory factory = new DefaultHttpRequestFactory();
        try {
            return factory.newHttpRequest("GET", url.toString());
        } catch (MethodNotSupportedException e) {
            return null; // never called
        }
    }

    private void sendRequest(SSLSocket socket, HttpRequest request) {
        try {
            OutputStreamWriter writer;
            writer = new OutputStreamWriter(socket.getOutputStream(), "utf-8");
            writer.write(request.toString());
            writer.flush();
        } catch (IOException e) {
            // todo why would this happen?
            // is there any scenario in which the socket would be closed
            e.printStackTrace();
            return;
        }
    }

    private String readStream(Socket socket) {
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
            try {
                line = reader.readLine();
                result += line;
            } catch (IOException e) {
                Log.e(TAG, "Error reading stream");
                e.printStackTrace();
            }
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

    private class HttpThread extends Thread {

        public HttpThread() {

        }
    }
}
