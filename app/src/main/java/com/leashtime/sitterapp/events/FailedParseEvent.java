package com.leashtime.sitterapp.events;

public class FailedParseEvent {

    public String userName;
    public String passWord;


    public FailedParseEvent(String uName, String pWord) {

        this.userName = uName;
        this.passWord = pWord;

    }
}