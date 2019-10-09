package com.apophis.maastokarttago;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.BatteryManager;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

class AppControls {
    private static final String TAG = "CTRL";
    MainActivity mApp;
    float mMovex, mMovey;

    AppControls(MainActivity app) {
        mApp = app;
    }

    void init() {
        TextView tv = mApp.findViewById(R.id.map_ruler);
        tv.bringToFront();
        updateRuler();

        final ImageView iw_dir = mApp.findViewById(R.id.dir_icon);
        iw_dir.setImageResource(R.drawable.direction);
        iw_dir.setX(mApp.mWidth / 2 - iw_dir.getWidth() / 2);
        iw_dir.setY(mApp.mHeight / 2 - iw_dir.getHeight() / 2);

        final ImageView iw = mApp.findViewById(R.id.ctr_icon);
        iw.setX(mApp.mWidth / 2 - iw.getWidth() / 2);
        iw.setY(mApp.mHeight / 2 - iw.getHeight() / 2);
        iw.bringToFront();
        iw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mApp.mSettings.follow = !mApp.mSettings.follow;
                setFollowOnOff(iw, iw_dir);
            }
        });
        iw_dir.bringToFront();
        setFollowOnOff(iw, iw_dir);

        final int orange = Color.parseColor(mApp.COLOR_ORANGE);
        if (mApp.mSettings.orangeon) {
            iw.setColorFilter(orange);
            iw_dir.setColorFilter(orange);
        }

        ImageButton ib;
        ib = mApp.findViewById(R.id.btn_zoom_in);
        ib.bringToFront();
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mApp.mSettings.zoom < mApp.MAX_ZOOM) {
                    mApp.cancelLoads();
                    mApp.updateCoords();
                    mApp.mSettings.zoom++;
                    mApp.update();
                }
            }
        });

        ib = mApp.findViewById(R.id.btn_zoom_out);
        ib.bringToFront();
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mApp.mSettings.zoom > mApp.MIN_ZOOM) {
                    mApp.cancelLoads();
                    mApp.updateCoords();
                    mApp.mSettings.zoom--;
                    mApp.update();
                }
            }
        });

        ib = mApp.findViewById(R.id.btn_markers);
        ib.bringToFront();
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mApp, MarkersActivity.class);
                mApp.updateCoords();
                double[] coords = {mApp.mSettings.lat, mApp.mSettings.lng};
                intent.putExtra("coords", coords);
                mApp.startActivityForResult(intent, mApp.MARKER_REQUEST);
            }
        });

        final SeekBar sb = mApp.findViewById(R.id.sb_brightness);
        sb.bringToFront();
        sb.setProgress(mApp.mSettings.alpha);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mApp.mSettings.alpha = i;
                mApp.setBrightness(mApp.mSettings.alpha);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final PopupMenu popup = new PopupMenu(mApp, mApp.findViewById(R.id.btn_maps));

        final PopupMenu.OnMenuItemClickListener listener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();

                // Submenu items
                if (id == R.id.mi_screen || id == R.id.mi_settings) {
                    return false;
                }

                // Direction indicator
                if (id == R.id.mi_diron) {
                    mApp.mSettings.diron = !menuItem.isChecked();
                    menuItem.setChecked(mApp.mSettings.diron);
                    setDirOnOff();
                    return true;
                }

                // Ruler indicator
                if (id == R.id.mi_ruleron) {
                    mApp.mSettings.ruleron = !menuItem.isChecked();
                    menuItem.setChecked(mApp.mSettings.ruleron);
                    setRulerOnOff();
                    return true;
                }

                // Speed indicator
                if (id == R.id.mi_speedon) {
                    mApp.mSettings.speedon = !menuItem.isChecked();
                    menuItem.setChecked(mApp.mSettings.speedon);
                    setSpeedOnOff();
                    return true;
                }

                // Orange pointer
                if (id == R.id.mi_orangeon) {
                    mApp.mSettings.orangeon = !menuItem.isChecked();
                    menuItem.setChecked(mApp.mSettings.orangeon);
                    if (mApp.mSettings.orangeon) {
                        iw.setColorFilter(orange);
                        iw_dir.setColorFilter(orange);
                    }
                    else {
                        iw.clearColorFilter();
                        iw_dir.clearColorFilter();
                    }
                    return true;
                }

                // Rotate map
                if (id == R.id.mi_rotateon) {
                    mApp.mSettings.rotateon = !menuItem.isChecked();
                    menuItem.setChecked(mApp.mSettings.rotateon);
                    setRotateOnOff(mApp.mSettings.rotateon);
                    return true;
                }

                // About
                if (id == R.id.mi_about) {
                    mApp.mAboutDlg.show();
                    if (!mApp.mSettings.follow) {
                        double[] ll = mApp.getLatLng(mApp.findCenterTile());
                        mApp.mAboutDlg.updateCoords(ll[0], ll[1]);
                    }
                    return true;
                }

                menuItem.setChecked(true);

                // Screen modes
                if (id == R.id.mi_screenoff) {
                    mApp.mSettings.screenmode = mApp.SCREEN_MODE_OFF;
                    setScreenOnOff();
                    return true;
                }
                if (id == R.id.mi_screenchg) {
                    mApp.mSettings.screenmode = mApp.SCREEN_MODE_CGH;
                    setScreenOnOff();
                    return true;
                }
                if (id == R.id.mi_screenon) {
                    mApp.mSettings.screenmode = mApp.SCREEN_MODE_ON;
                    setScreenOnOff();
                    return true;
                }

                // Map sources
                if (id == R.id.mi_orto) {
                    mApp.mSettings.url = mApp.ORTO_URL;
                }
                else if (id == R.id.mi_tausta) {
                    mApp.mSettings.url = mApp.TAUSTA_URL;
                }
                else {
                    mApp.mSettings.url = mApp.DEFAULT_URL;
                }

                mApp.updateCoords();
                mApp.update();
                return true;
            }
        };

        popup.setOnMenuItemClickListener(listener);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.maps, popup.getMenu());

        Menu m = popup.getMenu();
        MenuItem mi;
        if (mApp.mSettings.url.equals(mApp.TAUSTA_URL)) {
            mi = m.findItem(R.id.mi_tausta);
        }
        else if (mApp.mSettings.url.equals(mApp.ORTO_URL)) {
            mi = m.findItem(R.id.mi_orto);
        }
        else {
            mi = m.findItem(R.id.mi_maasto);
        }
        mi.setChecked(true);

        if (mApp.mSettings.screenmode == mApp.SCREEN_MODE_OFF) {
            mi = m.findItem(R.id.mi_screenoff);
        }
        else if (mApp.mSettings.screenmode == mApp.SCREEN_MODE_CGH) {
            mi = m.findItem(R.id.mi_screenchg);
        }
        else {
            mi = m.findItem(R.id.mi_screenon);
        }
        mi.setChecked(true);
        setScreenOnOff();

        mi = m.findItem(R.id.mi_diron);
        mi.setChecked(mApp.mSettings.diron);
        setDirOnOff();

        mi = m.findItem(R.id.mi_ruleron);
        mi.setChecked(mApp.mSettings.ruleron);
        setRulerOnOff();

        mi = m.findItem(R.id.mi_speedon);
        mi.setChecked(mApp.mSettings.speedon);
        setSpeedOnOff();

        mi = m.findItem(R.id.mi_orangeon);
        mi.setChecked(mApp.mSettings.orangeon);

        mi = m.findItem(R.id.mi_rotateon);
        mi.setChecked(mApp.mSettings.rotateon);

        final ImageButton maps = mApp.findViewById(R.id.btn_maps);
        maps.bringToFront();
        maps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.show();
            }
        });
    }

    void setFollowOnOff(ImageView iwCtr, ImageView iwDir) {
        if (mApp.mSettings.follow) {
            iwCtr.setImageResource(R.drawable.center);
            iwDir.setImageAlpha(255);
        }
        else {
            iwCtr.setImageResource(R.drawable.centeroff);
            iwDir.setImageAlpha(0);
            setRotateOnOff(false);
        }
    }

    void setScreenOnOff() {
        if (mApp.mSettings.screenmode == mApp.SCREEN_MODE_OFF) {
            mApp.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else if (mApp.mSettings.screenmode == mApp.SCREEN_MODE_CGH) {
            if (isCharging()) {
                mApp.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            else {
                mApp.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
        else {
            mApp.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    void setRotateOnOff(boolean rotate) {
        if (!rotate) {
            mApp.setRotation(0.f, 0.f);
            if (mApp.mInitialized) {
                mApp.update(mApp.mSettings.lat, mApp.mSettings.lng);
            }
        }
    }

    void setDirOnOff() {
        ImageView iw = mApp.findViewById(R.id.dir_icon);
        if (mApp.mSettings.diron) {
            iw.setVisibility(View.VISIBLE);
        }
        else {
            iw.setVisibility(View.INVISIBLE);
        }
    }

    void setRulerOnOff() {
        TextView tv = mApp.findViewById(R.id.map_ruler);
        if (mApp.mSettings.ruleron) {
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.INVISIBLE);
        }
    }

    void setSpeedOnOff() {
        TextView iw = mApp.findViewById(R.id.map_speed);
        if (mApp.mSettings.speedon) {
            iw.setVisibility(View.VISIBLE);
            iw.bringToFront();
        }
        else {
            iw.setVisibility(View.INVISIBLE);
        }
        iw.setY((mApp.mHeight - iw.getHeight()) / 2.f);
    }

    void updateRuler() {
        final double[] r_res  = { 1222.9900, 611.4962, 305.7481, 152.8741, 76.4370, 38.2185, 19.1093, 9.5546, 4.7773, 2.3887, 1.1943, 0.5972, 0.2986 };
        final int[] r_meters  = { 160000, 80000, 40000, 20000, 10000, 5000, 2000, 1000, 500, 250, 100, 50, 25};
        final String[] r_text = {"160km", "80km", "40km", "20km", "10km", "5km", "2km", "1km", "500m", "250m", "100m", "50m", "25m" };

        TextView tv = mApp.findViewById(R.id.map_ruler);
        int rulerx = (int) tv.getX();
        int rulery = (int) tv.getY();

        double r = r_res[mApp.mSettings.zoom - mApp.MIN_ZOOM];
        double lat = mApp.getLatLng(mApp.findTileAt(rulerx, rulery), rulerx, rulery)[0];
        //Log.d(TAG, "Ruler at lat: " + lat);
        r = r * Math.cos(Math.toRadians(lat));
        double width = r_meters[mApp.mSettings.zoom - mApp.MIN_ZOOM] / r;
        width *= (mApp.mTileSz / 256.0);

        tv.setWidth((int) Math.round(width));
        tv.setText(r_text[mApp.mSettings.zoom - mApp.MIN_ZOOM]);
    }

    boolean isCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mApp.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        return isCharging;
    }

    public boolean onTouchEvent(MotionEvent event) {
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

                int dx = (int) (nx - mMovex);
                int dy = (int) (ny - mMovey);

                Matrix mtx = new Matrix();
                mtx.setRotate(mApp.mRotation);
                float[] pts = {dx, dy};
                mtx.mapPoints(pts);
                mApp.moveTiles(Math.round(pts[0]), Math.round(pts[1]), true);

                mMovex = nx;
                mMovey = ny;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateRuler();
                if (!mApp.mSettings.follow) {
                    mApp.updateCoords();
                    mApp.mMarkers.update(mApp.mSettings.lat, mApp.mSettings.lng);
                }
                break;
        }
        return true;
    }
}
