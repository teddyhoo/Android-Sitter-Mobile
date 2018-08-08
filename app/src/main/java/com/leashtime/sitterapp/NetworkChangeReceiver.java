package com.leashtime.sitterapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.leashtime.sitterapp.events.StatusChangeEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Network Change Receiver CLASS BroadCastReceiver");
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService (Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        int networkType;

        if (null != networkInfo &&   networkInfo.isConnectedOrConnecting()) {
            if (isNetworkAvailable()) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    networkType = ConnectivityManager.TYPE_WIFI;
                    EventBus.getDefault().post(new StatusChangeEvent("network", "yes"));
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    networkType = ConnectivityManager.TYPE_MOBILE;
                    EventBus.getDefault().post(new StatusChangeEvent("network", "yes"));
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIMAX) {
                    networkType = ConnectivityManager.TYPE_WIMAX;
                    EventBus.getDefault().post(new StatusChangeEvent("network", "yes"));
                } else {
                    EventBus.getDefault().post(new StatusChangeEvent("network", "no"));
                }
            } else {
                EventBus.getDefault().post(new StatusChangeEvent("network","no"));
            }
        }
    }

    public  boolean isNetworkAvailable () {
        System.out.println("Is network available called");
        boolean success = false;
        try {
            URL url = new URL("https://google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.connect();
            success = connection.getResponseCode() == 200;
            System.out.println("Internet is available");
        } catch (IOException e) {
            System.out.println("Error checking internet connection" + e);
            return false;
        }
        return success;
    }
}

