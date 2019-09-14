package com.apophis.maastokarttago;

import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

class MarkersView {
    final static int NUM_MARKERS = 10;
    ImageView[] mImages = new ImageView[NUM_MARKERS];
    MainActivity mApp;
    int mWidth;
    int mHeight;
    ArrayList<Marker> mMarkersList = new ArrayList<>();
    View.OnClickListener mOnMarkerClick;

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
                Toast t = Toast.makeText(mApp, m.name, Toast.LENGTH_LONG);
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
        }

        load();
    }

    void load() {
        mMarkersList.clear();
        Util.loadMarkers(mApp, mMarkersList);
    }

    void moveTo(int index, int x, int y)
    {
        ImageView iw = mImages[index];
        iw.setX(x - mWidth / 2);
        iw.setY(y - mHeight);
        iw.setVisibility(View.VISIBLE);
    }

    void move(int dx, int dy)
    {
        for (int i = 0; i < mImages.length; i++) {
            ImageView iw = mImages[i];
            int nx = (int) iw.getX() + dx;
            int ny = (int) iw.getY() + dy;
            iw.setX(nx);
            iw.setY(ny);
        }
    }

    void update(double lat, double lng)
    {
        Util.sortMarkers(mMarkersList, lat, lng);

        int i;
        for (i = 0; i < Math.min(NUM_MARKERS, mMarkersList.size()); i++) {
            Marker m = mMarkersList.get(i);
            // Get tile where marker resides
            MapCenter ctr = mApp.calculateCenterTile(m.lat, m.lng);
            // Find if tile is in view
            int ti = mApp.findTileIndex(ctr.tilex, ctr.tiley);
            if (ti >= 0) {
                MapTile t = mApp.mTiles[ti];
                int x = (int) t.img.getX();
                int y = (int) t.img.getY();
                x += mApp.mTileSz/2 - ctr.dx;
                y += mApp.mTileSz/2 - ctr.dy;
                moveTo(i, x, y);
            }
            else {
                ImageView iw = mImages[i];
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
