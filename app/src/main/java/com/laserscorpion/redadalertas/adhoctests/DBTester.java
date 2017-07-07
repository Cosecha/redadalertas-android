package com.laserscorpion.redadalertas.adhoctests;

import android.content.Context;
import android.location.Location;

import com.laserscorpion.redadalertas.Alert;
import com.laserscorpion.redadalertas.db.AlertsDatabaseHelper;

import java.util.ArrayList;
import java.util.Date;

/**
 * Not a proper test suite
 * I'm just running this in the debugger and checking it manually...
 */
public class DBTester {
    public DBTester(Context context) throws Exception {
        AlertsDatabaseHelper db = new AlertsDatabaseHelper(context);
        db.reset();
        ArrayList<Alert> hopefullyEmpty = db.getAlertsSince(new Date(0));
        if (hopefullyEmpty.size() != 0)
            throw new Exception("Dang -- 1");

        Alert test = new Alert();
        test.ID = 20;
        test.time = new Date();
        test.type = Alert.AlertType.RAID;
        test.agency = Alert.Agency.ICE;
        Location loc = new Location("");
        loc.setLatitude(30.0);
        loc.setLongitude(120.0);
        test.location = loc;
        ArrayList<Alert> list = new ArrayList<>();
        list.add(test);
        db.addNewAlerts(list);

        hopefullyEmpty = db.getAlertsSince(new Date());
        if (hopefullyEmpty.size() != 0)
            throw new Exception("Dang -- 2");

        ArrayList<Alert> hopefullyOne = db.getAlertsSince(new Date(100000000));
        if (hopefullyOne.size() != 1)
            throw new Exception("Dang -- 3");

        long latest = db.getMostRecentAlertTime();
        if (latest != test.time.getTime())
            throw new Exception("Dang -- 4");

        long latestID = db.getHighestID();
        if (latestID != test.ID)
            throw new Exception("Dang -- 5");

    }

}
