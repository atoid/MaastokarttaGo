package com.apophis.maastokarttago;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.Locale;

class MarkerDlg {
    private AlertDialog mDlg;
    private MarkersActivity mCtx;
    private double mLat = 0, mLng = 0;

    MarkerDlg(MarkersActivity ctx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setView(R.layout.dialog_marker);

        builder.setNegativeButton(R.string.save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Save clicked
                addMarker();
                mCtx.updateView();
            }
        });

        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Close clicked
            }
        });

        builder.setNeutralButton(R.string.remove, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Remove clicked
                EditText et;
                et = mDlg.findViewById(R.id.marker_name);
                String name = et.getText().toString();

                mCtx.removeMarker(name);
                mCtx.updateView();
            }
        });

        mDlg = builder.create();
        mCtx = ctx;
    }

    void show() {
        mDlg.show();

        ImageButton ib;
        ib = mDlg.findViewById(R.id.btn_show_marker);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDlg.dismiss();
                addMarker();
                mCtx.showMarker(mLat, mLng);
            }
        });
    }

    void addMarker() {
        // Save clicked
        EditText et;
        et = mDlg.findViewById(R.id.marker_name);
        String name = et.getText().toString();
        et = mDlg.findViewById(R.id.marker_lat);
        String lat_s = et.getText().toString();
        et = mDlg.findViewById(R.id.marker_lng);
        String lng_s = et.getText().toString();

        double lat, lng;
        try {
            lat = Double.parseDouble(lat_s);
            lng = Double.parseDouble(lng_s);
        }
        catch (Exception e) {
            lat = 0;
            lng = 0;
        }

        mCtx.addMarker(name, lat, lng);
    }

    void setName(String name) {
        EditText et;
        et = mDlg.findViewById(R.id.marker_name);
        if (et != null) {
            et.setText(name);
            et.requestFocus();
        }
    }

    void setLatLng(double lat, double lng) {
        mLat = lat;
        mLng = lng;
        EditText et;
        et = mDlg.findViewById(R.id.marker_lat);
        if (et != null)
            et.setText(String.format(Locale.ROOT, "%.6f", lat));
        et = mDlg.findViewById(R.id.marker_lng);
        if (et != null)
            et.setText(String.format(Locale.ROOT, "%.6f", lng));
    }
}
