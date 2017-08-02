package com.laserscorpion.redadalertas;

import android.content.Context;

import com.laserscorpion.redadalertas.db.AlertsDatabaseHelper;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class AlertChecker implements URLDataReceiver {
    private static String ALERT_URL = "https://laserscorpion.com/other/example.json";
    private Context context;
    private int attemptsRemaining = 5;

    public AlertChecker(Context context) {
        this.context = context;
    }

    public void downloadAlerts() {
        try {
            TorURLLoader loader = new TorURLLoader(context, new URL(ALERT_URL), this);
            loader.run();
        } catch (MalformedURLException e) {
            // just going to go ahead and not malform this URL
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void requestComplete(boolean successful, String data) {
        if (successful) {
            AlertsDatabaseHelper db = new AlertsDatabaseHelper(context);
            try {
                ArrayList<Alert> alerts = AlertJSONParser.parse(data);
                ArrayList<Alert> newAlerts = subsetForNewAlerts(alerts, db);
                db.addNewAlerts(newAlerts);
                notifyIfApplicable(newAlerts);
            } catch (JSONException e) {
                // TODO hmm is this a problem we could face? what to do?
                e.printStackTrace();
            }
        } else {
            if (attemptsRemaining > 0)
                scheduleRetry();
            else
                notifyFailure();
        }
    }

    private ArrayList<Alert> subsetForNewAlerts(ArrayList<Alert> alerts, AlertsDatabaseHelper db) {
        ArrayList<Alert> newAlerts = new ArrayList<>();
        long latestExistingAlertMS = db.getMostRecentAlertTime();
        Date latestExistingAlert = new Date(latestExistingAlertMS);
        for (Alert alert : alerts) {
            if (alert.time.after(latestExistingAlert))
                newAlerts.add(alert);
        }
        return newAlerts;
    }

    private void notifyIfApplicable(ArrayList<Alert> newAlerts) {
        for (Alert alert : newAlerts) {
            if (isOfInterest(alert))
                notifyAlert(alert);
        }
    }

    private boolean isOfInterest(Alert alert) {
        return true; // TODO
    }

    private void notifyAlert(Alert alert) {

    }

    private void notifyFailure() {

    }

    private void scheduleRetry() {

    }
}
