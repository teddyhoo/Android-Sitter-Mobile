package com.leashtime.sitterapp.jobs;

import android.app.job.JobParameters;
import android.app.job.JobService;

import com.leashtime.sitterapp.VisitsAndTracking;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BadSendJob extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        OkHttpClient client = new OkHttpClient();
        VisitsAndTracking sVisitsAndTracking  = VisitsAndTracking.getInstance();

        if (!sVisitsAndTracking.resendCoordUploadRequest.isEmpty()) {

            for (Request request : sVisitsAndTracking.resendCoordUploadRequest) {
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }
                    @Override
                    public void onResponse(Call call, Response response) {
                    }
                });

            }
        }

        return true;
    }

    private boolean deleteQueueItem(Request request, String type) {


        return true;

    }

    @Override
    public boolean onStopJob(JobParameters params) {

        return true;

    }
}

