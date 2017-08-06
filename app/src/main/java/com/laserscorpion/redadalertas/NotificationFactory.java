package com.laserscorpion.redadalertas;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.security.SecureRandom;

/**
 * I don't particularly want to do this in this way. I would be happy to sublcass Notification with
 * e.g. AlertNotification. But there's already this other Builder that we have to use, so oh well.
 */
public class NotificationFactory {
    public static final int ERROR_NOTIFICATION_ID = 875916473; // some arbitrary number
    private static final long[] pattern = {0, 1500L, 1000L, 1500L};

    public static Notification createErrorNotification(Context context, String text) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle(context.getString(R.string.error_title));
        builder.setContentText(text);
        builder.setStyle(new Notification.BigTextStyle().bigText(text));
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.notification_icon);

        Intent mainActivity = new Intent(context, AlertListActivity.class);
        mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent loadErrorActivity = PendingIntent.getActivity(context, 0, mainActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(loadErrorActivity);

        return builder.build();
    }

    public static Notification createAlertNotification(Context context, Alert alert) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(context.getString(R.string.alert_available_message));
        builder.setStyle(new Notification.BigTextStyle().bigText(context.getString(R.string.alert_available_message)));
        builder.setSmallIcon(R.drawable.notification_icon);
        builder.setAutoCancel(true);
        builder.setVibrate(pattern);

        Intent mapActivity = new Intent(context, AlertMapActivity.class);
        mapActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mapActivity.putExtra("alert", alert.toString()); // later, need to make Alert serializable for this
        int requestCode = new SecureRandom().nextInt(); // not clear if necessary, but we don't want two pending intents for different alerts to risk being equal
        PendingIntent alertActivity = PendingIntent.getActivity(context, requestCode, mapActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(alertActivity);

        return builder.build();
    }


}
