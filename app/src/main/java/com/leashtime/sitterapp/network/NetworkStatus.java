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
            //System.out.println("Network reachable");
            hasConnect  = true;
            //System.out.println("Has route to network");
//System.out.println("Network reachable, NO ROUTE");
            hasRouteToNetwork = hasNetworkConnection(context);
        } else {
            hasConnect = false;
            //System.out.println("Network NOT REACHABLE");
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

        /*Socket socket = new Socket();

        try {
            InetAddress host = InetAddress.getByName("173.203.157.32");
            socket.connect(new InetSocketAddress(host,80),5000);
            routeExists = true;
        } catch (IOException e) {
            routeExists = false;
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        routeExists = Boolean.TRUE;

        return connected && routeExists;

    }
}

