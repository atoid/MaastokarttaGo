package com.apophis.maastokarttago;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

class Util {
    final static String MARKERS_FILE = "com.apophis.maastokarttago.MARKERS";

    static void loadMarkers(Context ctx, ArrayList<Marker> markersList) {
        SharedPreferences prefs = ctx.getSharedPreferences(MARKERS_FILE, ctx.MODE_PRIVATE);
        if (prefs != null) {
            int id = 0;
            Map<String, ?> markers = prefs.getAll();
            for (String key : markers.keySet()) {
                String[] data = ((String) markers.get(key)).split(",");

                // Migrate from old format
                if (data.length < 3) {
                    String[] mig_data = new String[3];
                    mig_data[0] = key;
                    mig_data[1] = data[0];
                    mig_data[2] = data[1];
                    data = mig_data;
                }

                double lat, lng;
                try {
                    lat = Double.parseDouble(data[1]);
                    lng = Double.parseDouble(data[2]);
                }
                catch (Exception e) {
                    lat = 0.0;
                    lng = 0.0;
                }

                Marker m = new Marker(id, data[0], lat, lng);
                markersList.add(m);
                id++;
            }
        }
    }

    static void saveMarkers(Context ctx, ArrayList<Marker> markersList) {
        SharedPreferences prefs = ctx.getSharedPreferences(MARKERS_FILE, ctx.MODE_PRIVATE);
        if (prefs == null)
            return;
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (int i = 0; i < markersList.size(); i++) {
            Marker m = markersList.get(i);
            String name = String.format(Locale.ROOT,"%d", i);
            String data = String.format(Locale.ROOT, "%s,%.6f,%.6f", m.name, m.lat, m.lng);
            editor.putString(name, data);
        }
        editor.apply();
    }

    static void sortMarkers(ArrayList<Marker> markersList, double lat, double lng) {
        for (int i = 0; i < markersList.size(); i++) {
            Marker m = markersList.get(i);
            m.dist = m.distanceTo(lat, lng);
        }

        Collections.sort(markersList, new Comparator<Marker>() {
            @Override
            public int compare(Marker a, Marker b) {
                return Double.compare(a.dist, b.dist);
            }
        });
    }

    static String getMarkerString(Marker m) {
        String name;
        double d = m.dist;
        if (d >= 1) {
            name = String.format(Locale.ROOT, "%s (%.1f km)", m.name, d);
        }
        else if (d > 0.001) {
            name = String.format(Locale.ROOT, "%s (%.0f m)", m.name, d * 1000);
        }
        else {
            name = m.name;
        }
        return name;
    }
}
