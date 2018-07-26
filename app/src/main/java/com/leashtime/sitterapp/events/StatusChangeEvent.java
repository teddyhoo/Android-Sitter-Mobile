package com.leashtime.sitterapp.events;

public class StatusChangeEvent {

    public final String typeStatus;
    public final String changeTo;

    public StatusChangeEvent(String typeStatus, String changeTo) {

        this.typeStatus = typeStatus;
        this.changeTo = changeTo;

    }
}
