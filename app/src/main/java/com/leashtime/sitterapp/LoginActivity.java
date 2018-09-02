package com.leashtime.sitterapp;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.leashtime.sitterapp.events.LoginEvent;
import com.leashtime.sitterapp.network.NetworkStatus;

import org.greenrobot.eventbus.EventBus;

public class LoginActivity extends AppCompatActivity {

    private String versionName;
    private int versionNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_login);
        Bundle extras = this.getIntent().getExtras();
        VisitsAndTracking sVisitsAndTracking = VisitsAndTracking.getInstance();
        int osVersion = Build.VERSION.SDK_INT;
        String manufacturer = Build.MANUFACTURER;
        String brand        = Build.BRAND;
        String product      = Build.PRODUCT;
        String model        = Build.MODEL;

        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
            versionNum = getPackageManager().getPackageInfo(getPackageName(),0).versionCode;
            sVisitsAndTracking.USER_AGENT = "LEASHTIME ANDROID ver:" + versionName + ", build:" + versionNum + " / OS:" + osVersion + ", Model: " + model;
        } catch(PackageManager.NameNotFoundException name) {
            name.printStackTrace();
        }

        final EditText usernameText = findViewById(R.id.loginName);
        final EditText passwordText = findViewById(R.id.password);
        final TextView errorText = findViewById(R.id.errorLogin);
        final TextView versionInfo = findViewById(R.id.versionInfo);
        final TextView networkConnectStatus = findViewById(R.id.networkConn);
        final Button loginButton = findViewById(R.id.login_button);
        ImageView networkIcon = findViewById(R.id.connectIcon);
        NetworkStatus networkConn = new NetworkStatus(this.getApplicationContext());

        if(!sVisitsAndTracking.lastLoginResponseCode.equals("OK")) {
            errorText.setText(sVisitsAndTracking.lastLoginResponseCode);
        } else {
            errorText.setText(sVisitsAndTracking.lastLoginResponseCode);
        }

        if(!networkConn.hasConnect && !networkConn.hasRouteToNetwork) {
            networkIcon.setAlpha(0.25f);
            networkConnectStatus.setText("NO NETWORK");
            networkConnectStatus.setTextColor(Color.RED);
            errorText.setText("NO CONNECT");
        } else if (networkConn.hasConnect && !networkConn.hasRouteToNetwork) {
            networkConnectStatus.setText("NETWORK PROBLEM");
            networkIcon.setAlpha(0.25f);
            networkConnectStatus.setTextColor(Color.RED);
            errorText.setText("NO ROUTE");
        } else if (networkConn.hasConnect && networkConn.hasRouteToNetwork) {
            networkIcon.setAlpha(1.0f);
            networkConnectStatus.setText("GOOD NETWORK");
            networkConnectStatus.setTextColor(Color.BLACK);
        }

        if(!sVisitsAndTracking.prefUserName().isEmpty()) {
            usernameText.setText(sVisitsAndTracking.prefUserName());
        }
        usernameText.setHint("Username");
        passwordText.setHint("Password");
        versionInfo.setText("VERSION: " + versionName + ", build: " + versionNum);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                String username = usernameText.getText().toString();
                String password = passwordText.getText().toString();
                //password = "QVX992DISABLED";
                String loginStatus = loginValidate(username,password);
                if(loginStatus.equals("OK")) {
                    System.out.println("Login status is OK");
                    sVisitsAndTracking.prefSetUserName(username);
                    LoginEvent logEvent = new LoginEvent(username,password);
                    EventBus.getDefault().post(logEvent);
                    finish();
                }
            }
        });
    }

    private String loginValidate(String uName, CharSequence pWord) {

        if(uName.isEmpty()) {
            Toast.makeText(this.getApplicationContext(), "NEED USER NAME", Toast.LENGTH_LONG).show();
            return "NOT";
        } else if (pWord.equals("")) {
            Toast.makeText(this.getApplicationContext(), "PLEASE SUPPLY PASSWORD", Toast.LENGTH_LONG).show();
            return "NOT";
        } else if (4 > pWord.length()) {
            Toast.makeText(this.getApplicationContext(), "PASSWORD TOO SHORT", Toast.LENGTH_LONG).show();
            return "NOT";
        }
        return "OK";
    }

    @Override
    public void onStart() {
        super.onStart();

    }
    @Override
    public void onPause() {
        System.out.println("Login Activity pause-");
        super.onPause();
    }

    @Override
    public void onResume() {
        System.out.println("Login Activity resume");
        super.onResume();
    }
    @Override
    public void onStop() {
        System.out.println("Login Activity stop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}



