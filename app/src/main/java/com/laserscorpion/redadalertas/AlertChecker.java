package com.laserscorpion.redadalertas;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import com.laserscorpion.redadalertas.db.AlertsDatabaseHelper;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;

public class AlertChecker implements URLDataReceiver {
    private static final String TAG = "AlertChecker";
    private static final int SECONDS_UNTIL_RETRY = 60;
    private static String ALERT_URL = "https://laserscorpion.com/other/example.json";
    private static boolean retryPending = false;
    private Context context;
    private int attemptsRemaining = 5;

    public AlertChecker(Context context) {
        this.context = context;
    }

    public void downloadAlerts() {
        try {
            TorURLLoader loader = new TorURLLoader(context, new URL(ALERT_URL), this);
            loader.start();
        } catch (MalformedURLException e) {
            // just going to go ahead and not malform this URL
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void requestComplete(boolean successful, String data) {
        synchronized (AlertChecker.class) {
            retryPending = false;
        }
        if (successful) {
            cancelErrorNotification();
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
            Log.d(TAG, "Download failed");
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
        Notification alertNotification = NotificationFactory.createAlertNotification(context, alert);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int id = new SecureRandom().nextInt(); // I don't want these to collide; we want multiple notifications to appear if applicable
        notificationManager.notify(id, alertNotification);
    }

    private void notifyFailure() {
        String errorMessage = context.getString(R.string.alert_download_failure_message);
        Notification errorNotification = NotificationFactory.createErrorNotification(context, errorMessage);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int id = NotificationFactory.ERROR_NOTIFICATION_ID; // I *DO* want these to collide, so we don't show more than one error notification at a time
        notificationManager.notify(id, errorNotification);
    }

    private void cancelErrorNotification() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NotificationFactory.ERROR_NOTIFICATION_ID);
    }

    private void scheduleRetry() {
        synchronized (AlertChecker.class) {
            if (!retryPending) {
                Log.d(TAG, "Scheduling retry");
                retryPending = true;
                attemptsRemaining--;
                sleepUninteruptibly(SECONDS_UNTIL_RETRY);
                if (retryPending) { // it might not be after sleeping
                    Log.d(TAG, "Attempting download retry");
                    downloadAlerts(); // we're still holding the monitor lock but this method call mainly defers to another thread so I'm not worried
                }
            }
        }
    }

    private void sleepUninteruptibly(int seconds) {
        long msRemaining = seconds * 1000;
        while (msRemaining > 0) {
            long start = new Date().getTime();
            try {
                Thread.sleep(msRemaining);
                return;
            } catch (InterruptedException e) {
                long now = new Date().getTime();
                msRemaining -= (now - start);
            }
        }
    }
}
