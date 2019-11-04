package com.apophis.maastokarttago;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

class UrlDlg {
    private AlertDialog mDlg;
    private MainActivity mCtx;

    UrlDlg(MainActivity ctx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setView(R.layout.dialog_url);

        builder.setNegativeButton(R.string.save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                EditText et;
                et = mDlg.findViewById(R.id.own_url);
                String url = et.getText().toString();
                mCtx.mSettings.ownurl = url;
                mCtx.mSettings.url = url;
                mCtx.update();
            }
        });

        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        mDlg = builder.create();
        mCtx = ctx;
    }

    void show() {
        mDlg.show();
        EditText et;
        et = mDlg.findViewById(R.id.own_url);
        if (et != null) {
            et.setText(mCtx.mSettings.ownurl);
        }
    }
}
