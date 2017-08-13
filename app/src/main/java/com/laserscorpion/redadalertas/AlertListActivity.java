package com.laserscorpion.redadalertas;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.R.layout.*;
import android.widget.ListView;

import com.laserscorpion.redadalertas.db.AlertsDatabaseHelper;

import java.util.ArrayList;
import java.util.Date;

public class AlertListActivity extends ListActivity {
    Context context = this;
    ArrayList<Alert> alertList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertChecker checker = new AlertChecker(this);
        checker.downloadAlerts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlertsDatabaseHelper db = new AlertsDatabaseHelper(this);
        alertList = db.getAlertsSince(new Date(0L)); // pretty sure we're not going to deal with alerts before 1970...
        /*for (Alert alert : alerts) {

        }*/
        AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Alert alert = alertList.get(position);
                Intent mapActivity = new Intent(context, AlertMapActivity.class);
                mapActivity.putExtra(Alert.ALERT_EXTRA_NAME, alert);
                startActivity(mapActivity);
            }
        };
        ArrayAdapter<Alert> adapter = new ArrayAdapter<>(this, R.layout.list_item, alertList);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(clickListener);
    }
}
