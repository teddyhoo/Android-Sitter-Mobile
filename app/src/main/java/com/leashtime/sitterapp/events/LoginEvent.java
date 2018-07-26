package com.leashtime.sitterapp.events;

public class LoginEvent {
    public final String username;
    public final String password;

    public LoginEvent(String uname, String pword) {
        this.username = uname;
        this.password = pword;
    }
}