package com.apophis.maastokarttago;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Locale;

class Marker {
    String name;
    double lat;
    double lng;
    double dist;

    Marker(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.dist = 0;
    }

    double distanceTo(double lat, double lng) {
        if (lat == 0 && lng == 0)
            return 0;

        double R = 6371.0;
        double lata_r = Math.toRadians(this.lat);
        double lnga_r = Math.toRadians(this.lng);
        double latb_r = Math.toRadians(lat);
        double lngb_r = Math.toRadians(lng);

        return R * Math.acos(   Math.cos(lata_r) *
                                Math.cos(latb_r) *
                                Math.cos(lngb_r - lnga_r) +
                                Math.sin(lata_r) *
                                Math.sin(latb_r));
    }
}

public class MarkersActivity extends AppCompatActivity {
    final String TAG = "MARKERS";
    ArrayList<Marker> mMarkers = new ArrayList<>();
    ArrayList<String> mMarkerNames = new ArrayList<>();
    MarkerDlg mDlg;
    ArrayAdapter<String> mItemsAdapter;
    double mLat = 0, mLng = 0;
    boolean mDirty = false;
    Intent mResultIntent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markers);

        mItemsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMarkerNames);

        ListView markerList;
        markerList = findViewById(R.id.markers);
        markerList.setAdapter(mItemsAdapter);
        markerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {
                mDlg.show();
                Marker m = mMarkers.get(position);
                mDlg.setLatLng(m.lat, m.lng);
                mDlg.setName(m.name);
            }
        });

        mDlg = new MarkerDlg(this);
        Intent intent = getIntent();
        double[] coords = intent.getDoubleArrayExtra("coords");
        if (coords != null) {
            mLat = coords[0];
            mLng = coords[1];
            mDlg.show();
            mDlg.setLatLng(mLat, mLng);
            mDlg.setName("");
        }

        Util.loadMarkers(this, mMarkers);
        updateView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        Util.saveMarkers(this, mMarkers);

        if (mDirty) {
            mResultIntent.putExtra("reload", true);
            setResult(Activity.RESULT_OK, mResultIntent);
        }

        super.finish();
    }

    void addMarker(String name, double lat, double lng) {
        if (!name.isEmpty()) {
            Marker m = new Marker(name, lat, lng);
            mMarkers.add(m);
            mDirty = true;
        }
    }

    void removeMarker(String name) {
        if (!name.isEmpty()) {
            for (int i = 0; i < mMarkers.size(); i++) {
                if (mMarkers.get(i).name.equals(name))
                {
                    mMarkers.remove(i);
                    mDirty = true;
                    break;
                }
            }
        }
    }

    void showMarker(double lat, double lng) {
        double[] coords = {lat, lng};
        mResultIntent.putExtra("coords", coords);
        mDirty = true;
        finish();
    }

    void updateView() {
        Util.sortMarkers(mMarkers, mLat, mLng);
        mMarkerNames.clear();
        for (int i = 0; i < mMarkers.size(); i++) {
            Marker m = mMarkers.get(i);
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

            mMarkerNames.add(name);
        }
        mItemsAdapter.notifyDataSetChanged();
    }
}
