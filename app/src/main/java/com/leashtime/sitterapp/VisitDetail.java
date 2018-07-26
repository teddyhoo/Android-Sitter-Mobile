package com.leashtime.sitterapp;

import android.location.Location;
import android.os.Environment;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Request;

public class VisitDetail implements Serializable {

    public Iterable<Location> gpsCoordsForVisit = new ArrayList<>();
    public ArrayList<HashMap<String, String>> gpsDicForVisit = new ArrayList<>();
    public ArrayList<String> petListVisit = new ArrayList<>();
    public ArrayList<String> retransmitDates = new ArrayList<>();

    public String currentArriveStatus = "NONE";
    public String currentCompleteStatus = "NONE";
    public String completedLocationStatus = "NONE";
    public String imageUploadStatus = "NONE";
    public String mapSnapUploadStatus = "NONE";
    public String visitReportUploadStatus = "NONE";
    public String mapSnapImageCreate = "NONE";
    public Request visitReportRequest;
    public String providerptr;
    public String appointmentid;
    public String longitude;
    public String service;
    public String pets;
    public String canceled;
    public String starttime;
    public String endtime;
    public String note;
    public String clientptr;
    public String petNames;
    public String status;
    public String timeofday;
    public String latitude;
    @Nullable
    public String arrived;
    public String completed;
    public String shortNaturalDate;
    public String endDateTime;

    public String visitNoteBySitter;
    public String dateTimeVisitReportSubmit;
    public String coordinateLatitudeMarkArrive;
    public String coordinateLongitudeMarkArrive;
    public String coordinateLatitudeMarkComplete;
    public String coordinateLongitudeMarkComplete;
    public String petPicFileName;
    public String mapSnapShotImage;

    public String clientname;
    public String date;

    public String hasKey;
    public String keyID;
    public String keyDescription;
    public String noKeyRequired;
    public String useKeyDescriptionInstead;
    public String keyDescriptionText;
    public String garageGateCode;
    public String alarmInfo;
    public String rawStartTime;
    public String sequenceID;

    boolean highpriority;
    boolean pendingChange;
    boolean hasArrived;
    boolean isCanceled;
    boolean isComplete;
    boolean inProcess;
    boolean isLate;
    boolean cancelationPending;

    boolean didPoo;
    boolean didPee;
    boolean didPlay;
    boolean wasHappy;
    boolean wasSad;
    boolean wasAngry;
    boolean wasShy;
    boolean wasHungry;
    boolean wasSick;
    boolean wasCat;
    boolean didScoopLitter;


    public void initializeMoods () {

        didPoo = false;
        didPee = false;
        didPlay = false;
        wasHappy = false;
        wasSad = false;
        wasHungry = false;
        wasAngry = false;
        wasShy = false;
        wasSick = false;
        wasCat = false;
        didScoopLitter = false;

    }

    public void addCoordinateForVisit(Location coordinate) {

        gpsDicForVisit.add(convertLocationToDictionary(coordinate));
    }
    public void printCoordinates() {

        System.out.println(gpsCoordsForVisit);
    }
    private static HashMap<String,String> convertLocationToDictionary(Location location) {

        HashMap<String,String> locationDic = new HashMap<>();

        String lat = String.valueOf(location.getLatitude());
        String lon = String.valueOf(location.getLongitude());
        String coordTimeStamp = String.valueOf(location.getTime());
        String accuracy = String.valueOf(location.getAccuracy());
        String heading = String.valueOf(location.getBearing());
        String speed = String.valueOf(location.getSpeed());

        locationDic.put("latitude", lat);
        locationDic.put("longitude", lon);
        locationDic.put("timestamp", coordTimeStamp);
        if(null != accuracy) {
            locationDic.put("accuracy",accuracy);
        }
        if(null != heading) {
            locationDic.put("heading",heading);
        }
        if(null != speed) {
            locationDic.put("speed",speed);
        }
        return locationDic;
    }
    public void setSitterVisitNote(String note) {
        this.visitNoteBySitter = note;
    }
    private File getPictureFile() {
        String fileName = "PHOTO_" + this.appointmentid + ".jpg";
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
    }
    public void prettyPrint() {
        System.out.println("appointment ID: " + appointmentid);
        System.out.println("client name: " + clientname);
        System.out.println("service name: " + service);
        System.out.println("has key: " + hasKey);
        System.out.println("key id: " + keyID);
        System.out.println("key description: " + keyDescription);
        System.out.println("key description text: " + keyDescriptionText);
        System.out.println("use key description instead: " + useKeyDescriptionInstead);


    }

}


