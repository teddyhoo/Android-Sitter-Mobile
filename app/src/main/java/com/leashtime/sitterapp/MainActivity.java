package com.leashtime.sitterapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.leashtime.sitterapp.events.FailedParseEvent;
import com.leashtime.sitterapp.events.LoginEvent;
import com.leashtime.sitterapp.events.ReloadVisitsEvent;
import com.leashtime.sitterapp.events.StatusChangeEvent;
import com.leashtime.sitterapp.network.SendPhotoServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class MainActivity extends android.support.v7.app.AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();
    private VisitsAndTracking sVisitsAndTracking;
    private SharedPreferences mPreferences;
    private TrackerServiceSitter trackerServiceSitter;
    public AlarmManager visitUpdate;
    private BroadcastReceiver receiver;
    private Context mContext;

    public ListView list;
    private android.support.v7.widget.RecyclerView recyclerView = null;
    private VisitAdapter visitAdapter = null;

    private MenuItem dateDisplay;
    private TextView noVisitTextView;
    private TextView dateText;
    private TextView monthText;
    public TextView networkStatusView;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", new Locale("US"));
    private static final SimpleDateFormat nextPrevDateFormat = new SimpleDateFormat("M/d/yyyy", new Locale("US"));
    public SimpleDateFormat formatDateComparison;

    public boolean pollingUpdate;
    public boolean initialLogin;
    public boolean initialLogin2;
    private boolean serviceBound;
    public boolean isBackground;

    private static final int POLLING_INTERVAL = 1200000; //20 minute
    private static final int delay = 1200000;
    private static final int JOB_UPDATE_ID = 1001;
    private static final int ONE_DAY = 1000 * 60 * 60 * 24;
    private static final int ONE_HOUR = ONE_DAY / 24;
    private static final int ONE_MINUTE = ONE_HOUR / 60;
    private static final int ONE_SECOND = ONE_MINUTE / 60;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackerServiceSitter.RunServiceBinder binder = (TrackerServiceSitter.RunServiceBinder) service;
            trackerServiceSitter = binder.getService();
            serviceBound = true;
            trackerServiceSitter.background();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private static class MainUiThreadUpdate extends Thread {

        WeakReference<MainActivity> activity;
        String username;
        String password;

        public MainUiThreadUpdate(MainActivity activity, String username, String password) {
            this.activity = new WeakReference<>(activity);
            this.username = username;
            this.password = password;
        }

        @Override
        public void run() {
            MainActivity myActivity = activity.get();
            myActivity.runOnUiThread(new MainActivity.MainUiThreadUpdate.MyRunnable(myActivity));
            myActivity.loginWithNewCredentials(username, password);
        }

        private static class MyRunnable implements Runnable {
            private final MainActivity myActivity;

            public MyRunnable(MainActivity myActivity) {
                this.myActivity = myActivity;
            }

            @Override
            public void run() {
                myActivity.populateAdapter();
                myActivity.pollingUpdate = false;
            }
        }
    }

    private static class SetupThread extends Thread {
        WeakReference<MainActivity> activity;
        public SetupThread(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }
        @Override
        public void run() {
            MainActivity myActivity = activity.get();
            myActivity.runOnUiThread(new MainActivity.SetupThread.MyRunnable(myActivity));
        }
        private static class MyRunnable implements Runnable {
            private final MainActivity myActivity;
            public MyRunnable(MainActivity myActivity) {
                this.myActivity = myActivity;
            }
            @Override
            public void run() {
                myActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                myActivity.connectServiceFirstTime();
                myActivity.setContentView(R.layout.main_list_visits);
                myActivity.recyclerView = myActivity.findViewById(R.id.recycler_view);
                myActivity.addToolbar();

                if (myActivity.checkNetworkConnection()) {
                    myActivity.launchLogin("noConnection");
                } else {
                    myActivity.launchLogin("connection");
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Lato-Regular.ttf");
        mContext = this.getApplicationContext();
        sVisitsAndTracking = VisitsAndTracking.getInstance();
        initialLogin = TRUE;
        initialLogin2 = TRUE;
        pollingUpdate = FALSE;
        isBackground = FALSE;
        formatDateComparison = new SimpleDateFormat("yyyyMMdd");

        MainActivity.SetupThread threadSetup = new MainActivity.SetupThread(this);
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        mainThreadHandler.post(threadSetup);
        EventBus.getDefault().register(this);

    }
    public void populateAdapter() {
        if (recyclerView == null) {
            recyclerView = findViewById(R.id.recycler_view);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        visitAdapter = new VisitAdapter(this, sVisitsAndTracking.visitData);
        if (recyclerView.getAdapter() != null) {
            recyclerView.setAdapter(visitAdapter);
        } else {
            recyclerView.swapAdapter(visitAdapter, TRUE);
        }
        visitAdapter.notifyDataSetChanged();
    }

    public Date getToday() {
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.HOUR, 1);
        c2.set(Calendar.MINUTE, 0);
        c2.set(Calendar.SECOND, 0);
        c2.set(Calendar.MILLISECOND, 0);
        Date todayRightNow = new Date();
        return todayRightNow;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (checkNetworkConnection()) {
            resendBadRequest();
            if (MainApplication.wasInBackground) {
                Date todayRightNow = getToday();
                if (dateText != null && monthText != null) {
                    setToolbarDate();
                }
                Toast.makeText(getApplicationContext(), "UPDATING VISITS, PLEASE WAIT", Toast.LENGTH_LONG).show();
                MainApplication.wasInBackground = false;
                if (!initialLogin && !initialLogin2) {
                    pollingUpdate = true;
                    getVisits(sVisitsAndTracking.mPreferences.getString("username",""), sVisitsAndTracking.mPreferences.getString("password",""), daysBefore(), daysLater(), "1");
                }

                sVisitsAndTracking.onWhichDate = todayRightNow;
                sVisitsAndTracking.showingWhichDate = todayRightNow;
                sVisitsAndTracking.todayDateFormat = sVisitsAndTracking.formatter.format(sVisitsAndTracking.onWhichDate);
                sVisitsAndTracking.showingDateFormat = sVisitsAndTracking.formatter.format(sVisitsAndTracking.showingWhichDate);

            }
        } else {
            Toast.makeText(MainApplication.getAppContext(), "NO NETWORK CONNECTION", Toast.LENGTH_SHORT).show();
        }

        //if (!serviceBound) {
            // Duplicatte calls to Service TrackerServiceSitter when initially staring up
            Intent intent = new Intent(this, TrackerServiceSitter.class);
            startService(intent);
            bindService(intent, mConnection, 0);
            serviceBound = true;
        //}
    }
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (checkNetworkConnection()) {

                }
            }
        };
        this.registerReceiver(receiver, intentFilter);
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        if (!initialLogin && !initialLogin2) {
            if (serviceBound) {
                trackerServiceSitter.foreground();
            } else {
                stopService(new Intent(this, TrackerServiceSitter.class));
            }
            unbindService(mConnection);
            serviceBound = false;
        }

        if (visitAdapter != null) {
            System.out.println("Visit adapter is NOT null");
        }

        this.unregisterReceiver(this.receiver);
        System.out.println("Main Activity ON STOP");
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        cleanVisitData();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
    public void connectServiceFirstTime() {
        if (canAccessLocation()) {
            Intent intent = new Intent(this, TrackerServiceSitter.class);
            startService(intent);
            bindService(intent, mConnection, 0);
            serviceBound = true;
        }
    }
    public void loginWithNewCredentials(final String username, final String password) {
        if (initialLogin && initialLogin2) {
            Date today = new Date();
            String dateBeginEnd = dateFormat.format(today);
            getVisits(username, password, dateBeginEnd, dateBeginEnd, "1");
        } else if (!initialLogin && initialLogin2) {
            getVisits(username, password, daysBefore(), daysLater(), "1");
        }
    }


    private void getVisits(final String username, final String password, final String dateForVisits, final String endDate, final String firstLogin) {
        HttpUrl.Builder urlBuilderGetVisits = HttpUrl.parse("https://leashtime.com/native-prov-multiday-list.php")
                .newBuilder();
        urlBuilderGetVisits.addQueryParameter("loginid", username);
        urlBuilderGetVisits.addQueryParameter("password", password);
        urlBuilderGetVisits.addQueryParameter("start", dateForVisits);
        urlBuilderGetVisits.addQueryParameter("end", endDate);
        urlBuilderGetVisits.addQueryParameter("firstLogin", firstLogin);
        urlBuilderGetVisits.addQueryParameter("clientdocs", "complete");

        String url = urlBuilderGetVisits.build().toString();

        if (sVisitsAndTracking.USER_AGENT == null) {
            sVisitsAndTracking.USER_AGENT = "LeashTime Android / null user agent";
        }

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", sVisitsAndTracking.USER_AGENT)
                .build();

        System.out.println(request);
        if (initialLogin || initialLogin2 || pollingUpdate) {

            if (recyclerView != null) {
                recyclerView.setLayoutFrozen(FALSE);
                recyclerView.removeAllViewsInLayout();
            }

            client.newCall(request).enqueue(new Callback() {
                ResponseBody getVisitResponseBody;

                @Override
                public void onFailure(Call call, IOException e) {
                    String message = "LOGIN FAILED";
                    e.printStackTrace();
                    if (pollingUpdate) {
                        pollingUpdate = false;
                    } else if (initialLogin && initialLogin2) {
                        launchLogin("noConnection");
                    } else if (!initialLogin && initialLogin2) {

                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    getVisitResponseBody = response.body();
                    String responseData = response.body().string();
                    String loginCodeString = checkLoginCode(responseData);

                    if (!response.isSuccessful()) {
                        sVisitsAndTracking.lastLoginResponseCode = "NETWORK UNAVAILABLE";
                        launchLogin("noConnection");
                        throw new IOException("Unexpected code " + response);
                    } else {
                        if (loginCodeString.equals("OK")) {
                            if (initialLogin && initialLogin2) {
                                sVisitsAndTracking.onWhichVisitID = "0000";
                                sVisitsAndTracking.parseResponseVisitData(responseData);
                                sVisitsAndTracking.prefSetUserName(username);
                                sVisitsAndTracking.prefSetPass(password);
                                initialLogin = false;

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        populateAdapter();
                                    }
                                });
                                Runnable secondRequestRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        loginWithNewCredentials(username, password);
                                    }
                                };
                                new Thread(secondRequestRunnable).start();
                                getVisitResponseBody.close();
                            } else if (!initialLogin && initialLogin2) {
                                initialLogin2 = false;
                                sVisitsAndTracking.parseSecondVisitRequest(responseData);
                                getVisitResponseBody.close();
                            } else if (!initialLogin && !initialLogin2 && pollingUpdate) {
                                sVisitsAndTracking.parsePollingUpdate(responseData);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        populateAdapter();
                                    }
                                });
                            }
                        } else {
                            launchLogin("noConnection");
                        }
                    }
                }
            });
        }
    }
    private void launchLogin(String networkStatus) {
        Bundle basket = new Bundle();
        basket.putString("firstLogin", "yes");
        basket.putString("networkStatus", networkStatus);
        Intent loginIntent = new Intent(this.getApplicationContext(), LoginActivity.class);
        loginIntent.putExtras(basket);
        startActivity(loginIntent);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int pollingUpdateID = 11111;
        resendBadRequest();
        if (requestCode == JOB_UPDATE_ID) {
            if (!pollingUpdate) {
                if (checkNetworkConnection()) {
                    pollingUpdate = true;
                    Date today = new Date();
                    String todayString = formatDateComparison.format(today);
                    String onWhichString = formatDateComparison.format(sVisitsAndTracking.onWhichDate);
                    if (!todayString.equals(onWhichString)) {
                        sVisitsAndTracking.onWhichDate = today;
                        sVisitsAndTracking.showingWhichDate = today;
                        sVisitsAndTracking.todayDateFormat = sVisitsAndTracking.formatter.format(sVisitsAndTracking.onWhichDate);
                        sVisitsAndTracking.showingDateFormat = sVisitsAndTracking.formatter.format(sVisitsAndTracking.showingWhichDate);
                        getVisits(sVisitsAndTracking.mPreferences.getString("username",""), sVisitsAndTracking.mPreferences.getString("password",""), daysBefore(), daysLater(), "1");
                    } else {
                        getVisits(sVisitsAndTracking.mPreferences.getString("username",""), sVisitsAndTracking.mPreferences.getString("password",""), daysBefore(), daysLater(), "0");
                    }
                }
            }
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFailedParseEvent(FailedParseEvent event) {
        if (sVisitsAndTracking.lastLoginResponseCode.equals("BAD USERNAME/PASSWORD")) {
            launchLogin("noConnection");
        } else {
            getVisits(event.userName, event.passWord, daysBefore(), daysLater(), "1");
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginEvent(LoginEvent event) {
        loginWithNewCredentials(event.username, event.password);
        visitPollingUpdates();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReloadVisitsEvent(ReloadVisitsEvent event) {
        //setToolbarDate();
        MainActivity.MainUiThreadUpdate updateList = new MainActivity.MainUiThreadUpdate(this, sVisitsAndTracking.USERNAME, sVisitsAndTracking.PASSWORD);
        updateList.start();
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        mainThreadHandler.post(updateList);
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStatusChangeEvent(StatusChangeEvent status) {
        Toast.makeText(this, "ACQUIRED NEW COORDINATE", Toast.LENGTH_LONG).show();
    }
    public void visitPollingUpdates() {

        PendingIntent visitUpdatePending = createPendingResult(JOB_UPDATE_ID, new Intent(), 0);
        visitUpdate = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= 23) {
            visitUpdate.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + POLLING_INTERVAL,
                    POLLING_INTERVAL,
                    visitUpdatePending);
        } else {
            visitUpdate.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay,
                    delay,
                    visitUpdatePending);
        }
    }
    private void resendBadRequest() {
        for (final VisitDetail visitDetail : sVisitsAndTracking.visitData) {
            if (visitDetail.currentArriveStatus.equals("FAIL")) {
                Toast.makeText(MainApplication.getAppContext(), "BAD REQUEST RESEND ARRIVE: " + visitDetail.appointmentid, Toast.LENGTH_SHORT).show();
                reSendArriveRequest(visitDetail);
            }
            if (visitDetail.currentCompleteStatus.equals("FAIL")) {
                Toast.makeText(MainApplication.getAppContext(), "BAD REQUEST RESEND COMPLETE: " + visitDetail.appointmentid, Toast.LENGTH_SHORT).show();
                reSendCompleteRequest(visitDetail);
            }
            if (visitDetail.visitReportUploadStatus.equals("FAIL")) {
                Toast.makeText(MainApplication.getAppContext(), "BAD REQUEST VISIT REPORT: " + visitDetail.appointmentid, Toast.LENGTH_SHORT).show();
                client.newCall(visitDetail.visitReportRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        visitDetail.visitReportUploadStatus = "SUCCESS";
                        visitDetail.visitReportRequest = null;
                    }
                });
            }
            if (visitDetail.imageUploadStatus.equals("FAIL")) {
                Toast.makeText(MainApplication.getAppContext(), "BAD REQUEST RESEND PHOTO IMAGE: " + visitDetail.appointmentid, Toast.LENGTH_SHORT).show();
                SendPhotoServer photoUpload = new SendPhotoServer(sVisitsAndTracking.mPreferences.getString("password",""), sVisitsAndTracking.mPreferences.getString("password",""), visitDetail, "petPhoto");
            }
            if (visitDetail.mapSnapUploadStatus.equals("FAIL")) {
                Toast.makeText(MainApplication.getAppContext(), "BAD REQUEST RESEND MAP SNAPSHOT: " + visitDetail.appointmentid, Toast.LENGTH_SHORT).show();
                SendPhotoServer photoUpload = new SendPhotoServer(sVisitsAndTracking.mPreferences.getString("password",""), sVisitsAndTracking.mPreferences.getString("password",""), visitDetail, "map");
            }
        }

        if (!sVisitsAndTracking.resendCoordUploadRequest.isEmpty()) {
            Toast.makeText(MainApplication.getAppContext(), "BAD REQUEST COORDINATE UPLOAD", Toast.LENGTH_SHORT).show();

            for (final Request request : sVisitsAndTracking.resendCoordUploadRequest) {
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        sVisitsAndTracking.resendCoordUploadRequest.remove(request);
                    }
                });
            }
        }
    }
    public void reSendArriveRequest(final VisitDetail visitResend) {

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.leashtime.com/native-visit-action.php")
                .newBuilder();
        urlBuilder.addQueryParameter("loginid", sVisitsAndTracking.mPreferences.getString("username",""));
        urlBuilder.addQueryParameter("password", sVisitsAndTracking.mPreferences.getString("password",""));
        urlBuilder.addQueryParameter("datetime", visitResend.arrived);
        urlBuilder.addQueryParameter("coords", "{\"appointmentptr\" : \"" +
                visitResend.appointmentid +
                "\", \"lat\" : \"" + visitResend.coordinateLatitudeMarkArrive + "\", " +
                "\"lon\" : \"" + visitResend.coordinateLongitudeMarkArrive + "\"," +
                " \"event\" : \"arrived\", " +
                "\"accuracy\" : \"5.0\"}");

        String url = urlBuilder.toString();

        if (sVisitsAndTracking.USER_AGENT == null) {
            sVisitsAndTracking.USER_AGENT = "LeashTime Android / null user agent";
        }

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", sVisitsAndTracking.USER_AGENT)
                .build();


        client.newCall(request).enqueue(new Callback() {

            Response theResponse;

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) {
                visitResend.currentArriveStatus = "SUCCESS";
                sVisitsAndTracking.writeVisitDataToFile(visitResend);
                theResponse = response;
                theResponse.close();

            }
        });

    }
    public void reSendCompleteRequest(final VisitDetail visitDetail) {

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.leashtime.com/native-visit-action.php")
                .newBuilder();
        urlBuilder.addQueryParameter("loginid", sVisitsAndTracking.USERNAME);
        urlBuilder.addQueryParameter("password", sVisitsAndTracking.PASSWORD);
        urlBuilder.addQueryParameter("datetime", visitDetail.completed);
        urlBuilder.addQueryParameter("coords",
                "{\"appointmentptr\" : \"" + visitDetail.appointmentid +
                        "\", \"lat\" : \"" + visitDetail.coordinateLatitudeMarkComplete +
                        "\", \"lon\" : \"" + visitDetail.coordinateLongitudeMarkComplete +
                        "\", \"event\" : \"completed\", " +
                        "\"accuracy\" : \"5.0\"}");

        String url = urlBuilder.toString();

        if (sVisitsAndTracking.USER_AGENT == null) {
            sVisitsAndTracking.USER_AGENT = "LeashTime Android / null user agent";
        }

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", sVisitsAndTracking.USER_AGENT)
                .build();

        client.newCall(request).enqueue(new Callback() {
            Response theResponse;

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) {
                visitDetail.currentCompleteStatus = "SUCCESS";
                sVisitsAndTracking.writeVisitDataToFile(visitDetail);
                theResponse = response;
                theResponse.close();
            }
        });
    }
    public void addToolbar() {

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        dateText = toolbar.findViewById(R.id.dayWeek);
        monthText = toolbar.findViewById(R.id.onDate);
        networkStatusView = toolbar.findViewById(R.id.networkStatus);
        setToolbarDate();

    }
    public void   setToolbarDate(){
        Calendar calendar = new GregorianCalendar();
        String dayWeek = "";
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if(dayOfWeek == 1) {
            dayWeek = "SUN";
        } else if (dayOfWeek == 2){
            dayWeek = "MON";
        } else if (dayOfWeek == 3){
            dayWeek = "TUE";
        } else if (dayOfWeek == 4){
            dayWeek = "WED";
        } else if (dayOfWeek == 5){
            dayWeek = "THU";
        } else if (dayOfWeek == 6){
            dayWeek = "FRI";
        } else if (dayOfWeek == 7){
            dayWeek = "SAT";
        }

        final String dayWeekInt = dayWeek;

        int dateMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);

        String dateNum = Integer.toString(dateMonth);

        String monthString = "";
        if(month == 0) {
            monthString = "JAN";
        } else if (month == 1) {
            monthString = "FEB";
        } else if (month == 2) {
            monthString = "MAR";
        } else if (month == 3) {
            monthString = "APR";
        } else if (month == 4) {
            monthString = "MAY";
        } else if (month == 5) {
            monthString = "JUN";
        } else if (month == 6) {
            monthString = "JUL";
        } else if (month == 7) {
            monthString = "AUG";
        } else if (month == 8) {
            monthString = "SEP";
        } else if (month == 9) {
            monthString = "OCT";
        } else if (month == 10) {
            monthString = "NOV";
        } else if (month == 11) {
            monthString = "DEC";
        }

        final String monthDateString = monthString + ' ' + dateNum;

        if (monthText != null && dateText != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    monthText.setText(monthDateString);
                    dateText.setText(dayWeekInt);
                }
            });
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        MenuItem showKey = menu.findItem(R.id.showKey);
        MenuItem showFlags = menu.findItem(R.id.showFlags);
        MenuItem showClientName = menu.findItem(R.id.showClientName);
        MenuItem showVisitTimer = menu.findItem(R.id.showVisitTimer);
        MenuItem showPetPic = menu.findItem(R.id.showPetPic);
        MenuItem mapView = menu.findItem(R.id.mapView);
        MenuItem webView = menu.findItem(R.id.webView);
        webView.setVisible(FALSE);

        if(sVisitsAndTracking.showKey) {
            showKey.setChecked(TRUE);
        }
        if(sVisitsAndTracking.showFlags) {
            showFlags.setChecked(TRUE);
        }
        if(sVisitsAndTracking.showClientName) {
            showClientName.setChecked(TRUE);
        }
        if(sVisitsAndTracking.showVisitTimer) {
            showVisitTimer.setChecked(TRUE);
        }
        if(sVisitsAndTracking.showPetPic) {
            showPetPic.setChecked(TRUE);
        }
        if(sVisitsAndTracking.showVisitTimer) {
            showVisitTimer.setChecked(TRUE);
        }

        return true;
    }
    @Override
    public boolean          onOptionsItemSelected(MenuItem item) {

        SharedPreferences.Editor editor = sVisitsAndTracking.mPreferences.edit();
        Calendar cal = new GregorianCalendar();
        switch (item.getItemId()) {
            case R.id.prev:
                cal.setTime(sVisitsAndTracking.showingWhichDate);
                cal.add(Calendar.DAY_OF_MONTH, -1);
                Date yesterday = cal.getTime();
                sVisitsAndTracking.getNextPrevDay(yesterday, nextPrevDateFormat.format(yesterday),"before");
                dateText.setText(getDayWeek(cal.get(Calendar.DAY_OF_WEEK)));
                monthText.setText(getMonthDay(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)));
                return true;

            case R.id.next:
                cal.setTime(sVisitsAndTracking.showingWhichDate);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                Date tomorrow = cal.getTime();
                sVisitsAndTracking.getNextPrevDay(tomorrow, nextPrevDateFormat.format(tomorrow), "after");

                dateText.setText(getDayWeek(cal.get(Calendar.DAY_OF_WEEK)));
                monthText.setText(getMonthDay(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)));
                return true;

            case R.id.showPetPic:
                sVisitsAndTracking.mPreferences = this.getApplicationContext().getSharedPreferences(sVisitsAndTracking.PREF_NAME, Activity.MODE_PRIVATE);
                if(sVisitsAndTracking.showPetPic) {
                    sVisitsAndTracking.showPetPic = false;
                    editor.putString("showPetPic","NO");
                    item.setChecked(FALSE);
                } else {
                    sVisitsAndTracking.showPetPic = true;
                    editor.putString("showPetPic","YES");
                    item.setChecked(TRUE);
                }
                visitAdapter.notifyDataSetChanged();
                editor.apply();
                return true;
            case R.id.showFlags:
                sVisitsAndTracking.mPreferences = this.getApplicationContext().getSharedPreferences(sVisitsAndTracking.PREF_NAME, Activity.MODE_PRIVATE);
                if(sVisitsAndTracking.showFlags) {
                    sVisitsAndTracking.showFlags = false;
                    editor.putString("showFlags","NO");
                    item.setChecked(FALSE);
                } else {
                    sVisitsAndTracking.showFlags = true;
                    editor.putString("showFlags","YES");
                    item.setChecked(TRUE);
                }
                visitAdapter.notifyDataSetChanged();
                editor.apply();
                return true;
            case R.id.showClientName:
                sVisitsAndTracking.mPreferences = this.getApplicationContext().getSharedPreferences(sVisitsAndTracking.PREF_NAME, Activity.MODE_PRIVATE);
                if(sVisitsAndTracking.showClientName) {
                    sVisitsAndTracking.showClientName = false;
                    editor.putString("showClientName","NO");
                    item.setChecked(FALSE);
                } else {
                    sVisitsAndTracking.showClientName = true;
                    editor.putString("showClientName","YES");
                    item.setChecked(TRUE);
                }
                visitAdapter.notifyDataSetChanged();
                editor.apply();
                return true;
            case R.id.showKey:
                sVisitsAndTracking.mPreferences = this.getApplicationContext().getSharedPreferences(sVisitsAndTracking.PREF_NAME, Activity.MODE_PRIVATE);
                if(sVisitsAndTracking.showKey) {
                    sVisitsAndTracking.showKey = false;
                    editor.putString("showKey","NO");
                    item.setChecked(FALSE);
                } else {
                    sVisitsAndTracking.showKey = true;
                    editor.putString("showKey","YES");
                    item.setChecked(TRUE);
                }
                visitAdapter.notifyDataSetChanged();
                editor.apply();
                return true;
            case R.id.showVisitTimer:
                sVisitsAndTracking.mPreferences = this.getApplicationContext().getSharedPreferences(sVisitsAndTracking.PREF_NAME, Activity.MODE_PRIVATE);
                if(sVisitsAndTracking.showVisitTimer) {
                    sVisitsAndTracking.showVisitTimer = false;
                    editor.putString("showVisitTimer","NO");
                    item.setChecked(FALSE);
                } else {
                    sVisitsAndTracking.showVisitTimer = true;
                    editor.putString("showVisitTimer","YES");
                    item.setChecked(TRUE);
                }
                visitAdapter.notifyDataSetChanged();
                editor.apply();
                return true;

            case R.id.mapView:
                Intent mapIntent = new Intent(this.getApplicationContext(),MapActivity.class);
                startActivity(mapIntent);
                return true;

            case R.id.webView:
                Bundle basket2 = new Bundle();
                basket2.putString("documentURL","<URL-To-DOC>");
                Intent visitDetailIntent = new Intent(getApplicationContext(), WebViewActivity.class);
                visitDetailIntent.putExtras(basket2);
                startActivity(visitDetailIntent);
                return true;
            case R.id.logout:
                sVisitsAndTracking.PASSWORD = "";
                sVisitsAndTracking.USERNAME = "";
                TrackerServiceSitter tracker = new TrackerServiceSitter(MainApplication.getAppContext());
                Intent intent = new Intent(MainApplication.getAppContext(), TrackerServiceSitter.class);
                stopService(intent);
                this.initialLogin = TRUE;

                Iterator<VisitDetail> visitDetailListIterator= sVisitsAndTracking.visitData.listIterator();
                while(visitDetailListIterator.hasNext()) {
                    VisitDetail visitDetail = visitDetailListIterator.next();
                    visitDetailListIterator.remove();
                }

                Iterator<VisitDetail> daysBeforeIterator= sVisitsAndTracking.daysBeforeVisit.listIterator();
                while(daysBeforeIterator.hasNext()) {
                    VisitDetail visitDetail = daysBeforeIterator.next();
                    daysBeforeIterator.remove();
                }

                Iterator<VisitDetail> daysAfterIterator = sVisitsAndTracking.daysAfterVisit.listIterator();
                while(daysAfterIterator.hasNext()) {
                    VisitDetail visitDetail = daysAfterIterator.next();
                    daysAfterIterator.remove();
                }

                Iterator<ClientDetail> clientDetailIterator = sVisitsAndTracking.clientData.listIterator();
                while(clientDetailIterator.hasNext()) {
                    ClientDetail clientDetail = clientDetailIterator.next();
                    clientDetailIterator.remove();
                }

                Iterator<FlagItem> flagItemIterator = sVisitsAndTracking.mFlagData.listIterator();
                while(flagItemIterator.hasNext()) {
                    FlagItem flagItem = flagItemIterator.next();
                    flagItemIterator.remove();
                }

                recyclerView.setAdapter(null);
                visitAdapter = null;
                initialLogin = true;
                initialLogin2 = true;
                pollingUpdate = false;
                sVisitsAndTracking.reInit(this.getApplicationContext());
                launchLogin("connection");
                return true;
        }

        return(super.onOptionsItemSelected(item));

    }
    private static String  daysBefore() {
        Date today = new Date();
        Calendar cal = new GregorianCalendar();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date today30 = cal.getTime();
        return dateFormat.format(today30);
    }
    private static String daysLater() {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 21);
        Date threeWeeksLater = calendar.getTime();
        return dateFormat.format(threeWeeksLater);

    }
    private String      checkLoginCode(String lastLoginResponseCode) {
        switch (lastLoginResponseCode) {
            case "S":
                sVisitsAndTracking.lastLoginResponseCode = "SITTER MOBILE APP NOT ENABLED FOR BUSINESS";
                return "SITTER MOBILE APP NOT ENABLED FOR BUSINESS";
            case "P":
                sVisitsAndTracking.lastLoginResponseCode = "UNKNOWN ACCOUNT INFO";
                return  "UNKNOWN ACCOUNT INFO";
            case "U":
                sVisitsAndTracking.lastLoginResponseCode = "UNKNOWN ACCOUNT INFO";
                return "UNKNOWN ACCOUNT INFO";
            case "I":
                sVisitsAndTracking.lastLoginResponseCode = "UNKNOWN ACCOUNT INFO";
                return "UNKNOWN ACCOUNT INFO";
            case "F":
                sVisitsAndTracking.lastLoginResponseCode = "NO BUSINESS FOUND";
                return "NO BUSINESS FOUND";
            case "B":
                sVisitsAndTracking.lastLoginResponseCode = "BUSINESS INACTIVE";
                return "BUSINESS INACTIVE";
            case "M":
                sVisitsAndTracking.lastLoginResponseCode = "MISSING ORGANIZATION";
                return "MISSING ORGANIZATION";
            case "O":
                sVisitsAndTracking.lastLoginResponseCode = "ORGANIZATION INACTIVE";
                return "ORGANIZATION INACTIVE";
            case "R":
                sVisitsAndTracking.lastLoginResponseCode = "RIGHTS MISSING. CONTACT support@leashtime.com";
                return "RIGHTS MISSING. CONTACT support@leashtime.com";
            case "C":
                sVisitsAndTracking.lastLoginResponseCode = "NO COOKIE";
                return "NO COOKIE";
            case "L":
                sVisitsAndTracking.lastLoginResponseCode = "ACCOUNT LOCKED";
                return "ACCOUNT LOCKED";
            case "T":
                sVisitsAndTracking.lastLoginResponseCode = "TEMP PASSWORD";
                return "TEMP PASSWORD";
            case "X":
                sVisitsAndTracking.lastLoginResponseCode = "NOT A SITTER ACCOUNT";
                return "NOT A SITTER ACCOUNT";
            default:
                sVisitsAndTracking.lastLoginResponseCode = "OK";
                return "OK";
        }
    }
    public void          cleanVisitData()  {
        TrackerServiceSitter tracker = new TrackerServiceSitter(MainApplication.getAppContext());
        Intent intent = new Intent(MainApplication.getAppContext(), TrackerServiceSitter.class);
        stopService(intent);
        this.initialLogin = TRUE;

        Iterator<VisitDetail> visitDetailListIterator= sVisitsAndTracking.visitData.listIterator();
        while(visitDetailListIterator.hasNext()) {
            VisitDetail visitDetail = visitDetailListIterator.next();
            visitDetailListIterator.remove();
        }
        Iterator<VisitDetail> daysBeforeIterator= sVisitsAndTracking.daysBeforeVisit.listIterator();
        while(daysBeforeIterator.hasNext()) {
            VisitDetail visitDetail = daysBeforeIterator.next();
            daysBeforeIterator.remove();
        }
        Iterator<VisitDetail> daysAfterIterator = sVisitsAndTracking.daysAfterVisit.listIterator();
        while(daysAfterIterator.hasNext()) {
            VisitDetail visitDetail = daysAfterIterator.next();
            daysAfterIterator.remove();
        }
        Iterator<ClientDetail> clientDetailIterator = sVisitsAndTracking.clientData.listIterator();
        while(clientDetailIterator.hasNext()) {
            ClientDetail clientDetail = clientDetailIterator.next();
            clientDetailIterator.remove();
        }
        Iterator<VisitDetail> tempVisitIterator = sVisitsAndTracking.tempVisitData.listIterator();
        while(tempVisitIterator.hasNext()) {
            VisitDetail visitDetail = tempVisitIterator.next();
            tempVisitIterator.remove();
        }
    }
    private boolean  canAccessLocation () {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{ACCESS_FINE_LOCATION},
                        0);
                return true;
            }
        }
        return true;
    }
    public boolean   checkNetworkConnection() {
        if (isNetworkConnected(this)) {
            ConnectivityManager cm = (ConnectivityManager) this.getSystemService (Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if (ni.isConnectedOrConnecting()) {
                int type = ni.getType();
                if (null != ni) {
                    if(type == ConnectivityManager.TYPE_WIFI) {
                        if (networkStatusView != null) {
                            networkStatusView.setText("WIFI");
                            networkStatusView.setVisibility(View.INVISIBLE);
                        }
                    } else if (type== ConnectivityManager.TYPE_MOBILE) {
                        if (networkStatusView != null) {
                            networkStatusView.setText("MOBILE");
                            networkStatusView.setVisibility(View.INVISIBLE);
                        }
                    } else if (type== ConnectivityManager.TYPE_WIMAX) {
                        if (networkStatusView != null) {
                            networkStatusView.setText("WIMAX");
                            networkStatusView.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        if (networkStatusView != null) {
                            networkStatusView.setVisibility(View.VISIBLE);
                            networkStatusView.setText("NO CONNECT");
                            networkStatusView.setTextColor(Color.RED);
                        }
                    }
                    if (isInternetAvailable()) {
                        if (networkStatusView != null) {
                            networkStatusView.setVisibility(View.INVISIBLE);
                        }
                        return true;
                    } else {
                        if (networkStatusView != null) {
                            networkStatusView.setText("NO route to internet");
                            networkStatusView.setTextColor(Color.RED);
                            networkStatusView.setVisibility(View.VISIBLE);
                        }
                        return false;
                    }
                }
            }
        } else {
            if (networkStatusView != null) {
                networkStatusView.setText("NO CONNECT");
                networkStatusView.setTextColor(Color.RED);
                networkStatusView.setVisibility(View.VISIBLE);
            }
            return false;
        }
        return false;
    }
    public static boolean isNetworkConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService (Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }
    public static boolean isInternetAvailable() {
        String host = "www.google.com";
        int port = 80;
        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(host, port), 2000);
            socket.close();
            return true;
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException es) {}
            return false;
        }
    }
    private String                      getMonthDay(int dateMonth, int month) {
        String dateNum = Integer.toString(dateMonth);
        String monthString = "";
        if(month == 0) {
            monthString = "JAN";
        } else if (month == 1) {
            monthString = "FEB";
        } else if (month == 2) {
            monthString = "MAR";
        } else if (month == 3) {
            monthString = "APR";
        } else if (month == 4) {
            monthString = "MAY";
        } else if (month == 5) {
            monthString = "JUN";
        } else if (month == 6) {
            monthString = "JUL";
        } else if (month == 7) {
            monthString = "AUG";
        } else if (month == 8) {
            monthString = "SEP";
        } else if (month == 9) {
            monthString = "OCT";
        } else if (month == 10) {
            monthString = "NOV";
        } else if (month == 11) {
            monthString = "DEC";
        }

        monthString += ' ' + dateNum;

        return monthString;
    }
    private String                      getDayWeek(int dayOfWeek){
        String dayWeek = "";
        if(dayOfWeek == 1) {
            dayWeek = "SUN";
        } else if (dayOfWeek == 2){
            dayWeek = "MON";
        } else if (dayOfWeek == 3){
            dayWeek = "TUE";
        } else if (dayOfWeek == 4){
            dayWeek = "WED";
        } else if (dayOfWeek == 5){
            dayWeek = "THU";
        } else if (dayOfWeek == 6){
            dayWeek = "FRI";
        } else if (dayOfWeek == 7){
            dayWeek = "SAT";
        }
        return dayWeek;
    }

}
