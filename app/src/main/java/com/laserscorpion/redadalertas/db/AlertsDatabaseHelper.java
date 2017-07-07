package com.laserscorpion.redadalertas.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.AsyncTask;

import com.laserscorpion.redadalertas.Alert;

import java.util.ArrayList;
import java.util.Date;


public class AlertsDatabaseHelper extends SQLiteOpenHelper {
    private static final String LAT_LONG_SEP = ", ";
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Alerts.db";
    private static final String SQL_CREATE_ALERTS =
            "CREATE TABLE " + AlertsDatabaseContract.Alerts.TABLE_NAME + " (" +
                    AlertsDatabaseContract.Alerts.COLUMN_NAME_ALERT_ID + " INTEGER PRIMARY KEY," +
                    AlertsDatabaseContract.Alerts.COLUMN_NAME_TIME + " INTEGER," +
                    AlertsDatabaseContract.Alerts.COLUMN_NAME_LOCATION + " TEXT," +
                    AlertsDatabaseContract.Alerts.COLUMN_NAME_AGENCY + " INTEGER, " +
                    AlertsDatabaseContract.Alerts.COLUMN_NAME_TYPE + " INTEGER, FOREIGN KEY(" +
                    AlertsDatabaseContract.Alerts.COLUMN_NAME_AGENCY + ") REFERENCES " +
                    AlertsDatabaseContract.Agencies.TABLE_NAME + "(" +
                    AlertsDatabaseContract.Agencies.COLUMN_NAME_AGENCY_ID + ")," + " FOREIGN KEY(" +
                    AlertsDatabaseContract.Alerts.COLUMN_NAME_TYPE + ") REFERENCES " +
                    AlertsDatabaseContract.AlertTypes.TABLE_NAME + "(" +
                    AlertsDatabaseContract.AlertTypes.COLUMN_NAME_ALERT_TYPE_ID + ") )";
    private static final String SQL_CREATE_AGENCIES =
            "CREATE TABLE " + AlertsDatabaseContract.Agencies.TABLE_NAME + " (" +
                    AlertsDatabaseContract.Agencies.COLUMN_NAME_AGENCY_ID + " INTEGER PRIMARY KEY," +
                    AlertsDatabaseContract.Agencies.COLUMN_NAME_AGENCY_NAME + " TEXT)";
    private static final String SQL_CREATE_TYPES =
            "CREATE TABLE " + AlertsDatabaseContract.AlertTypes.TABLE_NAME + " (" +
                    AlertsDatabaseContract.AlertTypes.COLUMN_NAME_ALERT_TYPE_ID + " INTEGER PRIMARY KEY," +
                    AlertsDatabaseContract.AlertTypes.COLUMN_NAME_ALERT_TYPE_NAME + " TEXT)";
    private int insertsPending = 0; // reinventing the semaphore via the monitor =P

