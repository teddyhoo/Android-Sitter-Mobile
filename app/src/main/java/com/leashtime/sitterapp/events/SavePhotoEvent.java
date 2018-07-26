package com.leashtime.sitterapp.events;

public class SavePhotoEvent {

    public final String appointmentID;
    public final String sourcePhoto;


    public SavePhotoEvent(String apptID, String source) {

        this.appointmentID = apptID;
        this.sourcePhoto = source;
    }
}

