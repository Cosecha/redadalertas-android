package com.laserscorpion.redadalertas;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.laserscorpion.redadalertas.db.AlertsDatabaseHelper;

import java.util.ArrayList;
import java.util.Date;

public class AlertListActivity extends AppCompatActivity {
    Context context = this;
    ArrayList<Alert> alertList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*Toolbar toolbar = (Toolbar) findViewById(R.id.alert_toolbar);
        setSupportActionBar(toolbar);*/
        getSupportActionBar().setTitle(getString(R.string.alert_list_title));
        setContentView(R.layout.activity_alert_list);

        Intent creationIntent = getIntent();
        String message = creationIntent.getStringExtra(NotificationFactory.ERROR_TEXT_EXTRA_NAME);
        if (message != null) {
            ErrorDialog dialog = ErrorDialog.newInstance(message);
            dialog.show(getFragmentManager(), null);
        } else {
            AlertChecker checker = new AlertChecker(this);
            checker.downloadAlerts();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlertsDatabaseHelper db = new AlertsDatabaseHelper(this);
        alertList = db.getAlertsSince(new Date(0L)); // pretty sure we're not going to deal with alerts before 1970...
        for (Alert alert : alertList) {
            if (!alert.isOfInterest(this))
                alertList.remove(alert);
        }
        AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Alert alert = alertList.get(position);
                Intent mapActivity = new Intent(context, AlertMapActivity.class);
                mapActivity.putExtra(Alert.ALERT_EXTRA_NAME, alert);
                startActivity(mapActivity);
            }
        };
        ArrayAdapter<Alert> adapter = new ArrayAdapter<>(this, R.layout.list_item, alertList);

        ListView alertList = (ListView)findViewById(R.id.alert_listview);
        alertList.setAdapter(adapter);
        alertList.setOnItemClickListener(clickListener);

        //setListAdapter(adapter);
        //getListView().setOnItemClickListener(clickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_alert_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_open_settings) {
            Intent prefsActivity = new Intent(this, LocationListActivity.class);
            startActivity(prefsActivity);
        }
        return super.onOptionsItemSelected(item);
    }



}
