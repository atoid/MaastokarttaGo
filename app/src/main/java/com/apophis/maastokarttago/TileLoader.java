package com.apophis.maastokarttago;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;

interface TileLoadedCb {
    void setBitmap(Bitmap bm, int tag);
}

class LoadTileTask extends AsyncTask<String, Void, Bitmap> {
    final String TAG = "LOADTILETASK";
    String tileUrl;
    InputStream tileIs = null;
    WeakReference<TileLoadedCb> cbRef;
    int tag;

    Bitmap doLoad(String strUrl) {
        Bitmap bm = null;
        try {
            URL url = new URL(strUrl);
            tileIs = url.openStream();
            bm = BitmapFactory.decodeStream(tileIs);
        }
        catch (Exception e) {
            bm = null;
        }
        finally {
            closeInput();
        }
        return bm;
    }

    @Override
    protected Bitmap doInBackground(String... urls) {
        //Log.d(TAG, "Load url: " + urls[0]);
        tileUrl = urls[0];
        Bitmap bm = doLoad(tileUrl);

        if (!isCancelled() && bm != null && urls[1] != null) {
            Bitmap tmp = doLoad(urls[1]);
            if (!isCancelled() && tmp != null) {
                Bitmap bmOverlay = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), bm.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bm, 0, 0, null);
                canvas.drawBitmap(tmp, 0, 0, null);
                bm = bmOverlay;
            }
        }

        //Log.d(TAG, "Complete");
        return bm;
    }

    @Override
    protected void onPostExecute(Bitmap bm) {
        //Log.d(TAG, "Set to tile");
        TileLoadedCb cb = cbRef.get();
        if (cb != null) {
            cb.setBitmap(bm, tag);
        }
        TileLoader.updateCache(tileUrl, bm);
        TileLoader.setTaskDone();
    }

    @Override
    protected void onCancelled(Bitmap bm) {
        TileLoader.setTaskDone();
        closeInput();
    }

    void closeInput() {
        if (tileIs != null) {
            try {
                tileIs.close();
            }
            catch (IOException e) {
                Log.d(TAG, e.toString());
            }
            finally {
                tileIs = null;
            }
        }
    }
}

public class TileLoader {
    final String TAG = "TILELOADER";
    static LruCache<String, Bitmap> mCache;
    static int mNumTasks = 0;
    LoadTileTask[] mLoaderTasks;

    public static void updateCache(String tileUrl, Bitmap bm) {
        if (mCache != null && bm != null) {
            mCache.put(tileUrl, bm);
        }
    }

    public static void setTaskDone() {
        if (mNumTasks > 0) {
            mNumTasks--;
        }
    }

    public void setLruCache(LruCache<String, Bitmap> cache) {
        mCache = cache;
    }

    public void setMaxLoaders(int maxLoaders) {
        mLoaderTasks = new LoadTileTask[maxLoaders];
    }

    public void invalidateCache() {
        cancelLoads();
        if (mCache != null) {
            mCache.evictAll();
        }
    }

    public void cancelLoads() {
        for (int i = 0; i < mLoaderTasks.length; i++) {
            if (mLoaderTasks[i] != null) {
                mLoaderTasks[i].cancel(false);
            }
        }
    }

    public void load(TileLoadedCb cb, String[] urls, int tag) {
        // Cancel ongoing load
        if (mLoaderTasks[tag] != null) {
            mLoaderTasks[tag].cancel(false);
        }

        // Check for cached bitmap
        if (mCache != null) {
            Bitmap bm = mCache.get(urls[0]);
            if (bm != null) {
                //Log.d(TAG, "Cache hit: " + url);
                cb.setBitmap(bm, tag);
                return;
            }
        }

        // Load from url
        //Log.d(TAG, "Cache miss: " + url);
        LoadTileTask tmp = new LoadTileTask();
        mLoaderTasks[tag] = tmp;
        tmp.cbRef = new WeakReference<>(cb);
        tmp.tag = tag;

        try {
            if (++mNumTasks > 64) {
                //Log.d(TAG, "Use serial executor");
                tmp.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, urls);
            } else {
                //Log.d(TAG, "Use thread pool executor");
                tmp.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, urls);
            }
        }
        catch (RejectedExecutionException e) {
            setTaskDone();
            Log.w(TAG, "Executor failed");
        }
    }
}
