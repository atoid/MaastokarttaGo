package com.apophis.maastokarttago;

import android.content.Context;
import android.content.SharedPreferences;

class AppSettings {
    static final int SCREEN_MODE_OFF = 0;
    static final int SCREEN_MODE_CGH = 1;
    static final int SCREEN_MODE_ON = 2;
    static final int DEFAULT_ZOOM = 15;
    static final int DEFAULT_ALPHA = 200;
    static final boolean DEFAULT_FOLLOW = true;
    static final boolean DEFAULT_DIRON = true;
    static final boolean DEFAULT_RULERON = true;
    static final boolean DEFAULT_SPEEDON = true;
    static final boolean DEFAULT_ORANGEON = false;
    static final boolean DEFAULT_ROTATEON = false;
    static final boolean DEFAULT_RAJATON = false;
    static final float DEFAULT_LAT = 64.220932f;
    static final float DEFAULT_LNG = 27.727754f;
    static final int DEFAULT_TILESZ = 256;
    static final String DEFAULT_URL = "https://tiles.kartat.kapsi.fi/peruskartta/";
    static final String TAUSTA_URL = "https://tiles.kartat.kapsi.fi/taustakartta/";
    static final String ORTO_URL = "https://tiles.kartat.kapsi.fi/ortokuva/";
    static final String RAJAT_URL = "https://tiles.kartat.kapsi.fi/kiinteistorajat/";

    int zoom;
    int alpha;
    double lat;
    double lng;
    boolean follow;
    String url;
    String ownurl;
    int screenmode;
    boolean diron;
    boolean ruleron;
    boolean speedon;
    boolean orangeon;
    boolean rotateon;
    boolean rajaton;

    SharedPreferences mPrefs;
    MainActivity mApp;

    AppSettings(MainActivity app) {
        mApp = app;
    }

    void load() {
        mPrefs = mApp.getPreferences(Context.MODE_PRIVATE);
        alpha = mPrefs.getInt("alpha", DEFAULT_ALPHA);
        follow = mPrefs.getBoolean("follow", DEFAULT_FOLLOW);
        zoom = mPrefs.getInt("zoom", DEFAULT_ZOOM);
        lat = (double) mPrefs.getFloat("lat", DEFAULT_LAT);
        lng = (double) mPrefs.getFloat("lng", DEFAULT_LNG);
        url = mPrefs.getString("url", DEFAULT_URL);
        ownurl = mPrefs.getString("ownurl", "");
        // Check for URL changes
        if (    !url.equals(DEFAULT_URL) &&
                !url.equals(TAUSTA_URL) &&
                !url.equals(ORTO_URL) &&
                !url.equals(ownurl)) {
            url = DEFAULT_URL;
        }
        screenmode = mPrefs.getInt("screenmode", SCREEN_MODE_ON);
        diron = mPrefs.getBoolean("diron", DEFAULT_DIRON);
        ruleron = mPrefs.getBoolean("ruleron", DEFAULT_RULERON);
        speedon = mPrefs.getBoolean("speedon", DEFAULT_SPEEDON);
        orangeon = mPrefs.getBoolean("orangeon", DEFAULT_ORANGEON);
        rotateon = mPrefs.getBoolean("rotateon", DEFAULT_ROTATEON);
        rajaton = mPrefs.getBoolean("rajaton", DEFAULT_RAJATON);
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
        editor.putString("ownurl", ownurl);
        editor.putInt("screenmode", screenmode);
        editor.putBoolean("diron", diron);
        editor.putBoolean("ruleron", ruleron);
        editor.putBoolean("speedon", speedon);
        editor.putBoolean("orangeon", orangeon);
        editor.putBoolean("rotateon", rotateon);
        editor.putBoolean("rajaton", rajaton);
        editor.apply();
    }
}
