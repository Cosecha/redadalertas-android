package com.laserscorpion.redadalertas;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

public class TorCheckActivity extends AppCompatActivity implements URLDataReceiver {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tor_check);
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            TorURLLoader loader = new TorURLLoader(this, new URL("https://check.torproject.org"), this);
            loader.start();
        } catch (MalformedURLException e) {
            // let's just not malform the URL, ok?
        }

    }

    @Override
    public void requestComplete(final boolean successful, final String data) {
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
