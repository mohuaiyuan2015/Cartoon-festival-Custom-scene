package com.tobot.tobot.base;

import android.app.Application;
//import android.support.multidex.MultiDexApplication;

import com.tobot.tobot.db.bean.UserDBManager;
import com.tobot.tobot.db.model.User;

import butterknife.OnClick;

/**
 * Created by Javen on 2017/7/10.
 */

public class TobotApplication extends Application {

    private static TobotApplication instance;
    private User currentUser;

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        instance = this;
    }

    public synchronized static TobotApplication getInstance() {
        if (null == instance) {
            instance = new TobotApplication();
        }
        return instance;
    }

    public User getCurrentUser() {
        if (currentUser == null) {
            currentUser = UserDBManager.getManager().getCurrentUser();
        }
        return currentUser;
    }

    @Override
    public void onTerminate() {
        // 程序终止的时候执行
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        // 低内存的时候执行
        super.onLowMemory();
        System.gc();
    }

    @Override
    public void onTrimMemory(int level) {
        // 程序在内存清理的时候执行
        super.onTrimMemory(level);

    }

}
