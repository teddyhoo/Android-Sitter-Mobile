package com.leashtime.sitterapp.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStatus {

    private final Context networkContext;
    public Boolean hasConnect;
    public Boolean hasRouteToNetwork;

    public NetworkStatus(Context context) {
        networkContext = context;
        hasConnect= false;
        hasRouteToNetwork = false;

        if(isNetworkReachable()) {
            hasConnect  = true;
            hasRouteToNetwork = hasNetworkConnection(context);
        } else {
            hasConnect = false;
        }
    }

    private boolean isNetworkReachable() {
        ConnectivityManager manager = (ConnectivityManager)networkContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo != null;
    }

    private  boolean hasNetworkConnection(Context context) {

        ConnectivityManager manager = (ConnectivityManager)networkContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean connected = null != networkInfo &&  networkInfo.isConnected();
        if (!connected) return false;

        boolean routeExists;

        routeExists = Boolean.TRUE;

        return connected && routeExists;

    }
}

