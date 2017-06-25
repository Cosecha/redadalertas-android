package com.laserscorpion.redadalertas;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

/**
 * Not intended for use in the final app, just to test if I'm connecting to Tor successfully
 */
public class TorCheckActivity extends AppCompatActivity implements URLDataReceiver {
    //private static final String URL = "https://check.torproject.org/";
    private static final String URL = "https://eff.org/";
    private TorURLLoader loader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tor_check);

        try {
            loader = new TorURLLoader(this, new URL(URL), this);
            loader.start();
        } catch (MalformedURLException e) {
            // let's just not malform the URL, ok?
        }
    }

    @Override
    protected void onDestroy() {
        if (loader != null)
            loader.cancel();
        super.onDestroy();
    }

    @Override
    public void requestComplete(final boolean successful, final String data) {
        loader = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WebView web = (WebView)findViewById(R.id.tor_web_view);
                if (successful) {
                    web.loadData(data, "text/html", "utf-8");
                } else
                    web.loadData("uhhhh failure: " + data, "text/plain", "utf-8");

            }
        });

    }
}
