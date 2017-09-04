package com.laserscorpion.redadalertas;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.laserscorpion.redadalertas.db.AlertsDatabaseContract;
import com.laserscorpion.redadalertas.db.LocationPrefDatabaseHelper;
import com.laserscorpion.redadalertas.db.StoredLocation;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class Alert implements Serializable {
    public static final String ALERT_EXTRA_NAME = "com.laserscorpion.redadalertas.Alert";
    public static final float ALERT_DISTANCE_METERS = 2000;

    public enum Agency {
        ICE, CBP, LOCAL_POLICE, LOCAL_SHERIFF
    }

    public enum AlertType {
        RAID, CHECKPOINT
    }

    public int ID;
    public Date time;
    public Location location;
    public AlertType type;
    public Agency agency;

    @Override
    public String toString() {
        if (time == null || location == null || type == null || agency == null)
            return null;
        return time + ": " + agency + " " + type + " at (" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(ID);
        out.writeObject(time);
        out.writeDouble(location.getLongitude());
        out.writeDouble(location.getLatitude());
        out.writeObject(type);
        out.writeObject(agency);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        ID = in.readInt();
        time = (Date)in.readObject();

        double longitude = in.readDouble();
        double latitude = in.readDouble();
        location = new Location("");
        location.setLongitude(longitude);
        location.setLatitude(latitude);

        type = (AlertType) in.readObject();
        agency = (Agency) in.readObject();
    }

    public String getEventType(Context context) {
        String raid = context.getString(R.string.event_type_raid);
        String checkpoint = context.getString(R.string.event_type_checkpoint);
        if (type == AlertType.RAID)
            return raid;
        return checkpoint;
    }

    public String getAgency(Context context) {
        switch (agency) {
            case ICE:
                return context.getString(R.string.agency_name_ICE);
            case CBP:
                return context.getString(R.string.agency_name_CBP);
            case LOCAL_POLICE:
                return context.getString(R.string.agency_name_local_police);
            case LOCAL_SHERIFF:
                return context.getString(R.string.agency_name_local_sheriff);
            default:
                return context.getString(R.string.agency_name_unknown);
        }
    }

    public boolean isOfInterest(Context context) {
        LocationPrefDatabaseHelper db = new LocationPrefDatabaseHelper(context);
        ArrayList<StoredLocation> locations = db.getLocations();
        for (StoredLocation loc : locations) {
            if (location.distanceTo(loc.location) < ALERT_DISTANCE_METERS)
                return true;
        }
        return false;
    }
}
