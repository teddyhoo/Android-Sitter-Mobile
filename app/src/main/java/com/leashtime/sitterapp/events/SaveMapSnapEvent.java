package com.leashtime.sitterapp.events;

public class SaveMapSnapEvent {

    public final String appointmentID;
    public final String sourceMapImage;

    public SaveMapSnapEvent(String appointmentID, String sourceMapImage) {
        this.appointmentID = appointmentID;
        this.sourceMapImage = sourceMapImage;

    }
}
