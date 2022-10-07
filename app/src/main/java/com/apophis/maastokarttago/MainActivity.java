package com.apophis.maastokarttago;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

class MapTile {
    int tilex;
    int tiley;
    Bitmap bm;
    int x;
    int y;
    int index;
}

class MapCenter {
    int tilex;
    int tiley;
    int dx;
    int dy;
}

public class MainActivity extends AppCompatActivity implements TileLoadedCb{
    private static final String TAG = "MAIN";

    final float ROTATE_LIMIT = 3;
    final int MAX_ZOOM = 19;
    final int MIN_ZOOM = 7;
    final int MAX_MOVE = 200;
    final String COLOR_ORANGE = "#FF6A00";
    final int MARKER_REQUEST = 1;
    final int MAX_TILES = 9;

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
    AppSettings mSettings = new AppSettings(this);
    AppControls mControls = new AppControls(this);
    MarkersView mMarkers = new MarkersView(this);
    AboutDlg mAboutDlg;
    UrlDlg mUrlDlg;
    int mTileSz = AppSettings.DEFAULT_TILESZ;
    ImageView mMainIw;
    Canvas mCanvas;
    float mRotation = 0.f;
    TileLoader mTileLoader = new TileLoader();
    Paint mBgPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSettings.load();
        mAboutDlg = new AboutDlg(this);
        mUrlDlg = new UrlDlg(this);

        RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(getSupportFragmentManager());
        int maxSize = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;
        LruCache<String, Bitmap> tmp = retainFragment.getLruCache(maxSize);
        if (tmp == null) {
            tmp = retainFragment.getLruCache(maxSize/2);
        }
        mTileLoader.setLruCache(tmp);

