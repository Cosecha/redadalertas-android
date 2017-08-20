package com.laserscorpion.redadalertas;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.laserscorpion.redadalertas.db.LocationPrefDatabaseHelper;
import com.laserscorpion.redadalertas.db.StoredLocation;

import java.util.ArrayList;
import com.google.android.gms.location.places.ui.PlacePicker;


public class LocationListActivity extends AppCompatActivity {
    private static final String TAG = "LocationListActivity";
    private static final int PLACE_PICKER_REQUEST = 22919; // some arbitrary number I chose
    private Activity activity = this;
    private LocationPrefDatabaseHelper db;
    private ArrayAdapter<StoredLocation> adapter;
    private ArrayList<StoredLocation> savedLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.location_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new LocationPrefDatabaseHelper(this);
        savedLocations = db.getLocations();
        adapter = new ArrayAdapter<StoredLocation>(this, R.layout.location_list_item, savedLocations);
        ListView list = (ListView)findViewById(R.id.location_listview);
        list.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            list.setNestedScrollingEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_location, menu);
        return true;
    }

    private void updateList() {
        //savedLocations = db.getLocations();
        ArrayList<StoredLocation> currentLocations = db.getLocations();
        ArrayList<StoredLocation> newLocations = new ArrayList<>(currentLocations);
        newLocations.removeAll(savedLocations);
        adapter.addAll(newLocations);
        adapter.notifyDataSetChanged();
        savedLocations = currentLocations;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "got the location, probably");
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                if (place != null) {
                    LatLng latlng = place.getLatLng();
                    Location location = new Location("");
                    location.setLatitude(latlng.latitude);
                    location.setLongitude(latlng.longitude);
                    db.addLocation(location);
                    updateList();
                }
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_location) {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            try {
                Intent placePickerIntent = builder.build(activity);
                startActivityForResult(placePickerIntent, PLACE_PICKER_REQUEST);
            } catch (GooglePlayServicesRepairableException e) {
                int statusCode = e.getConnectionStatusCode();
                Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(activity, statusCode, PLACE_PICKER_REQUEST);
                errorDialog.show();
            } catch (GooglePlayServicesNotAvailableException e) {
                ErrorDialog dialog = ErrorDialog.newInstance(getString(R.string.gms_not_available_message));
                dialog.show(getFragmentManager(), null);
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
