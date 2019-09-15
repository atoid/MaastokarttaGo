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
            Map<String, ?> markers = prefs.getAll();
            for (String key : markers.keySet()) {
                String[] coords = ((String) markers.get(key)).split(",");
                double lat, lng;

                try {
                    lat = Double.parseDouble(coords[0]);
                    lng = Double.parseDouble(coords[1]);
                }
                catch (Exception e) {
                    lat = 0.0;
                    lng = 0.0;
                }

                Marker m = new Marker(key, lat, lng);
                markersList.add(m);
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
            String name = m.name;
            String coords = String.format(Locale.ROOT, "%.6f,%.6f", m.lat, m.lng);
            editor.putString(name, coords);
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
