package com.leashtime.sitterapp.events;

public class SendVisitReportEvent {
    public final String message;

    public  SendVisitReportEvent(String appointmentID) {
        this.message = appointmentID;
    }
}
