package com.leashtime.sitterapp.events;

public class DoVisitReportEvent {
    public String visitID;

    public DoVisitReportEvent(String visit) {
        visitID = visit;
    }
}
