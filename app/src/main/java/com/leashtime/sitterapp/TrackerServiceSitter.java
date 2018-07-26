package com.leashtime.sitterapp;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.leashtime.sitterapp.jobs.BadSendJob;
import com.leashtime.sitterapp.jobs.UploadCoordinateJobService;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

public class TrackerServiceSitter extends Service
{
    public static TrackerServiceSitter service;
    private Context context;
    private Thread serviceThread;
    private static final int JOB_ID=1337;
    private static final int JOB_ID_BAD_SEND=1447;
    public static final int SMALL_ICON = R.drawable.network_icon_2x;
    //public static final int STOP_ACTION_ICON = R.drawable.network_icon_2x;

    private TrackerServiceSitter.MyServiceRunnable myServiceRunningThread;
    private VisitsAndTracking visitsAndTracking;
    public boolean isForeground;
    public IBinder serviceBinder = new RunServiceBinder();
    private static final int NOTIFICATION_ID = 4444;

    @TargetApi(26)
    public static class ONotification {
        public static final String CHANNEL_ONE_ID = "com.leashtime.sitterapp.ONE";
        public static final String CHANNEL_ONE_NAME = "Channel One";
        public static final int ONGOING_NOTIFICATION_ID = 5334;

        private static PendingIntent getLaunchActivityPI(Service context) {
            PendingIntent piLaunchMainActivity;
            int randNum =  new Random().nextInt(100000);
            Intent iLaunchMainActivity = new Intent(context, MainActivity.class);
            piLaunchMainActivity = PendingIntent.getActivity(context, randNum, iLaunchMainActivity, 0);
            return piLaunchMainActivity;
        }
        private static PendingIntent getStopServicePI(Service context) {
            PendingIntent piStopService;
            int randNum =  new Random().nextInt(100000);
            Intent iStopService = new Intent(context,TrackerServiceSitter.class);
            piStopService = PendingIntent.getService(context, randNum, iStopService, 0);
            return piStopService;
        }
        public static void createNotification(Service context) {
            String channelId = createChannel(context);
            Notification notification = buildNotification(context, channelId);
            context.startForeground(ONGOING_NOTIFICATION_ID, notification);
        }
        private static String createChannel(Service context) {
            NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence channelName = "GPS Tracker Channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,channelName,importance);
            notificationManager.createNotificationChannel(notificationChannel);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            return CHANNEL_ONE_ID;

        }
        private static Notification buildNotification(Service context, String channelId) {
            PendingIntent piLaunchMainActivity = getLaunchActivityPI(context);
            PendingIntent piStopService = getStopServicePI(context);

            Notification backNotification = new Notification.Builder(context,channelId)
                    .setContentTitle("GPS Track")
                    .setContentText("LeashTime background GPS tracking")
                    .setSmallIcon(SMALL_ICON)
                    .setContentIntent(piLaunchMainActivity)
                    .setStyle(new Notification.BigTextStyle())
                    .build();
            return backNotification;
        }
    }
    public class RunServiceBinder extends Binder {
        TrackerServiceSitter getService() {
            return TrackerServiceSitter.this;
        }
    }

