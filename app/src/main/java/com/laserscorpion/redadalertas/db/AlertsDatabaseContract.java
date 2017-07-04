package com.laserscorpion.redadalertas.db;


import android.provider.BaseColumns;

public final class AlertsDatabaseContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private AlertsDatabaseContract() {}

    /* Inner class that defines the table contents */
    public static class Alerts implements BaseColumns {
        public static final String TABLE_NAME = "alerts";
        public static final String COLUMN_NAME_ALERT_ID = "id";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_LOCATION = "location";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_AGENCY = "agency";
    }

    public static class Agencies implements BaseColumns {
        public static final String TABLE_NAME = "agencies";
        public static final String COLUMN_NAME_AGENCY_ID = "id";
        public static final String COLUMN_NAME_AGENCY_NAME = "name";
    }

    public static class AlertTypes implements BaseColumns {
        public static final String TABLE_NAME = "types";
        public static final String COLUMN_NAME_ALERT_TYPE_ID = "id";
        public static final String COLUMN_NAME_ALERT_TYPE_NAME = "name";
    }
}
