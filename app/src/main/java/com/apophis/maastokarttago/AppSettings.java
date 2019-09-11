package com.apophis.maastokarttago;

import android.content.Context;
import android.content.SharedPreferences;

class AppSettings {
    int zoom;
    int alpha;
    double lat;
    double lng;
    boolean follow;
    String url;
    int screenmode;
    boolean diron;
    boolean ruleron;
    boolean speedon;
    boolean orangeon;

    SharedPreferences mPrefs;
    MainActivity mApp;

    AppSettings(MainActivity app) {
        mApp = app;
    }

    void load() {
        mPrefs = mApp.getPreferences(Context.MODE_PRIVATE);
        alpha = mPrefs.getInt("alpha", mApp.DEFAULT_ALPHA);
        follow = mPrefs.getBoolean("follow", mApp.DEFAULT_FOLLOW);
        zoom = mPrefs.getInt("zoom", mApp.DEFAULT_ZOOM);
        lat = (double) mPrefs.getFloat("lat", mApp.DEFAULT_LAT);
        lng = (double) mPrefs.getFloat("lng", mApp.DEFAULT_LNG);
        url = mPrefs.getString("url", mApp.DEFAULT_URL);
        screenmode = mPrefs.getInt("screenmode", mApp.SCREEN_MODE_ON);
        diron = mPrefs.getBoolean("diron", mApp.DEFAULT_DIRON);
        ruleron = mPrefs.getBoolean("ruleron", mApp.DEFAULT_RULERON);
        speedon = mPrefs.getBoolean("speedon", mApp.DEFAULT_SPEEDON);
        orangeon = mPrefs.getBoolean("orangeon", mApp.DEFAULT_ORANGEON);
    }

    void save() {
        if (mPrefs == null)
            return;
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt("alpha", alpha);
        editor.putBoolean("follow", follow);
        editor.putInt("zoom", zoom);
        mApp.updateCoords();
        editor.putFloat("lat", (float) lat);
        editor.putFloat("lng", (float) lng);
        editor.putString("url", url);
        editor.putInt("screenmode", screenmode);
        editor.putBoolean("diron", diron);
        editor.putBoolean("ruleron", ruleron);
        editor.putBoolean("speedon", speedon);
        editor.putBoolean("orangeon", orangeon);
        editor.apply();
    }
}
