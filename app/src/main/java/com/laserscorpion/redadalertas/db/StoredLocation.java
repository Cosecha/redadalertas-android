package com.laserscorpion.redadalertas.db;

import android.location.Location;

public class StoredLocation {
    int ID;
    Location location;

    @Override
    public String toString() {
        return location.toString();
    }
}
