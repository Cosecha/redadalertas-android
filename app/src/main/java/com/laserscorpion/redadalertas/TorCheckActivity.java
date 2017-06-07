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
        TorURLLoader loader = new TorURLLoader(this);
        try {
            loader.loadURL(new URL("https://check.torproject.org"), this);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // let's just not malform the URL, ok?
        }
    }

    @Override
    public void requestComplete(boolean successful, String data) {
        WebView web = (WebView)findViewById(R.id.tor_web_view);
        if (successful) {
            web.loadData(data, "text/html", "utf-8");
        } else
            web.loadData("uhhhh failure", "text/plain", "utf-8");

    }
}
