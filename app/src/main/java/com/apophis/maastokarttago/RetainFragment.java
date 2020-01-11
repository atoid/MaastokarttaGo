package com.apophis.maastokarttago;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.util.LruCache;

public class RetainFragment extends Fragment {
    private static final String TAG = "RetainFragment";
    public LruCache<String, Bitmap> retainedCache;

    public RetainFragment() {}

    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new RetainFragment();
            fm.beginTransaction().add(fragment, TAG).commit();
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public LruCache<String, Bitmap> getLruCache(int maxSize) {
        if (retainedCache == null) {
            Log.d(TAG, "Create LRU cache of size: " + maxSize);
            retainedCache = new LruCache<String, Bitmap>(maxSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };
        }

        return retainedCache;
    }
}