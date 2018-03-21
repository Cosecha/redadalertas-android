package com.redadalertas.client.adhoctests;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

import com.redadalertas.client.R;
import com.redadalertas.client.TorURLLoader;
import com.redadalertas.client.URLDataReceiver;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Not intended for use in the final app, just to test if I'm connecting to Tor successfully
 */
public class TorCheckActivity extends AppCompatActivity implements URLDataReceiver {
    private static final String URL = "https://check.torproject.org/"; // tests Tor
    //private static final String URL = "https://eff.org/"; // tests redirect
    //private static final String URL = "https://eff.xjf/"; // tests (one type of) failure
    //private static final String URL = "https://laserscorpion.com/other/example.json";

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
