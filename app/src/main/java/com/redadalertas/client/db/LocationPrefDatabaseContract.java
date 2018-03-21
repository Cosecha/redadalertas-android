package com.redadalertas.client.db;


import android.provider.BaseColumns;

public class LocationPrefDatabaseContract {

    private LocationPrefDatabaseContract() {}

    public static class LocationPrefs implements BaseColumns {
        public static final String TABLE_NAME = "location_prefs";
        public static final String COLUMN_NAME_LOCATION_ID = "id";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
    }
}
