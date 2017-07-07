package com.laserscorpion.redadalertas;

import android.location.Location;

import com.laserscorpion.redadalertas.db.AlertsDatabaseContract;

import java.util.Date;

public class Alert {
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
}
