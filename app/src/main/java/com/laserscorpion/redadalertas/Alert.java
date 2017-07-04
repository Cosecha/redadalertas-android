package com.laserscorpion.redadalertas;

import android.location.Location;

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
}
