package com.apophis.maastokarttago;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.BatteryManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.FusedLocationProviderClient;

import com.squareup.picasso.Picasso;
import java.util.Arrays;
import java.util.Comparator;

class MapTile {
    int tilex;
    int tiley;
    boolean dirty;
    ImageView img;
}

class MapCenter {
    int tilex;
    int tiley;
    int dx;
    int dy;
}

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
}

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN";

    final int MAX_ZOOM = 19;
    final int MIN_ZOOM = 7;
    final int DEFAULT_ZOOM = 15;
    final int MAX_MOVE = 200;
    final int DEFAULT_ALPHA = 200;
    final boolean DEFAULT_FOLLOW = true;
    final int SCREEN_MODE_OFF = 0;
    final int SCREEN_MODE_CGH = 1;
    final int SCREEN_MODE_ON = 2;
    final boolean DEFAULT_DIRON = true;
    final boolean DEFAULT_RULERON = true;
    final boolean DEFAULT_SPEEDON = true;
    final boolean DEFAULT_ORANGEON = false;
    final String COLOR_ORANGE = "#FF6A00";
    final float DEFAULT_LAT = 64.220932f;
    final float DEFAULT_LNG = 27.727754f;
    final int DEFAULT_TILESZ = 256;
    final String DEFAULT_URL = "https://tiles.kartat.kapsi.fi/peruskartta/";
    final String TAUSTA_URL = "https://tiles.kartat.kapsi.fi/taustakartta/";
    final String ORTO_URL = "https://tiles.kartat.kapsi.fi/ortokuva/";

    FusedLocationProviderClient mFusedLocationClient;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    int mCols;
    int mRows;
    int mWidth;
    int mHeight;
    MapTile[] mTiles;
    int mMinx;
    int mMiny;
    int mMaxx;
    int mMaxy;
    boolean mInitialized = false;
    float mMovex, mMovey;
    SharedPreferences mPrefs;
    AppSettings mSettings = new AppSettings();
    AboutDlg mAboutDlg;
    int mTileSz = DEFAULT_TILESZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadSettings();
        mAboutDlg = new AboutDlg(this);

        View view = findViewById(R.id.tiles);
        view.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "View is ready");
                initTiles();
                initControls();
                update();
                mInitialized = true;
            }
        });

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1);

        Log.d(TAG, "onCreate");
    }

    private void loadSettings() {
        mPrefs = getPreferences(Context.MODE_PRIVATE);
        mSettings.alpha = mPrefs.getInt("alpha", DEFAULT_ALPHA);
        mSettings.follow = mPrefs.getBoolean("follow", DEFAULT_FOLLOW);
        mSettings.zoom = mPrefs.getInt("zoom", DEFAULT_ZOOM);
        mSettings.lat = (double) mPrefs.getFloat("lat", DEFAULT_LAT);
        mSettings.lng = (double) mPrefs.getFloat("lng", DEFAULT_LNG);
        mSettings.url = mPrefs.getString("url", DEFAULT_URL);
        mSettings.screenmode = mPrefs.getInt("screenmode", SCREEN_MODE_ON);
        mSettings.diron = mPrefs.getBoolean("diron", DEFAULT_DIRON);
        mSettings.ruleron = mPrefs.getBoolean("ruleron", DEFAULT_RULERON);
        mSettings.speedon = mPrefs.getBoolean("speedon", DEFAULT_SPEEDON);
        mSettings.orangeon = mPrefs.getBoolean("orangeon", DEFAULT_ORANGEON);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt("alpha", mSettings.alpha);
        editor.putBoolean("follow", mSettings.follow);
        editor.putInt("zoom", mSettings.zoom);
        updateCoords();
        editor.putFloat("lat", (float) mSettings.lat);
        editor.putFloat("lng", (float) mSettings.lng);
        editor.putString("url", mSettings.url);
        editor.putInt("screenmode", mSettings.screenmode);
        editor.putBoolean("diron", mSettings.diron);
        editor.putBoolean("ruleron", mSettings.ruleron);
        editor.putBoolean("speedon", mSettings.speedon);
        editor.putBoolean("orangeon", mSettings.orangeon);
        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAboutDlg = null;
        saveSettings();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFusedLocationClient != null && mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onPause();
        requestUpdates();
        setScreenOnOff();
        Log.d(TAG, "onResume");
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Permission results");

        boolean grantLoc = false;

        if (grantResults.length >= 1) {
            grantLoc = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }

        if (grantLoc) {
            Log.d(TAG, "Location granted");
            initLocation();
        }
    }

    private void initLocation()
    {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                setScreenOnOff();
                Location lastl = locationResult.getLastLocation();

                if (lastl != null && mInitialized) {
                    if (mSettings.follow) {
                        LatLng nl = new LatLng(lastl.getLatitude(), lastl.getLongitude());
                        //Log.d(TAG, "lat: " + nl.latitude + " lng: " + nl.longitude);
                        mSettings.lat = nl.latitude;
                        mSettings.lng = nl.longitude;
                        update(mSettings.lat, mSettings.lng);
                        ImageView iw_dir = findViewById(R.id.dir_icon);
                        iw_dir.setRotation(lastl.getBearing());
                        mAboutDlg.updateCoords(mSettings.lat, mSettings.lng);
                    }
                    TextView tv_speed = findViewById(R.id.map_speed);
                    tv_speed.setText("" + (int) Math.round(3.6 * lastl.getSpeed()));
                }
            }
        };

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(500);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        requestUpdates();
    }

    public void requestUpdates() {
        try {
            if (mFusedLocationClient != null && mLocationRequest != null) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                        mLocationCallback,
                        null /* Looper */);
            }
        } catch(SecurityException e) {}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointer = event.getPointerId(index);

        if (pointer != 0) {
            return true;
        }

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                mMovex = event.getX();
                mMovey = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float nx = event.getX();
                float ny = event.getY();

                float dx = nx - mMovex;
                float dy = ny - mMovey;

                moveTiles((int) dx, (int) dy);

                mMovex = nx;
                mMovey = ny;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateRuler();
                break;
        }
        return true;
    }

    private void initControls() {
        TextView tv = findViewById(R.id.map_ruler);
        tv.bringToFront();
        updateRuler();

        final ImageView iw_dir = findViewById(R.id.dir_icon);
        iw_dir.setImageResource(R.drawable.direction);
        iw_dir.setX(mWidth / 2 - iw_dir.getWidth() / 2);
        iw_dir.setY(mHeight / 2 - iw_dir.getHeight() / 2);

        final ImageView iw = findViewById(R.id.ctr_icon);
        iw.setX(mWidth / 2 - iw.getWidth() / 2);
        iw.setY(mHeight / 2 - iw.getHeight() / 2);
        iw.bringToFront();
        iw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSettings.follow = !mSettings.follow;
                setFollowOnOff(iw, iw_dir);
            }
        });
        iw_dir.bringToFront();
        setFollowOnOff(iw, iw_dir);

        final int orange = Color.parseColor(COLOR_ORANGE);
        if (mSettings.orangeon) {
            iw.setColorFilter(orange);
            iw_dir.setColorFilter(orange);
        }

        ImageButton ib;
        ib = findViewById(R.id.btn_zoom_in);
        ib.bringToFront();
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSettings.zoom < MAX_ZOOM) {
                    updateCoords();
                    mSettings.zoom++;
                    update();
                }
            }
        });

        ib = findViewById(R.id.btn_zoom_out);
        ib.bringToFront();
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSettings.zoom > MIN_ZOOM) {
                    updateCoords();
                    mSettings.zoom--;
                    update();
                }
            }
        });

        final SeekBar sb = findViewById(R.id.sb_brightness);
        sb.bringToFront();
        sb.setProgress(mSettings.alpha);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mSettings.alpha = i;
                setBrightness(mSettings.alpha);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final PopupMenu popup = new PopupMenu(this, findViewById(R.id.btn_maps));

        final OnMenuItemClickListener listener = new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();

                // Submenu items
                if (id == R.id.mi_screen || id == R.id.mi_settings) {
                    return false;
                }

                // Direction indicator
                if (id == R.id.mi_diron) {
                    mSettings.diron = !menuItem.isChecked();
                    menuItem.setChecked(mSettings.diron);
                    setDirOnOff();
                    return true;
                }

                // Ruler indicator
                if (id == R.id.mi_ruleron) {
                    mSettings.ruleron = !menuItem.isChecked();
                    menuItem.setChecked(mSettings.ruleron);
                    setRulerOnOff();
                    return true;
                }

                // Speed indicator
                if (id == R.id.mi_speedon) {
                    mSettings.speedon = !menuItem.isChecked();
                    menuItem.setChecked(mSettings.speedon);
                    setSpeedOnOff();
                    return true;
                }

                // Orange pointer
                if (id == R.id.mi_orangeon) {
                    mSettings.orangeon = !menuItem.isChecked();
                    menuItem.setChecked(mSettings.orangeon);
                    if (mSettings.orangeon) {
                        iw.setColorFilter(orange);
                        iw_dir.setColorFilter(orange);
                    }
                    else {
                        iw.clearColorFilter();
                        iw_dir.clearColorFilter();
                    }
                    return true;
                }

                // About
                if (id == R.id.mi_about) {
                    mAboutDlg.show();
                    if (!mSettings.follow) {
                        double[] ll = getLatLng(findCenterTile());
                        mAboutDlg.updateCoords(ll[0], ll[1]);
                    }
                    return true;
                }

                menuItem.setChecked(true);

                // Screen modes
                if (id == R.id.mi_screenoff) {
                    mSettings.screenmode = SCREEN_MODE_OFF;
                    setScreenOnOff();
                    return true;
                }
                if (id == R.id.mi_screenchg) {
                    mSettings.screenmode = SCREEN_MODE_CGH;
                    setScreenOnOff();
                    return true;
                }
                if (id == R.id.mi_screenon) {
                    mSettings.screenmode = SCREEN_MODE_ON;
                    setScreenOnOff();
                    return true;
                }

                // Map sources
                if (id == R.id.mi_orto) {
                    mSettings.url = ORTO_URL;
                }
                else if (id == R.id.mi_tausta) {
                    mSettings.url = TAUSTA_URL;
                }
                else {
                    mSettings.url = DEFAULT_URL;
                }

                updateCoords();
                update();
                return true;
            }
        };

        popup.setOnMenuItemClickListener(listener);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.maps, popup.getMenu());

        Menu m = popup.getMenu();
        MenuItem mi;
        if (mSettings.url.equals(TAUSTA_URL)) {
            mi = m.findItem(R.id.mi_tausta);
        }
        else if (mSettings.url.equals(ORTO_URL)) {
            mi = m.findItem(R.id.mi_orto);
        }
        else {
            mi = m.findItem(R.id.mi_maasto);
        }
        mi.setChecked(true);

        if (mSettings.screenmode == SCREEN_MODE_OFF) {
            mi = m.findItem(R.id.mi_screenoff);
        }
        else if (mSettings.screenmode == SCREEN_MODE_CGH) {
            mi = m.findItem(R.id.mi_screenchg);
        }
        else {
            mi = m.findItem(R.id.mi_screenon);
        }
        mi.setChecked(true);
        setScreenOnOff();

        mi = m.findItem(R.id.mi_diron);
        mi.setChecked(mSettings.diron);
        setDirOnOff();

        mi = m.findItem(R.id.mi_ruleron);
        mi.setChecked(mSettings.ruleron);
        setRulerOnOff();

        mi = m.findItem(R.id.mi_speedon);
        mi.setChecked(mSettings.speedon);
        setSpeedOnOff();

        mi = m.findItem(R.id.mi_orangeon);
        mi.setChecked(mSettings.orangeon);

        final ImageButton maps = findViewById(R.id.btn_maps);
        maps.bringToFront();
        maps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.show();
            }
        });
    }

    private void setFollowOnOff(ImageView iwCtr, ImageView iwDir) {
        if (mSettings.follow) {
            iwCtr.setImageResource(R.drawable.center);
            iwDir.setImageAlpha(255);
        }
        else {
            iwCtr.setImageResource(R.drawable.centeroff);
            iwDir.setImageAlpha(0);
        }
    }

    private void setScreenOnOff() {
        if (mSettings.screenmode == SCREEN_MODE_OFF) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else if (mSettings.screenmode == SCREEN_MODE_CGH) {
            if (isCharging()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private boolean isCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        return isCharging;
    }

    private void setDirOnOff() {
        ImageView iw = findViewById(R.id.dir_icon);
        if (mSettings.diron) {
            iw.setVisibility(View.VISIBLE);
        }
        else {
            iw.setVisibility(View.INVISIBLE);
        }
    }

    private void setRulerOnOff() {
        TextView tv = findViewById(R.id.map_ruler);
        if (mSettings.ruleron) {
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.INVISIBLE);
        }
    }

    private void setSpeedOnOff() {
        TextView iw = findViewById(R.id.map_speed);
        if (mSettings.speedon) {
            iw.setVisibility(View.VISIBLE);
            iw.bringToFront();
        }
        else {
            iw.setVisibility(View.INVISIBLE);
        }
        iw.setY((mHeight - iw.getHeight()) / 2.f);
    }

    private void updateCoords() {
        // If not following GPS, convert back from center tile position
        if (!mSettings.follow) {
            double[] ll = getLatLng(findCenterTile());
            mSettings.lat = ll[0];
            mSettings.lng = ll[1];
        }
    }

    private void updateRuler() {
        final double[] r_res  = { 1222.9900, 611.4962, 305.7481, 152.8741, 76.4370, 38.2185, 19.1093, 9.5546, 4.7773, 2.3887, 1.1943, 0.5972, 0.2986 };
        final int[] r_meters  = { 160000, 80000, 40000, 20000, 10000, 5000, 2000, 1000, 500, 250, 100, 50, 25};
        final String[] r_text = {"160km", "80km", "40km", "20km", "10km", "5km", "2km", "1km", "500m", "250m", "100m", "50m", "25m" };

        TextView tv = findViewById(R.id.map_ruler);
        int rulerx = (int) tv.getX();
        int rulery = (int) tv.getY();

        double r = r_res[mSettings.zoom - MIN_ZOOM];
        double lat = getLatLng(findTileAt(rulerx, rulery), rulerx, rulery)[0];
        //Log.d(TAG, "Ruler at lat: " + lat);
        r = r * Math.cos(Math.toRadians(lat));
        double width = r_meters[mSettings.zoom - MIN_ZOOM] / r;
        width *= (mTileSz / 256.0);

        tv.setWidth((int) Math.round(width));
        tv.setText(r_text[mSettings.zoom - MIN_ZOOM]);
    }

    private void setBrightness(int alpha)
    {
        for (int i = 0; i < mTiles.length; i++) {
            mTiles[i].img.setImageAlpha(alpha);
        }
    }

    private String getTileUrl(MapTile tile)
    {
        String url = mSettings.url;
        url += mSettings.zoom + "/" + tile.tilex + "/" + tile.tiley + ".png";
        //Log.d(TAG, "url: " + url);
        return url;
    }

    private void initTiles() {
        ConstraintLayout cl = findViewById(R.id.tiles);

        mWidth = cl.getWidth();
        mHeight = cl.getHeight();

        if (mWidth < mTileSz || mHeight < mTileSz) {
            mTileSz = Math.min(mWidth, mHeight);
            if (mTileSz == 0) {
                mTileSz = DEFAULT_TILESZ;
            }
            Log.i(TAG, "Adjust tilesize to: " + mTileSz);
        }

        Log.d(TAG, "screen w: " + mWidth);
        Log.d(TAG, "screen h: " + mHeight);

        mCols = (1 + (mWidth - 1) / mTileSz + 2) | 1;
        mRows = (1 + (mHeight - 1) / mTileSz + 2) | 1;

        Log.d(TAG, "cols: " + mCols);
        Log.d(TAG, "rows: " + mRows);

        // Create tiles
        mTiles = new MapTile[mCols * mRows];
        for (int i = 0; i < mTiles.length; i++)
        {
            ImageView iw = new ImageView(this);
            MapTile tile = new MapTile();
            tile.img = iw;
            mTiles[i] = tile;
            cl.addView(iw);
        }

        // Boundaries
        mMinx = (mWidth - mCols * mTileSz) / 2;
        mMaxx = mMinx + mCols * mTileSz;
        mMiny = (mHeight - mRows * mTileSz) / 2;
        mMaxy = mMiny + mRows * mTileSz;
    }

    void resetTilePositions()
    {
        int posy = mMiny;
        for (int y = 0; y < mRows; y++) {
            int posx = mMinx;
            for (int x = 0; x < mCols; x++) {
                ImageView iw = mTiles[y*mCols + x].img;
                iw.setX(posx);
                iw.setY(posy);
                posx += mTileSz;
            }
            posy += mTileSz;
        }
    }

    private MapCenter calculateCenterTile(double lat, double lng)
    {
        double tx, ty;
        try {
            tx = (lng + 180.0) / 360.0 * (1 << mSettings.zoom);
            ty = (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << mSettings.zoom);
        }
        catch (Exception e) {
            tx = 0;
            ty = 0;
        }

        int tilex = (int) Math.floor( tx );
        int tiley = (int) Math.floor( ty );

        MapCenter res = new MapCenter();
        res.tilex = tilex;
        res.tiley = tiley;
        res.dx = (int) (-mTileSz * (tx - tilex - 0.5));
        res.dy = (int) (-mTileSz * (ty - tiley - 0.5));

        //Log.d(TAG, "ctr tx: " + tx);
        //Log.d(TAG, "ctr ty: " + ty);
        //Log.d(TAG, "ctr dx: " + res.dx);
        //Log.d(TAG, "ctr dy: " + res.dy);

        return res;
    }

    private double[] getLatLng(MapTile tile)
    {
        return getLatLng(tile, mWidth/2, mHeight/2);
    }

    private double[] getLatLng(MapTile tile, int refx, int refy)
    {
        double[] res = new double[2];

        try {
            double dx = (tile.img.getX() - refx) / mTileSz;
            double dy = (tile.img.getY() - refy) / mTileSz;

            double tx = tile.tilex - dx;
            double ty = tile.tiley - dy;

            double lng = (tx / Math.pow(2, mSettings.zoom) * 360 - 180);
            double n = Math.PI - 2 * Math.PI * ty / Math.pow(2, mSettings.zoom);
            double lat = (180 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));

            res[0] = lat;
            res[1] = lng;
        }
        catch(Exception e) {
            res[0] = (double) DEFAULT_LAT;
            res[1] = (double) DEFAULT_LNG;
        }

        return res;
    }

    private void setTileImages(int centerTilex, int centerTiley)
    {
        // Always odd number of cols and rows
        int cx = mCols / 2;
        int cy = mRows / 2;
        Log.d(TAG, "Center tile: " + cx + ", " + cy);

        int tilex = centerTilex - cx;
        int tiley = centerTiley - cy;

        class TileDist {
            int ti;
            int dist;
            TileDist(int ti, int dist) {
                this.ti = ti;
                this.dist = dist;
            }
        }

        TileDist[] arr = new TileDist[mTiles.length];

        for (int y = 0; y < mRows; y++)
        {
            for (int x = 0; x < mCols; x++)
            {
                int ti = y * mCols + x;
                MapTile tile = mTiles[ti];
                tile.tilex = tilex + x;
                tile.tiley = tiley + y;
                // Distance from center tile
                arr[ti] = new TileDist(ti, (x-cx)*(x-cx) + (y-cy)*(y-cy));
            }
        }

        Arrays.sort(arr, new Comparator<TileDist>() {
            @Override
            public int compare(TileDist a, TileDist b) {
                return a.dist-b.dist;
            }
        });

        for (int i = 0; i < arr.length; i++) {
            updateTileImage(mTiles[arr[i].ti]);
        }
    }

    void updateTileImage(MapTile tile)
    {
        Picasso.with(getApplicationContext()).load(getTileUrl(tile)).resize(mTileSz, mTileSz).into(tile.img);
        tile.dirty = false;
    }

    private void moveSingleTile(MapTile tile, int dx, int dy)
    {
        ImageView iw = tile.img;

        int nx = (int) iw.getX() + dx;
        int ny = (int) iw.getY() + dy;

        if (nx >= (mMaxx - mTileSz/2))
        {
            nx -= mCols * mTileSz;
            tile.tilex -= mCols;
            tile.dirty = true;
        }

        if (ny >= (mMaxy - mTileSz/2))
        {
            ny -= mRows * mTileSz;
            tile.tiley -= mRows;
            tile.dirty = true;
        }

        if (nx < (mMinx - mTileSz/2))
        {
            nx += mCols * mTileSz;
            tile.tilex += mCols;
            tile.dirty = true;
        }

        if (ny < (mMiny - mTileSz/2))
        {
            ny += mRows * mTileSz;
            tile.tiley += mRows;
            tile.dirty = true;
        }

        iw.setX(nx);
        iw.setY(ny);

        if (tile.dirty)
        {
            updateTileImage(tile);
        }
    }
    
    private void moveTiles(int dx, int dy) {
        if (Math.abs(dx) > MAX_MOVE)
        {
            dx = (dx < 0) ? -MAX_MOVE : MAX_MOVE;
        }

        if (Math.abs(dy) > MAX_MOVE)
        {
            dy = (dy < 0) ? -MAX_MOVE : MAX_MOVE;
        }

        for (int i = 0; i < mTiles.length; i++)
        {
            moveSingleTile(mTiles[i], dx, dy);
        }
    }

    int findTileIndex(int tilex, int tiley)
    {
        for (int i = 0; i < mTiles.length; i++)
        {
            if (mTiles[i].tilex == tilex && mTiles[i].tiley == tiley)
            {
                return i;
            }
        }
        return -1;
    }

    MapTile findCenterTile()
    {
        return findTileAt(mWidth/2, mHeight/2);
    }

    MapTile findTileAt(int tx, int ty)
    {
        for (int i = 0; i < mTiles.length; i++) {
            int x = (int) mTiles[i].img.getX();
            int y = (int) mTiles[i].img.getY();

            if (tx >= x && tx < (x + mTileSz)){
                if (ty >= y && ty < (y + mTileSz)) {
                    return mTiles[i];
                }
            }
        }
        return null;
    }

    private int updateLocation(MapCenter ctr, MapTile tile)
    {
        int dx = mWidth/2 - (int) tile.img.getX() - mTileSz/2;
        int dy = mHeight/2 - (int) tile.img.getY() - mTileSz/2;

        //Log.d(TAG, "Offset x to screen center: " + dx);
        //Log.d(TAG, "       y to screen center: " + dy);

        dx += ctr.dx;
        dy += ctr.dy;

        //Log.d(TAG, "Offset x to loc center: " + dx);
        //Log.d(TAG, "       y to loc center: " + dy);

        if (Math.abs(dx) <= MAX_MOVE && Math.abs(dy) <= MAX_MOVE)
        {
            moveTiles(dx, dy);
            return 0;
        }

        return -1;
    }

    private void update()
    {
        update(mSettings.lat, mSettings.lng, true);
    }

    private void update(double lat, double lng)
    {
        update(lat, lng, false);
    }

    private void update(double lat, double lng, boolean forceRefresh)
    {
        MapCenter ctr = calculateCenterTile(lat, lng);

        int i = -1;
        if (!forceRefresh) {
            i = findTileIndex(ctr.tilex, ctr.tiley);
        }

        if (i >= 0) {
            i = updateLocation(ctr, mTiles[i]);
        }

        if (i < 0) {
            Log.d(TAG, "Tiles full reset");
            resetTilePositions();
            setTileImages(ctr.tilex, ctr.tiley);
            setBrightness(mSettings.alpha);
            moveTiles(ctr.dx, ctr.dy);
        }
        updateRuler();
    }
}
