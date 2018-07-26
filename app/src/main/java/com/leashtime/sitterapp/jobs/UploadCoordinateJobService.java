package com.leashtime.sitterapp.jobs;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.location.Location;

import com.leashtime.sitterapp.VisitsAndTracking;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UploadCoordinateJobService  extends JobService {

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public boolean onStartJob(JobParameters params) {

        Date dateTimeStamp = new Date();
        SimpleDateFormat rightNowFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        VisitsAndTracking sVisitsAndTracking = VisitsAndTracking.getInstance();
        String postCoordURL = "https://leashtime.com/native-sitter-location.php";
        String userName = sVisitsAndTracking.mPreferences.getString("username","");
        String password = sVisitsAndTracking.mPreferences.getString("password","");
        String getStringOpen = "[\n";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        if(!sVisitsAndTracking.isMultiVisitArrive && !sVisitsAndTracking.onWhichVisitID.equals("0000")) {
            if (sVisitsAndTracking.sessionLocationArray != null) {
                int locArrayCount = sVisitsAndTracking.sessionLocationArray.size();
                for (int i = 0; i < locArrayCount; i++) {
                    getStringOpen += buildCoordinateStringSend(sVisitsAndTracking.sessionLocationArray.get(i),sVisitsAndTracking.onWhichVisitID);
                    String error = "0";
                    if (i < locArrayCount - 1) {
                        getStringOpen += "\"error\" : " + '"' + error + "\"\n},\n";
                    } else {
                        getStringOpen += "\"error\" : " + '"' + error + "\"\n}\n]";
                    }
                }
            }
        } else if (sVisitsAndTracking.isMultiVisitArrive && sVisitsAndTracking.onWhichVisits.size() > 0){
            if (sVisitsAndTracking.sessionLocationArray != null) {
                int multiVisitCount = sVisitsAndTracking.onWhichVisits.size();
                for(String multiVisitID : sVisitsAndTracking.onWhichVisits) {
                    int locArrayCount = sVisitsAndTracking.sessionLocationArray.size();
                    for (int i = 0; i < locArrayCount; i++) {
                        getStringOpen += buildCoordinateStringSend(sVisitsAndTracking.sessionLocationArray.get(i),multiVisitID);
                        String error = "0";
                        if (i < locArrayCount - 1) {
                            getStringOpen += "\"error\" : " + '"' + error + "\"\n},\n";
                        } else {
                            if (multiVisitCount == 1) {
                                getStringOpen += "\"error\" : " + '"' + error + "\"\n}\n]\n";
                            } else {
                                getStringOpen += "\"error\" : " + '"' + error + "\"\n},\n";
                            }
                        }
                    }
                    multiVisitCount--;
                }
            }

            if(userName != null && password != null) {
                RequestBody formBody = new FormBody.Builder()
                        .add("loginid", userName)
                        .add("password", password)
                        .add("coords", getStringOpen)
                        .build();

                if(sVisitsAndTracking.USER_AGENT == null) {
                    sVisitsAndTracking.USER_AGENT = "LeashTime Android / null user agent";
                }

                Request request = new Request.Builder()
                        .url(postCoordURL)
                        .header("User-Agent",sVisitsAndTracking.USER_AGENT)
                        .post(formBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        String responseString = response.toString();
                        ResponseBody responseBody = response.body();
                        sVisitsAndTracking.sessionLocationArray.clear();
                        responseBody.close();
                    }
                });
            }
        }
        return true;
    }

    private String buildCoordinateStringSend(Location loc, String visitID) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String latString = String.valueOf(loc.getLatitude());
        String lonString = String.valueOf(loc.getLongitude());
        String accuracy = String.valueOf(loc.getAccuracy());
        long coordTime = loc.getTime();
        Date date = new Date(coordTime);
        String dateTimeString = dateFormat.format(date);

        String event = "mv";
        String heading = "0";
        String error = "0";

        String locationRequest = "";
        locationRequest += "{\"appointmentptr\" : " + '"' +visitID + "\",\n";
        locationRequest += "\"date\" : " + '"' + dateTimeString + "\",\n";
        locationRequest += "\"lat\" : " + '"' + latString + "\",\n";
        locationRequest += "\"lon\" : " + '"' + lonString + "\",\n";
        locationRequest += "\"accuracy\" : " + '"' + accuracy + "\",\n";
        locationRequest += "\"event\" : " + '"' + event + "\",\n";
        locationRequest += "\"heading\" : " + '"' + heading + "\",\n";
        return locationRequest;

    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}