package com.leashtime.sitterapp;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Map;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveCanceledListener,
        GoogleMap.OnCameraIdleListener {

    private GoogleMap iMap;
    private CompoundButton mAnimateToggle;
    private CompoundButton mCustomDurationToggle;
    private SeekBar mCustomDurationBar;
    private PolylineOptions currPolylineOptions;
    //private final boolean isCanceled;
    public VisitsAndTracking sharedVisits;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout_view);
        sharedVisits = VisitsAndTracking.getInstance();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }
    @Override
    public void onMapReady(GoogleMap googleMap) {

        iMap = googleMap;
        iMap.setOnCameraIdleListener(this);
        iMap.setOnCameraMoveStartedListener(this);
        iMap.setOnCameraMoveListener(this);
        iMap.setOnCameraMoveCanceledListener(this);

        sharedVisits = VisitsAndTracking.getInstance();

        for (VisitDetail visitDetail : sharedVisits.visitData) {

            if (!visitDetail.latitude.equals("NONE") && !visitDetail.longitude.equals("NONE")) {
                String latitude = visitDetail.latitude;
                double lat = Double.parseDouble(latitude);
                String longitude = visitDetail.longitude;
                double lon = Double.parseDouble(longitude);
                LatLng visitLatLon = new LatLng(lat, lon);

                iMap.addMarker(new MarkerOptions().position(visitLatLon).title(visitDetail.clientname));
            }
        }

        VisitDetail visitDetailZoom = sharedVisits.visitData.get(0);
        if (visitDetailZoom.latitude != "NONE" && visitDetailZoom.longitude != "NONE") {
            double visitLat = Double.parseDouble(visitDetailZoom.latitude);
            double visitLon = Double.parseDouble(visitDetailZoom.longitude);

            if (visitLat != 0 && visitLon != 0) {
                LatLng visitLatLon = new LatLng(visitLat, visitLon);
                iMap.moveCamera(CameraUpdateFactory.newLatLngZoom(visitLatLon, 12.0f));
            }
        }

        drawPolylinesForVisit(sharedVisits.visitData.get(2), Color.RED);

    }

    private void drawPolylinesForVisit(VisitDetail visit, int color) {

        System.out.println("Drawing polyline for: " + visit.clientname);
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(4);

        ArrayList<LatLng> addPolyArray = new ArrayList<>();

        for (Map<String, String> location : visit.gpsDicForVisit) {
            String sLat = location.get("latitude");
            String sLon = location.get("longitude");

            float latitude = Float.parseFloat(sLat);
            float longitude = Float.parseFloat(sLon);
            LatLng latLon = new LatLng(latitude, longitude);
            addPolyArray.add(latLon);
        }
        polylineOptions.addAll(addPolyArray);
        iMap.addPolyline(polylineOptions);
    }

    private void changeCamera(CameraUpdate update) {
        changeCamera(update, null);
    }

    private void changeCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {

        iMap.animateCamera(update, callback);

    }

    public void onZoomIn(View view) {
        changeCamera(CameraUpdateFactory.zoomIn());
    }

    private void addCameraTargetToPath() {
        LatLng target = iMap.getCameraPosition().target;
    }

    @Override
    public void onCameraMoveStarted(int reason) {

        addCameraTargetToPath();
    }

    @Override
    public void onCameraMove() {

        addCameraTargetToPath();

    }

    @Override
    public void onCameraMoveCanceled() {

        addCameraTargetToPath();

    }

    @Override
    public void onCameraIdle() {

        addCameraTargetToPath();

    }
}