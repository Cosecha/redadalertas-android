package com.laserscorpion.redadalertas.db;

import android.location.Location;

public class StoredLocation {
    public int ID;
    public Location location;

    @Override
    public String toString() {
        // todo: this should be translatable
        return location.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof StoredLocation) )
            return false;
        StoredLocation other = (StoredLocation)obj;
        Location otherLoc = other.location;
        if (ID != other.ID)
            return false;
        return otherLoc.getLongitude() == location.getLongitude() && otherLoc.getLatitude() == location.getLatitude();
    }

    //todo: implement HashCode() too
}
