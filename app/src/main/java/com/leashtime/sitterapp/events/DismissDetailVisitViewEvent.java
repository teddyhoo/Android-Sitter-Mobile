package com.leashtime.sitterapp.events;

public class DismissDetailVisitViewEvent {
    public final String message;

    public DismissDetailVisitViewEvent(String appointmentid) {
        message = appointmentid;

    }
}
