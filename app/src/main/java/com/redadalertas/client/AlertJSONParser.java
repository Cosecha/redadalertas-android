package com.redadalertas.client;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class AlertJSONParser {

    private AlertJSONParser(){}

    public static ArrayList<Alert> parse(String JSON) throws JSONException {
        JSONObject obj = new JSONObject(JSON);
        JSONArray array = obj.getJSONArray("alerts");
        ArrayList<Alert> alerts = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject jsObj = array.getJSONObject(i);

            Alert alert = new Alert();
            alert.ID = jsObj.getInt("id");
            long unixMillis = jsObj.getLong("time");
            alert.time = new Date(unixMillis);
            String agency = jsObj.getString("agency").toUpperCase();
            alert.agency = Alert.Agency.valueOf(agency);
            String type = jsObj.getString("alertType").toUpperCase();
            alert.type = Alert.AlertType.valueOf(type);
            double latitude = jsObj.getDouble("latitude");
            double longitude = jsObj.getDouble("longitude");
            alert.location = new Location("");
            alert.location.setLatitude(latitude);
            alert.location.setLongitude(longitude);

            alerts.add(alert);
        }
        return alerts;
    }
}
