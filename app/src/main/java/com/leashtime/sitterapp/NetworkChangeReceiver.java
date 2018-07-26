package com.leashtime.sitterapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println("Network Change Receiver received network change message");
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService (Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        int networkType;

        if (null != networkInfo) {
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                networkType = ConnectivityManager.TYPE_WIFI;
            } else if(networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                networkType = ConnectivityManager.TYPE_MOBILE;
            } else {

            }
        }
    }
}

