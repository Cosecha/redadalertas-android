package com.laserscorpion.redadalertas;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class AlertMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Alert alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent creationIntent = getIntent();
        alert = (Alert)creationIntent.getSerializableExtra(NotificationFactory.ALERT_EXTRA_NAME);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng dangerLocation = new LatLng(alert.location.getLatitude(), alert.location.getLongitude());

        String eventType = alert.getEventType(this);

        mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(dangerLocation));
        mMap.addMarker(new MarkerOptions().position(dangerLocation).title(eventType));

        TextView alertHeading = (TextView)findViewById(R.id.alert_type_heading);
        alertHeading.setText(eventType);

        TextView timeField = (TextView)findViewById(R.id.raid_time_cell);
        timeField.setText(alert.time.toString());

        TextView agencyField = (TextView)findViewById(R.id.raid_agency_cell);
        String agency = alert.getAgency(this);
        agencyField.setText(agency);
    }


}
