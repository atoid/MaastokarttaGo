package com.apophis.maastokarttago;


import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.TextView;

import java.util.Locale;

public class AboutDlg {
    private AlertDialog mDlg;

    public AboutDlg(Context ctx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        LayoutInflater inflater = ((AppCompatActivity) ctx).getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_about, null));

        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Close clicked
            }
        });

        mDlg = builder.create();
    }

    public void show() {
        mDlg.show();
    }

    public void updateCoords(double lat, double lng) {
        TextView tv;
        tv = mDlg.findViewById(R.id.ab_lat);

        if (tv == null)
            return;

        tv.setText("N: " + String.format(Locale.ROOT, "%.6f", lat));
        tv = mDlg.findViewById(R.id.ab_lng);
        tv.setText("E: " + String.format(Locale.ROOT, "%.6f", lng));
    }
}

