package com.leashtime.sitterapp.events;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class LogBackgroundEvent {

    public String eventID;
    public String imageFileName;
    public String requestString;
    public String responseString;
    public String initialSendDateTime;
    public ArrayList<String> resendAttemptDateTime;
    private SimpleDateFormat formatter;


    public LogBackgroundEvent() {

        //Date rightNow = new Date();

    }

    public String getFlatLogObject() {
        String flatLog = "";
        return flatLog;

    }


}