    public AlertsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ALERTS);
        createAgencyTable(db);
        createTypeTable(db);
    }

    private void createAgencyTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_AGENCIES);
        for (Alert.Agency agency : Alert.Agency.values()) {
            ContentValues values = new ContentValues();
            values.put(AlertsDatabaseContract.Agencies.COLUMN_NAME_AGENCY_ID, agency.ordinal());
            values.put(AlertsDatabaseContract.Agencies.COLUMN_NAME_AGENCY_NAME, agency.name());
            db.insert(AlertsDatabaseContract.Agencies.TABLE_NAME, null, values);
        }
    }

    private void createTypeTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TYPES);
        for (Alert.AlertType type : Alert.AlertType.values()) {
            ContentValues values = new ContentValues();
            values.put(AlertsDatabaseContract.AlertTypes.COLUMN_NAME_ALERT_TYPE_ID, type.ordinal());
            values.put(AlertsDatabaseContract.AlertTypes.COLUMN_NAME_ALERT_TYPE_NAME, type.name());
            db.insert(AlertsDatabaseContract.AlertTypes.TABLE_NAME, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * Test logic in production, oops
     */
    public void reset() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE " + AlertsDatabaseContract.Alerts.TABLE_NAME);
        db.execSQL("DROP TABLE " + AlertsDatabaseContract.AlertTypes.TABLE_NAME);
        db.execSQL("DROP TABLE " + AlertsDatabaseContract.Agencies.TABLE_NAME);
        onCreate(db);
    }

    /**
     * Like it says, get the alerts since the given time.
     * Synchronous. You don't really expect me to set up some callback listener for each query do you?
     * @param time since this time, see?
     * @return all the alerts since the cutoff time, in all locations
     */
    public synchronized ArrayList<Alert> getAlertsSince(Date time) {
        waitForInsert();
        SQLiteDatabase db = getReadableDatabase();
        long cutoffTimeUnixUTC = time.getTime();
        /*String sql = "SELECT * FROM "   + AlertsDatabaseContract.Alerts.TABLE_NAME + " LEFT JOIN " +
                AlertsDatabaseContract.AlertTypes.TABLE_NAME + " ON " +
                AlertsDatabaseContract.Alerts.COLUMN_NAME_TYPE + "=" + AlertsDatabaseContract.AlertTypes.COLUMN_NAME_ALERT_TYPE_ID
                + " LEFT JOIN " + AlertsDatabaseContract.Agencies.TABLE_NAME + " ON " +
                AlertsDatabaseContract.Alerts.COLUMN_NAME_AGENCY + "=" + AlertsDatabaseContract.Agencies.COLUMN_NAME_AGENCY_ID +
                " WHERE " + AlertsDatabaseContract.Alerts.COLUMN_NAME_TIME + " >= " + cutoffTimeUnixUTC;
        Cursor results = db.rawQuery(sql, null);*/
        String where = AlertsDatabaseContract.Alerts.COLUMN_NAME_TIME + " >= " + cutoffTimeUnixUTC;
        Cursor results = db.query(AlertsDatabaseContract.Alerts.TABLE_NAME, null, where, null, null, null,
                AlertsDatabaseContract.Alerts.COLUMN_NAME_TIME);
        ArrayList<Alert> alerts = new ArrayList<>();
        while (results.moveToNext()) {
            Alert alert = createAlert(results);
            alerts.add(alert);
        }
        results.close();
        return alerts;
    }

    public synchronized long getMostRecentAlertTime() {
        waitForInsert();
        long time = Long.MIN_VALUE;
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT MAX(" + AlertsDatabaseContract.Alerts.COLUMN_NAME_TIME + ") FROM " + AlertsDatabaseContract.Alerts.TABLE_NAME;
        Cursor result = db.rawQuery(sql, null);
        if (result.moveToFirst())
            time = result.getLong(0);
        result.close();
        return time;
    }

    public synchronized long getHighestID() {
        waitForInsert();
        long time = Long.MIN_VALUE;
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT MAX(" + AlertsDatabaseContract.Alerts.COLUMN_NAME_ALERT_ID + ") FROM " + AlertsDatabaseContract.Alerts.TABLE_NAME;
        Cursor result = db.rawQuery(sql, null);
        if (result.moveToFirst())
            time = result.getInt(0);
        result.close();
        return time;
    }

    private synchronized void waitForInsert() {
        while (insertsPending > 0) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
    }

    private Alert createAlert(Cursor result) {
        int id = result.getInt(result.getColumnIndex(AlertsDatabaseContract.Alerts.COLUMN_NAME_ALERT_ID));
        long alertMillis = result.getLong(result.getColumnIndex(AlertsDatabaseContract.Alerts.COLUMN_NAME_TIME));
        int t = result.getInt(result.getColumnIndex(AlertsDatabaseContract.Alerts.COLUMN_NAME_TYPE));
        Alert.AlertType type = Alert.AlertType.values()[t];
        int a = result.getInt(result.getColumnIndex(AlertsDatabaseContract.Alerts.COLUMN_NAME_AGENCY));
        Alert.Agency agency = Alert.Agency.values()[a];
        String latLong = result.getString(result.getColumnIndex(AlertsDatabaseContract.Alerts.COLUMN_NAME_LOCATION));
        Location location = parseLocation(latLong);

        Alert alert = new Alert();
        alert.ID = id;
        alert.time = new Date(alertMillis);
        alert.agency = agency;
        alert.type = type;
        alert.location = location;

        return alert;
    }

    private Location parseLocation(String latLong) {
        String parts[] = latLong.split(LAT_LONG_SEP);
        double latitude = Double.valueOf(parts[0]);
        double longitude = Double.valueOf(parts[1]);
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    public synchronized void addNewAlerts(ArrayList<Alert> newEvents) {
        insertsPending++;
        new AlertAddingTask(this).execute(newEvents);
    }

    private class AlertAddingTask extends AsyncTask<ArrayList<Alert>, Void, Void> {
        private AlertsDatabaseHelper parent;

        public AlertAddingTask(AlertsDatabaseHelper parent) {
            super();
            this.parent = parent;
        }

        @Override
        protected Void doInBackground(ArrayList<Alert>... alerts) {
            synchronized (parent) {
                SQLiteDatabase db = getWritableDatabase();
                for (Alert alert : alerts[0]) {
                    ContentValues values = new ContentValues();
                    String location = alert.location.getLatitude() + LAT_LONG_SEP + alert.location.getLongitude();
                    long UnixMillisUTC = alert.time.getTime(); // just so you are aware what this is
                    values.put(AlertsDatabaseContract.Alerts.COLUMN_NAME_ALERT_ID, alert.ID);
                    values.put(AlertsDatabaseContract.Alerts.COLUMN_NAME_LOCATION, location);
                    values.put(AlertsDatabaseContract.Alerts.COLUMN_NAME_TIME, UnixMillisUTC);
                    values.put(AlertsDatabaseContract.Alerts.COLUMN_NAME_AGENCY, alert.agency.ordinal());
                    values.put(AlertsDatabaseContract.Alerts.COLUMN_NAME_TYPE, alert.type.ordinal());
                    db.insert(AlertsDatabaseContract.Alerts.TABLE_NAME, null, values);
                }
                insertsPending--;
                parent.notify();
                return null;
            }
        }
    }
}
