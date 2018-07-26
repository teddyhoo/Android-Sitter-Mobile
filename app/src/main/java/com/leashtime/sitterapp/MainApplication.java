package com.leashtime.sitterapp;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.StrictMode;

public class MainApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static MainApplication mInstance;
    private static Context appContext;
    private static final String TAG = MainApplication.class.getName();
    public static String stateOfLifeCycle = "";
    public static boolean wasInBackground;
    MainApplication.ScreenOffReceiver screenOffReceiver = new MainApplication.ScreenOffReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        registerReceiver(screenOffReceiver, new IntentFilter(
                "android.intent.action.SCREEN_OFF"
        ));

        //TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Lato-Light.ttf"); // font from assets: "assets/fonts/Roboto-Regular.ttf
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        appContext = this;
        mInstance = this;
        VisitsAndTracking sVisitsAndTracking = VisitsAndTracking.getInstance();
        sVisitsAndTracking.init(appContext);

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle arg1) {
        wasInBackground = false;
        stateOfLifeCycle = "Create";
    }
    @Override
    public void onActivityStarted(Activity activity){
        stateOfLifeCycle = "Start";
    }

    @Override
    public void onActivityResumed(Activity activity) {
        stateOfLifeCycle = "Resume";
    }

    @Override
    public void onActivityPaused(Activity activity) {
        stateOfLifeCycle = "Pause";
    }

    @Override
    public void onActivityStopped(Activity activity) {
        stateOfLifeCycle = "Stop";
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle arg1) {

    }
    @Override
    public void onActivityDestroyed(Activity activity){
        wasInBackground = false;
        stateOfLifeCycle = "Destroy";
    }

    @Override
    public void onTrimMemory(int level) {
        // System.out.println("Trim memory with level: " + level);
        if(stateOfLifeCycle.equals("Stop")) {
            wasInBackground = true;
        }
        super.onTrimMemory(level);
    }
    public static Context getAppContext() {
        return appContext;
    }
    public static synchronized MainApplication getInstance() {
        return mInstance;
    }

    public class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            wasInBackground = true;
        }
    }
}

