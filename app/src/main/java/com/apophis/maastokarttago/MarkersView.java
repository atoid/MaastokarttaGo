package com.apophis.maastokarttago;

import android.graphics.Matrix;
import android.graphics.Point;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

class MarkersView {
    final static int NUM_MARKERS = 10;
    ImageView[] mImages = new ImageView[NUM_MARKERS];
    Point[] mCoords = new Point[NUM_MARKERS];
    MainActivity mApp;
    int mWidth;
    int mHeight;
    ArrayList<Marker> mMarkersList = new ArrayList<>();
    View.OnClickListener mOnMarkerClick;
    float mRotation = 0.f;

    MarkersView(MainActivity app)
    {
        mApp = app;
    }

    void init() {
        ConstraintLayout cl = mApp.findViewById(R.id.tiles);
        mWidth = (int) mApp.getResources().getDimension(R.dimen.marker_w);
        mHeight = (int) mApp.getResources().getDimension(R.dimen.marker_h);

        mOnMarkerClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Marker m = mMarkersList.get((int) v.getTag());
                mApp.updateCoords();
                m.dist = m.distanceTo(mApp.mSettings.lat, mApp.mSettings.lng);
                Toast t = Toast.makeText(mApp, Util.getMarkerString(m), Toast.LENGTH_LONG);
                int yoff = (int) mApp.getResources().getDimension(R.dimen.toast_offset);
                t.setGravity(Gravity.TOP, 0, yoff);
                t.show();
            }
        };

        for (int i = 0; i < NUM_MARKERS; i++) {
            ImageView iw = new ImageView(mApp);
            iw.setImageResource(R.drawable.marker);
            cl.addView(iw);
            iw.bringToFront();
            iw.getLayoutParams().width = mWidth;
            iw.getLayoutParams().height = mHeight;
            iw.requestLayout();
            iw.setVisibility(View.INVISIBLE);
            iw.setTag(i);
            iw.setOnClickListener(mOnMarkerClick);
            mImages[i] = iw;
            mCoords[i] = new Point();
        }

        load();
    }

    void load() {
        mMarkersList.clear();
        Util.loadMarkers(mApp, mMarkersList);
    }

    void moveTo(int index, int x, int y)
    {
        mCoords[index].x = x;
        mCoords[index].y = y;

        Matrix mtx = new Matrix();
        mtx.setRotate(360.f - mRotation, mApp.mWidth/2, mApp.mHeight/2);
        float[] pts = {x, y};
        mtx.mapPoints(pts);

        ImageView iw = mImages[index];
        iw.setX(pts[0] - mWidth / 2);
        iw.setY(pts[1] - mHeight);
    }

    void move(int dx, int dy)
    {
        for (int i = 0; i < mImages.length; i++) {
            moveTo(i, mCoords[i].x + dx, mCoords[i].y + dy);
        }
    }

    void setRotation(float deg) {
        mRotation = deg;
    }

    void update(double lat, double lng)
    {
        Util.sortMarkers(mMarkersList, lat, lng);

        int i;
        for (i = 0; i < Math.min(NUM_MARKERS, mMarkersList.size()); i++) {
            Marker m = mMarkersList.get(i);
            ImageView iw = mImages[i];
            // Get tile where marker resides
            MapCenter ctr = mApp.calculateCenterTile(m.lat, m.lng);
            // Find if tile is in view
            int ti = mApp.findTileIndex(ctr.tilex, ctr.tiley);
            if (ti >= 0) {
                MapTile t = mApp.mTiles[ti];
                int x = t.x;
                int y = t.y;
                x += mApp.mTileSz/2 - ctr.dx;
                y += mApp.mTileSz/2 - ctr.dy;
                moveTo(i, x, y);
                iw.setVisibility(View.VISIBLE);
            }
            else {
                iw.setVisibility(View.INVISIBLE);
            }
        }

        // Hide rest
        for (; i < NUM_MARKERS; i++) {
            ImageView iw = mImages[i];
            iw.setVisibility(View.INVISIBLE);
        }
    }
}
