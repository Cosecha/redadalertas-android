package com.redadalertas.client.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.util.ArrayList;


public class LocationPrefDatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "LocationPrefs.db";
    private static final String SQL_CREATE_LOCATIONS =
            "CREATE TABLE " + LocationPrefDatabaseContract.LocationPrefs.TABLE_NAME + " (" +
                    LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LOCATION_ID + " INTEGER PRIMARY KEY," +
                    LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LATITUDE + " REAL, " +
                    LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LONGITUDE + " REAL)"
            ;

    public LocationPrefDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_LOCATIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addLocation(Location add) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LATITUDE, add.getLatitude());
        values.put(LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LONGITUDE, add.getLongitude());
        db.insert(LocationPrefDatabaseContract.LocationPrefs.TABLE_NAME, null, values);
    }

    public ArrayList<StoredLocation> getLocations() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor results = db.query(LocationPrefDatabaseContract.LocationPrefs.TABLE_NAME, null, null, null, null, null,
                LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LOCATION_ID);

        ArrayList<StoredLocation> locations = new ArrayList<>();
        while (results.moveToNext()) {
            StoredLocation location = new StoredLocation();
            location.ID = results.getInt(results.getColumnIndex(LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LOCATION_ID));
            Location loc = new Location("");
            loc.setLatitude(results.getDouble(results.getColumnIndex(LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LATITUDE)));
            loc.setLongitude(results.getDouble(results.getColumnIndex(LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LONGITUDE)));
            location.location = loc;
            locations.add(location);
        }
        results.close();
        return locations;
    }

    public void deleteLocation(int ID) {
        SQLiteDatabase db = getWritableDatabase();
        String[] IDarray = new String[1];
        IDarray[0] = Integer.toString(ID);
        String where = LocationPrefDatabaseContract.LocationPrefs.COLUMN_NAME_LOCATION_ID + " = ?";
        db.delete(LocationPrefDatabaseContract.LocationPrefs.TABLE_NAME, where, IDarray);
    }

}
