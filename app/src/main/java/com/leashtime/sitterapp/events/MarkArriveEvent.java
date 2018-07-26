package com.leashtime.sitterapp.events;

import com.leashtime.sitterapp.VisitDetail;

public class MarkArriveEvent {
    public VisitDetail visitDetail;

    public MarkArriveEvent(VisitDetail vid) {
        visitDetail = vid;
    }
}
