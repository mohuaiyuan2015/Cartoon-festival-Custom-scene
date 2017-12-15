package com.tobot.tobot.control.demand;


import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;

import com.tobot.tobot.utils.CommonRequestManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by YF-04 on 2017/11/10.
 */

public class DemandTools {
    private static final String TAG = "DemandTools";

    /**
     * okHttpClient对象
     */
    private OkHttpClient mOkHttpClient;
    private Callback mCallback;

    private Context mContext;
    private Handler handler;

    public DemandTools(Context context) {
        this.mContext=context;
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient();
        }
    }


    public void download(String url)throws Exception {
        boolean downloadResult = false;
        if (url==null || url.length()<1 || url.trim().length()<1){
            throw new Exception("Illegal argument of  url !");
        }

        Request request = new Request.Builder().url(url).build();
        mOkHttpClient.newCall(request).enqueue(mCallback);

    }

    public void download(String url,Callback callback)throws Exception {
        boolean downloadResult = false;
        if (url==null || url.length()<1 || url.trim().length()<1){
            throw new Exception("Illegal argument of  url !");
        }

        Request request = new Request.Builder().url(url).build();
        mOkHttpClient.newCall(request).enqueue(callback);

    }


    public Callback getmCallback() {
        return mCallback;
    }

    public void setmCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }



}
