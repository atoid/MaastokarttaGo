package com.apophis.maastokarttago;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.TextView;

import java.util.Locale;

class AboutDlg {
    private AlertDialog mDlg;
    private Context mCtx;

    AboutDlg(Context ctx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        LayoutInflater inflater = ((AppCompatActivity) ctx).getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_about, null));

        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Close clicked
            }
        });

        mDlg = builder.create();
        mCtx = ctx;
    }

    void show() {
        mDlg.show();
    }

    void updateCoords(double lat, double lng) {
        TextView tv;
        tv = mDlg.findViewById(R.id.ab_lat);
        if (tv != null)
            tv.setText(String.format(Locale.ROOT, mCtx.getResources().getString(R.string.lat_n), lat));
        tv = mDlg.findViewById(R.id.ab_lng);
        if (tv != null)
            tv.setText(String.format(Locale.ROOT, mCtx.getResources().getString(R.string.lng_n), lng));
    }
}

