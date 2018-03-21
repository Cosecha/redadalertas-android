package com.redadalertas.client.adhoctests;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.redadalertas.client.AlertChecker;
import com.redadalertas.client.R;

public class AlertCheckerTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_checker_test);
        AlertChecker checker = new AlertChecker(this);
        checker.downloadAlerts();
    }
}