    public TrackerServiceSitter(Context applicationContext) {
        service = this;
        context = applicationContext;
    }
    public TrackerServiceSitter()  {
        service = this;
    }
    public static TrackerServiceSitter getInstance() {
        return service;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isForeground = true;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        super.onStartCommand(intent, flags, startid);
        try {
            startServiceThread();
        } catch (SecurityException sE) {
            sE.printStackTrace();
        }
        return Service.START_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    public void startServiceThread() {
        System.out.println("SERVICE THREAD STARTED");
        setupCoordUploadJob();
        setupBadSendJob();
        myServiceRunningThread = new TrackerServiceSitter.MyServiceRunnable();
        serviceThread = new Thread(myServiceRunningThread);
        serviceThread.start();
    }

    public void foreground() {
        System.out.println("FOREGROUND SERVICE TRACKING");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TrackerServiceSitter.ONotification.createNotification(this);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }
    }
    public void background() {
        stopForeground(true);
    }
    public Notification createNotification () {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("LeashTime GPS Tracking")
                .setContentText("Tap to stop GPS tracking")
                .setSmallIcon(R.drawable.network_icon_2x);
        Intent resultIntent = new Intent(this, TrackerServiceSitter.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(MainApplication.getAppContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return builder.build();
    }
    private void setupCoordUploadJob() {
        JobScheduler uploadCoordsJob = (JobScheduler) MainApplication.getAppContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName component = new ComponentName(MainApplication.getAppContext(), UploadCoordinateJobService.class);
        JobInfo.Builder b=new JobInfo.Builder(JOB_ID,component);
        long interval = 900000L;
        PersistableBundle pb = new PersistableBundle();
        long MIN_NUM_COORDS_UPLOAD = 8L;
        pb.putLong("numberCoordinates", MIN_NUM_COORDS_UPLOAD);
        b.setExtras(pb);
        b.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        b.setPeriodic(interval);
        b.setPersisted(true);
        b.setRequiresCharging(false);
        b.setRequiresDeviceIdle(false);
        uploadCoordsJob.schedule(b.build());
    }
    private void setupBadSendJob() {
        JobScheduler checkBadSendJob = (JobScheduler) MainApplication.getAppContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName component2 = new ComponentName(MainApplication.getAppContext(),BadSendJob.class);
        JobInfo.Builder b = new JobInfo.Builder(JOB_ID_BAD_SEND,component2);
        long interval = 900000L;
        PersistableBundle bundleBadJob = new PersistableBundle();
        bundleBadJob.putString("badJob","jobs");
        b.setExtras(bundleBadJob);
        b.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        b.setPeriodic(interval);
        b.setPersisted(true);
        b.setRequiresCharging(false);
        b.setRequiresDeviceIdle(false);
        checkBadSendJob.schedule(b.build());
    }

    private static class MyServiceRunnable implements Runnable,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            LocationListener {

        public FusedLocationProviderClient fusedLocationProviderInstance;
        private GoogleApiClient mGoogleApiClient;
        private LocationRequest mLocationRequest;
        private Location mLastLocation;
        private String mLastUpdateTime;

        public static final int UPDATE_INTERVAL = 5000;
        public static final int FASTEST_UPDATE_INTERVAL = 5000;
        public static final float MIN_DISTANCE_CHANGE = 5.0f;
        public static final float MAX_DISTANCE_CHANGE = 2400.0f;
        public static final float MIN_ACCURACY = 75.0f;
        private VisitsAndTracking visitsAndTracking;
        private final SimpleDateFormat rightNowFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        /*public MyServiceRunnable(FusedLocationProviderClient fLPI) {
            this.fusedLocationProviderInstance = fLPI;
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    mLastLocation = locationResult.getLastLocation();
                    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                }
            };
        }*/

        public MyServiceRunnable() {
        }

        @Override
        public void run() {
            visitsAndTracking = VisitsAndTracking.getInstance();
            buildGoogleApiClient();
        }

        private void buildGoogleApiClient() {
            mGoogleApiClient = new GoogleApiClient.Builder(MainApplication.getAppContext())
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            mGoogleApiClient.connect();

        }

        @Override
        public void onConnected(Bundle bundle) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            mLocationRequest.setSmallestDisplacement(MIN_DISTANCE_CHANGE);
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);

            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                if(mLastLocation != null) {
                    this.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    visitsAndTracking.setLastValidLocation(mLastLocation);
                } else {

                }
            } catch (SecurityException sE) {
                sE.printStackTrace();
            }
        }
        @Override
        public void onConnectionSuspended(int i) {
            //System.out.println("Connection has been suspended");
        }
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
        @Override
        public void onLocationChanged(Location location) {
            if(null != location) {
                System.out.println("LOCATION COORDINATE: " + location.getLatitude() + ", " + location.getLongitude());
                mLastLocation = location;
                if (location.getAccuracy() < MIN_ACCURACY) {
                    float distFromLast = 0.0f;
                    if(visitsAndTracking.lastValidLocation != null) {
                        distFromLast = location.distanceTo(visitsAndTracking.lastValidLocation);
                    }
                    int numLocationCoord = visitsAndTracking.sessionLocationArray.size();
                    if(numLocationCoord > 0 && distFromLast < MAX_DISTANCE_CHANGE && distFromLast > MIN_DISTANCE_CHANGE) {
                        if (visitsAndTracking.visitData != null) {
                            visitsAndTracking.setLastValidLocation(location);
                        }
                    } else if(numLocationCoord < 1) {
                        visitsAndTracking.setLastValidLocation(location);
                    }
                }
            }


        }
        public void onStatusChanged(String provider, int status, Bundle extras) {

            //System.out.println("LOCATION LISTENER STATUS CHANGE");

        }
        public void onProviderEnabled(String provider) {

            //System.out.println("PROVIDER FOR LOCATION ENBABLED");

        }
        public void onProviderDisabled(String provider) {
            //System.out.println("PROVIDER FOR LOCATION DISABLED");
        }
        public void stopLocationUpdates() {
            //fusedLocationProviderInstance.removeLocationUpdates(mLocationCallback);

        }
    }
}