        View view = findViewById(R.id.tiles);
        view.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "View is ready");
                initTiles();
                mControls.init();
                mMarkers.init();
                update();
                mInitialized = true;
            }
        });

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION},
                1);

        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSettings.save();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ConstraintLayout cl = findViewById(R.id.tiles);
        cl.removeView(mMainIw);
        mMarkers.cleanup();
        cancelLoads();
        for (int i = 0; i < mTiles.length; i++) {
            mTiles[i].bm = null;
        }
        mCanvas = null;
        mMainIw = null;
        mAboutDlg = null;
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
        super.onResume();
        requestUpdates();
        mControls.setScreenOnOff();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case MARKER_REQUEST: {
                if (resultCode == Activity.RESULT_OK) {
                    double[] coords = data.getDoubleArrayExtra("coords");
                    boolean reload = data.getBooleanExtra("reload", false);

                    if (coords != null) {
                        mSettings.follow = false;
                        mSettings.lat = coords[0];
                        mSettings.lng = coords[1];
                        ImageView iw_dir = findViewById(R.id.dir_icon);
                        ImageView iw_ctr = findViewById(R.id.ctr_icon);
                        mControls.setFollowOnOff(iw_ctr, iw_dir);
                        mMarkers.load();
                        update(mSettings.lat, mSettings.lng, true);
                    }
                    else if (reload) {
                        mMarkers.load();
                        mMarkers.update(mSettings.lat, mSettings.lng);
                    }
                }
                break;
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
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

                mControls.setScreenOnOff();
                Location lastl = locationResult.getLastLocation();

                if (lastl != null && mInitialized) {
                    float speed = lastl.getSpeed() * 3.6f;
                    setRotation(lastl.getBearing(), speed);
                    if (mSettings.follow) {
                        LatLng nl = new LatLng(lastl.getLatitude(), lastl.getLongitude());
                        //Log.d(TAG, "lat: " + nl.latitude + " lng: " + nl.longitude);
                        mSettings.lat = nl.latitude;
                        mSettings.lng = nl.longitude;
                        update(mSettings.lat, mSettings.lng);
                        mAboutDlg.updateCoords(mSettings.lat, mSettings.lng);
                    }
                    TextView tv_speed = findViewById(R.id.map_speed);
                    if (speed < 10.f) {
                        tv_speed.setText(String.format(Locale.ROOT, "%.1f", speed));
                    }
                    else {
                        tv_speed.setText(String.format(Locale.ROOT, "%.0f", speed));
                    }
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
    public boolean onTouchEvent(MotionEvent event) {
        return mControls.onTouchEvent(event);
    }

    void setRotation(float deg, float speed) {
        ImageView iw_dir = findViewById(R.id.dir_icon);
        if (mSettings.rotateon && mSettings.follow) {
            iw_dir.setRotation(0);
            if (speed >= ROTATE_LIMIT) {
                mRotation = deg;
            }
        }
        else {
            iw_dir.setRotation(deg);
            mRotation = 0.f;
        }

        mMarkers.setRotation(mRotation);
        mCanvas.setMatrix(null);
        mCanvas.rotate(360.f - mRotation, mWidth/2, mHeight/2);
    }

    void updateCoords() {
        // If not following GPS, convert back from center tile position
        if (!mSettings.follow) {
            double[] ll = getLatLng(findCenterTile());
            mSettings.lat = ll[0];
            mSettings.lng = ll[1];
        }
    }

    void setBrightness(int alpha)
    {
        mMainIw.setImageAlpha(alpha);
    }

    private String[] getTileUrls(MapTile tile)
    {
        String url = mSettings.url;

        if (url.contains("${z}")) {
            url = url.replace("${z}", String.format(Locale.ROOT, "%d", mSettings.zoom));
            url = url.replace("${x}", String.format(Locale.ROOT, "%d", tile.tilex));
            url = url.replace("${y}", String.format(Locale.ROOT, "%d", tile.tiley));
        }
        else {
            url += mSettings.zoom + "/" + tile.tilex + "/" + tile.tiley + ".png";
        }
        //Log.d(TAG, "url: " + url);
        String[] res = new String[2];
        res[0] = url;

        // Overlays
        if (mSettings.rajaton) {
            url = AppSettings.RAJAT_URL + mSettings.zoom + "/" + tile.tilex + "/" + tile.tiley + ".png";
            res[1] = url;
        }

        return res;
    }

    private void initTiles() {
        ConstraintLayout cl = findViewById(R.id.tiles);
        mWidth = cl.getWidth();
        mHeight = cl.getHeight();
        Log.d(TAG, "screen w: " + mWidth);
        Log.d(TAG, "screen h: " + mHeight);
        int numPixels = Math.max(mWidth, mHeight);
        mTileSz = (int) getResources().getDimension(R.dimen.tilesz);
        Log.d(TAG, "tilesz: " + mTileSz);

        if (mWidth < mTileSz || mHeight < mTileSz) {
            mTileSz = Math.min(mWidth, mHeight);
            if (mTileSz == 0) {
                mTileSz = AppSettings.DEFAULT_TILESZ;
            }
            Log.i(TAG, "tilesz too big, adjust to: " + mTileSz);
        }

        mCols = (1 + (numPixels - 1) / mTileSz + 2) | 1;
        if (mCols > MAX_TILES) {
            mTileSz = (int)((numPixels - 1) / (MAX_TILES - 2.5));
            mCols = (1 + (numPixels - 1) / mTileSz + 2) | 1;
            Log.d(TAG, "max tiles exceeded, adjust tilesz to: " + mTileSz);
        }
        mRows = mCols;

        Log.d(TAG, "cols: " + mCols);
        Log.d(TAG, "rows: " + mRows);

        // Create tiles
        mTiles = new MapTile[mCols * mRows];
        for (int i = 0; i < mTiles.length; i++)
        {
            MapTile tile = new MapTile();
            tile.bm = null;
            tile.index = i;
            mTiles[i] = tile;
        }

        // Setup tile loader
        mTileLoader.setMaxLoaders(mCols * mRows);

        // Boundaries
        mMinx = (mWidth - mCols * mTileSz) / 2;
        mMaxx = mMinx + mCols * mTileSz;
        mMiny = (mHeight - mRows * mTileSz) / 2;
        mMaxy = mMiny + mRows * mTileSz;

        mBgPaint = new Paint();
        mBgPaint.setColor(0xff808080);

        // Main ImageView, Bitmap and Canvas
        ImageView iw = new ImageView(this);
        cl.addView(iw);
        iw.getLayoutParams().width = mWidth;
        iw.getLayoutParams().height = mHeight;
        iw.requestLayout();
        iw.setX(0);
        iw.setY(0);

        Bitmap bm = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        iw.setImageBitmap(bm);

        mCanvas = new Canvas(bm);
        mCanvas.drawARGB(255, 128, 128, 128);
        mMainIw = iw;
    }

    void resetTilePositions()
    {
        int posy = mMiny;
        for (int y = 0; y < mRows; y++) {
            int posx = mMinx;
            for (int x = 0; x < mCols; x++) {
                MapTile t = mTiles[y*mCols + x];
                t.x = posx;
                t.y = posy;
                posx += mTileSz;
            }
            posy += mTileSz;
        }
    }

    MapCenter calculateCenterTile(double lat, double lng)
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

    double[] getLatLng(MapTile tile)
    {
        return getLatLng(tile, mWidth/2, mHeight/2);
    }

    double[] getLatLng(MapTile tile, int refx, int refy)
    {
        double[] res = new double[2];

        try {
            double dx = (double)(tile.x - refx) / mTileSz;
            double dy = (double)(tile.y - refy) / mTileSz;

            double tx = tile.tilex - dx;
            double ty = tile.tiley - dy;

            double lng = (tx / Math.pow(2, mSettings.zoom) * 360 - 180);
            double n = Math.PI - 2 * Math.PI * ty / Math.pow(2, mSettings.zoom);
            double lat = (180 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));

            res[0] = lat;
            res[1] = lng;
        }
        catch(Exception e) {
            res[0] = (double) AppSettings.DEFAULT_LAT;
            res[1] = (double) AppSettings.DEFAULT_LNG;
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
        tile.bm = null;
        mTileLoader.load(this, getTileUrls(tile), tile.index);
    }

    public void setBitmap(Bitmap bm, int tag) {
        MapTile tile = mTiles[tag];
        if (tile != null) {
            tile.bm = bm;
            if (mCanvas != null && mMainIw != null) {
                drawTile(tile);
                mMainIw.invalidate();
            }
        }
    }

    public void cancelLoads()
    {
        mTileLoader.cancelLoads();
    }

    private boolean moveSingleTile(MapTile tile, int dx, int dy)
    {
        boolean dirty = false;
        int nx = tile.x + dx;
        int ny = tile.y + dy;

        if (nx >= (mMaxx - mTileSz/2))
        {
            nx -= mCols * mTileSz;
            tile.tilex -= mCols;
            dirty = true;
        }

        if (ny >= (mMaxy - mTileSz/2))
        {
            ny -= mRows * mTileSz;
            tile.tiley -= mRows;
            dirty = true;
        }

        if (nx < (mMinx - mTileSz/2))
        {
            nx += mCols * mTileSz;
            tile.tilex += mCols;
            dirty = true;
        }

        if (ny < (mMiny - mTileSz/2))
        {
            ny += mRows * mTileSz;
            tile.tiley += mRows;
            dirty = true;
        }

        tile.x = nx;
        tile.y = ny;

        if (dirty)
        {
            updateTileImage(tile);
        }

        return dirty;
    }

    void drawTile(MapTile tile) {
        if (mCanvas != null) {
            Rect r = new Rect();
            r.left = tile.x;
            r.right = tile.x + mTileSz;
            r.top = tile.y;
            r.bottom = tile.y + mTileSz;

            if (tile.bm != null) {
                mCanvas.drawBitmap(tile.bm, null, r, null);
            }
            else {
                mCanvas.drawRect(r, mBgPaint);
            }
        }
    }

    void moveTiles(int dx, int dy, boolean draw) {
        if (Math.abs(dx) > MAX_MOVE)
        {
            dx = (dx < 0) ? -MAX_MOVE : MAX_MOVE;
        }

        if (Math.abs(dy) > MAX_MOVE)
        {
            dy = (dy < 0) ? -MAX_MOVE : MAX_MOVE;
        }

        boolean dirty = false;
        for (int i = 0; i < mTiles.length; i++)
        {
            if (moveSingleTile(mTiles[i], dx, dy)) {
                dirty = true;
            }
            else {
                if (draw) {
                    drawTile(mTiles[i]);
                }
            }
        }

        // Move or update markers
        if (mSettings.follow) {
            if (dirty) {
                mMarkers.update(mSettings.lat, mSettings.lng);
            }
            else {
                mMarkers.move(dx, dy);
            }
        }
        else {
            mMarkers.move(dx, dy);
        }

        mMainIw.invalidate();
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
            int x = mTiles[i].x;
            int y = mTiles[i].y;

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
        int dx = mWidth/2 - tile.x - mTileSz/2;
        int dy = mHeight/2 - tile.y - mTileSz/2;

        //Log.d(TAG, "Offset x to screen center: " + dx);
        //Log.d(TAG, "       y to screen center: " + dy);

        dx += ctr.dx;
        dy += ctr.dy;

        //Log.d(TAG, "Offset x to loc center: " + dx);
        //Log.d(TAG, "       y to loc center: " + dy);

        if (Math.abs(dx) <= MAX_MOVE && Math.abs(dy) <= MAX_MOVE)
        {
            moveTiles(dx, dy, true);
            return 0;
        }

        return -1;
    }

    void update()
    {
        mCanvas.drawARGB(255, 128, 128, 128);
        update(mSettings.lat, mSettings.lng, true);
    }

    void update(double lat, double lng)
    {
        update(lat, lng, false);
    }

    void update(double lat, double lng, boolean forceRefresh)
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
            moveTiles(ctr.dx, ctr.dy, false);
            setTileImages(ctr.tilex, ctr.tiley);
            setBrightness(mSettings.alpha);
            mMarkers.update(lat, lng);
        }

        mControls.updateRuler();
    }
}
