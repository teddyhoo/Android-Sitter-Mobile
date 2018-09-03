package com.leashtime.sitterapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.leashtime.sitterapp.events.SendVisitReportEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class VisitReport extends android.support.v7.app.AppCompatActivity  implements OnMapReadyCallback {

    private GoogleMap iMap;
    private PolylineOptions currPolylineOptions;
    public SupportMapFragment mapFragment;
    public ImageView snapImg;
    ImageView petPicture;
    public VisitDetail currentVisit;
    private VisitDetail currentVisitTemp;
    final VisitsAndTracking mVisitsAndTracking = VisitsAndTracking.getInstance();
    public static final int MY_PERMISSIONS_REQUEST_FILE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Lato-Regular.ttf"); // font from assets: "assets/fonts/Roboto-Regular.ttf
        setContentView(R.layout.visit_report_activity);
        petPicture = this.findViewById(R.id.visit_picture);
        snapImg = findViewById(R.id.snapShotImage);
        ImageButton backButton = findViewById(R.id.back_arrow);
        TextView clientInfoText = findViewById(R.id.petInfo);
        TextView timeInfoText = findViewById(R.id.visitReportTimeInfo);
        TextView otherInfo = findViewById(R.id.visitReportOther);
        final TextInputEditText visitNoteText = findViewById(R.id.sitterVisitNote);

        Bundle getData = getIntent().getExtras();
        String visit = getData.getString("visit");
        for (VisitDetail visitItem : mVisitsAndTracking.visitData) {
            if (visitItem.appointmentid.equals(visit)) {
                currentVisitTemp = visitItem;
            }
        }
        currentVisit = currentVisitTemp;
        final String appointmentid = currentVisitTemp.appointmentid;
        final VisitDetail visitDetailFinal = currentVisit;
        final String currVisSum = currentVisit.clientname;
        final String currPetSum = currentVisit.petNames;
        String currTime;

        if (currentVisit.arrived.equals("NONE")) {
            currTime = "Not started";
        } else {
            currTime = trimTime(currentVisit.arrived);
        }

        if (currentVisit.completed.equals("NONE")) {
            currTime = currTime + " - " + "Not complete";
        } else {
            currTime = currTime + " - " + trimTime(currentVisit.completed);
        }
        currentVisitTemp.mapSnapShotImage = "None";

        final String finalCurrentTime = currTime;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                petPicture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Intent startIntent = new Intent(getApplicationContext(), PhotoActivity.class);
                        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startIntent.putExtra("visitID", appointmentid);
                        getApplicationContext().startActivity(startIntent);
                    }
                });
                clientInfoText.setText(currVisSum);
                timeInfoText.setText(currPetSum);
                otherInfo.setText(finalCurrentTime);
                visitNoteText.setText(currentVisit.visitNoteBySitter);
                setMoodButtons(currentVisitTemp);

            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        backButton.post(new Runnable() {
                            @Override
                            public void run() {
                                currentVisit.visitNoteBySitter = visitNoteText.getText().toString();
                                mVisitsAndTracking.writeVisitDataToFile(currentVisit);
                                finish();
                            }
                        });
                    }
                }).start();
            }
        });
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setAlpha(0.5f);
                view.setScaleX(1.1f);
                view.setScaleY(1.1f);
                Date now = new Date();
                SimpleDateFormat rightNowFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                String dateTimeString = rightNowFormat.format(now);
                String visitNote = visitNoteText.getText().toString();
                visitDetailFinal.dateTimeVisitReportSubmit = dateTimeString;
                visitDetailFinal.visitNoteBySitter = visitNote;
                Toast.makeText(MainApplication.getAppContext(), "SENDING VISIT REPORT.", Toast.LENGTH_SHORT).show();
                SendVisitReportEvent event = new SendVisitReportEvent(visitDetailFinal.appointmentid);
                EventBus.getDefault().post(event);

                if (currentVisitTemp.mapSnapShotImage == null || currentVisitTemp.mapSnapShotImage.equals("None")) {
                    Toast.makeText(MainApplication.getAppContext(), "TAKING MAP SNAPSHOT", Toast.LENGTH_SHORT).show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            fab.post(new Runnable() {
                                @Override
                                public void run() {
                                    mVisitsAndTracking.writeVisitDataToFile(visitDetailFinal);
                                    System.out.println("taking MAP SNAP");
                                    takeSnapshot();
                                    //finish();
                                }
                            });
                        }
                    }).start();
                } else {
                    Toast.makeText(MainApplication.getAppContext(), "SENDING VISIT REPORT.", Toast.LENGTH_SHORT).show();
                    mVisitsAndTracking.sendMapSnapToServer(visitDetailFinal);
                    //finish();
                }
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        Handler handler = new Handler();
        Thread photoImportThread = new Thread() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (currentVisit.petPicFileName != null) {
                            File file = new File(currentVisit.petPicFileName);
                            Uri uri = Uri.fromFile(file);
                            BitmapFactory.Options newOpts = new BitmapFactory.Options();
                            newOpts.inSampleSize = 4;
                            try {
                                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(uri);
                                Bitmap bm = BitmapFactory.decodeStream(inputStream, null, newOpts);
                                petPicture.setImageBitmap(bm);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        /*if (currentVisit.mapSnapShotImage != null) {
                            System.out.println("file name: " + currentVisit.mapSnapShotImage);
                            File file = new File(currentVisit.mapSnapShotImage);
                            Uri uri = Uri.fromFile(file);
                            Context context = VisitReport.this.getApplicationContext();
                            GlideApp.with(context.getApplicationContext()).load(uri).into(snapImg);
                        }*/
                    }
                });
            }
        };
        photoImportThread.start();
        setupMapSnapShot();

        System.out.println("VISIT REPORT ON START");

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FILE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;

                    if (currentVisitTemp.mapSnapShotImage.equals("None") || currentVisitTemp.mapSnapShotImage == null) {
                        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.vMap);
                        ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
                        mapFragment.getMapAsync(this);
                    } else {
                        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.vMap);
                        View mapView = mapFragment.getView();
                        mapView.setVisibility(View.INVISIBLE);
                        Bitmap snapBitMap = BitmapFactory.decodeFile(currentVisitTemp.mapSnapShotImage);
                        snapImg.getLayoutParams().height = width;
                        snapImg.requestLayout();
                        snapImg.setImageBitmap(snapBitMap);
                        snapImg.setVisibility(View.VISIBLE);
                    }
                } else {
                    //code for deny
                }
                break;
        }
    }
    public void setupMapSnapShot() {
        System.out.println("SET UP MAP SNAP SHOT");

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        System.out.println("Map snap shot image filename: " + currentVisit.mapSnapShotImage);

        if (checkFileWritePermssion()) {

            if (currentVisitTemp.mapSnapShotImage.equals("None") || currentVisitTemp.mapSnapShotImage == null) {
                mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.vMap);
                ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
                params.width = width;
                mapFragment.getMapAsync(this);

            } else {
                mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.vMap);
                ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
                params.width = 0;
                params.height = 0;
                //ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
                //mapFragment.getMapAsync(this);

                System.out.println("VISIT REPORT: get map async");
                ViewGroup.LayoutParams params2 = snapImg.getLayoutParams();
                params2.height = width;
                params2.width = width;
                snapImg.setVisibility(View.VISIBLE);
                snapImg.setImageBitmap(BitmapFactory.decodeFile(currentVisit.mapSnapShotImage));
            }
        }
    }
    private void takeSnapshot() {
        System.out.println("TAKE SNAP");
        final VisitDetail finalTempVisit = currentVisitTemp;
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
            Bitmap mapBitmap;
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                mapBitmap = bitmap;
                System.out.println("THREAD BITMAP WRITE TO FILE: " + mapBitmap.getByteCount());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FileOutputStream fileOutputStream = null;
                        try {
                            fileOutputStream= new FileOutputStream(getPictureFile());
                            mapBitmap.compress(Bitmap.CompressFormat.JPEG, 25,fileOutputStream);
                        } catch (FileNotFoundException fnf) {
                            fnf.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fileOutputStream.close();
                                currentVisit.mapSnapShotImage = getPictureFile().getAbsolutePath();
                                writeMapBitmapToFile(finalTempVisit, bitmap);
                                System.out.println("MAP SNAP FINISH  FILE WRITING");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        };
        if (callback != null && iMap != null) {
            iMap.snapshot(callback);
        }
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("BEGIN UPLOAD TO SERVER OF MAP SNAP");
                mVisitsAndTracking.sendMapSnapToServer(currentVisitTemp);
                timer.cancel();
                finish();
            }
        };
        timer.schedule(timerTask, 2000, 2000);
    }

    public void writeMapBitmapToFile(VisitDetail visitDetail, Bitmap bitmap) {
        visitDetail.mapSnapShotImage = getPictureFile().getAbsolutePath();
        mVisitsAndTracking.writeVisitDataToFile(currentVisitTemp);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snapImg.setImageBitmap(BitmapFactory.decodeFile(currentVisit.mapSnapShotImage));
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        System.out.println("ON MAP READY");
        iMap = googleMap;
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        if (!currentVisitTemp.latitude.equals("NONE") && !currentVisitTemp.longitude.equals("NONE")) {
            String latitude = currentVisitTemp.latitude;
            double lat = Double.parseDouble(latitude);
            String longitude = currentVisitTemp.longitude;
            double lon = Double.parseDouble(longitude);
            LatLng visitLatLon = new LatLng(lat, lon);
            if (lat != 0 && lon != 0) {
                iMap.moveCamera(CameraUpdateFactory.newLatLngZoom(visitLatLon, 12.0f));
            }
            ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
            params.height = width;
            mapFragment.getView().setLayoutParams(params);
            mapFragment.getView().requestLayout();
            iMap.addMarker(new MarkerOptions().position(visitLatLon).title(currentVisitTemp.clientname));
            drawPolylinesForVisit(currentVisitTemp, Color.RED);
        }
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
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onSendVisitReportEvent(SendVisitReportEvent event) {
        VisitsAndTracking sVisitsAndTracking = VisitsAndTracking.getInstance();
        VisitDetail currentVisit = new VisitDetail();
        String visitID = event.message;
        for (VisitDetail visit : sVisitsAndTracking.visitData) {
            if (visit.appointmentid.equals(visitID)) {
                currentVisit = visit;
            }
        }

        String dateTimeStringHTTP = getDate();
        HttpUrl.Builder urlBuilderVisitReport = HttpUrl.parse("https://leashtime.com/native-visit-update.php").newBuilder();
        urlBuilderVisitReport.addQueryParameter("loginid", sVisitsAndTracking.mPreferences.getString("username", ""));
        urlBuilderVisitReport.addQueryParameter("password", sVisitsAndTracking.mPreferences.getString("password", ""));
        urlBuilderVisitReport.addQueryParameter("datetime", dateTimeStringHTTP);
        urlBuilderVisitReport.addQueryParameter("appointmentptr", currentVisit.appointmentid);
        String consolidatedVisitNote = currentVisit.note;
        urlBuilderVisitReport.addQueryParameter("note", currentVisit.visitNoteBySitter);
        urlBuilderVisitReport.addQueryParameter("buttons", constructMoodButtonRequestDic(currentVisit));

        String url = urlBuilderVisitReport.toString();

        if (sVisitsAndTracking.USER_AGENT == null) {
            sVisitsAndTracking.USER_AGENT = "LeashTime Android / null user agent";
        }

        final Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", sVisitsAndTracking.USER_AGENT)
                .build();

        final VisitDetail fCurrentVisit = currentVisit;
        if (!checkNetworkConnection()) {
            fCurrentVisit.visitReportUploadStatus = "FAIL";
            fCurrentVisit.mapSnapImageCreate = "FAIL";
            fCurrentVisit.visitReportRequest = request;
            sVisitsAndTracking.writeVisitDataToFile(fCurrentVisit);
        } else {
            OkHttpClient sendReportClient = new OkHttpClient();
            sendReportClient.newCall(request).enqueue(new Callback() {
                ResponseBody body;

                @Override
                public void onFailure(Call call, IOException e) {
                    fCurrentVisit.visitReportUploadStatus = "FAIL";
                    fCurrentVisit.visitReportRequest = request;
                }

                @Override
                public void onResponse(Call call, Response response) {
                    body = response.body();
                    System.out.println("FINISHED VISIT REPORT FORM SEND TO SERVER: " + body);
                    body.close();
                    fCurrentVisit.visitReportUploadStatus = "SUCCESS";
                    //finish();
                }
            });
        }
    }

    public Boolean checkFileWritePermssion() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission request");
                    alertBuilder.setMessage("Please grant permission write to file system.");
                    alertBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        // @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_FILE);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_FILE);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
    public File getPictureFile() {
        String fileName = "MAP_" + currentVisitTemp.appointmentid + ".jpg";
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
    }
    private static String getDate() {
        Date transmitDate = new Date();
        SimpleDateFormat rightNowFormatTransmit = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return rightNowFormatTransmit.format(transmitDate);
    }
    private static String constructMoodButtonRequestDic(VisitDetail visit) {

        String moodButton = "{";

        if (visit.didPoo) {
            moodButton += "\"poo\" : \"yes\",";
        } else {
            moodButton += "\"poo\" : \"no\",";
        }
        if (visit.didPee) {
            moodButton += "\"pee\" : \"yes\",";
        } else {
            moodButton += "\"pee\" : \"no\",";

        }
        if (visit.didPlay) {
            moodButton += "\"play\" : \"yes\",";
        } else {
            moodButton += "\"play\" : \"no\",";

        }
        if (visit.wasHappy) {
            moodButton += "\"happy\" : \"yes\",";
        } else {
            moodButton += "\"happy\" : \"no\",";

        }

        if (visit.wasSad) {
            moodButton += "\"sad\" : \"yes\",";
        } else {
            moodButton += "\"sad\" : \"no\",";

        }
        if (visit.wasAngry) {
            moodButton += "\"angry\" : \"yes\",";
        } else {
            moodButton += "\"angry\" : \"no\",";

        }
        if (visit.wasShy) {
            moodButton += "\"shy\" : \"yes\",";
        } else {
            moodButton += "\"shy\" : \"no\",";
        }
        if (visit.wasHungry) {
            moodButton += "\"hungry\" : \"yes\",";
        } else {
            moodButton += "\"hungry\" : \"no\",";
        }
        if (visit.wasSick) {
            moodButton += "\"sick\" : \"yes\",";
        } else {
            moodButton += "\"sick\" : \"no\",";
        }
        if (visit.wasCat) {
            moodButton += "\"cat\" : \"yes\",";
        } else {
            moodButton += "\"cat\" : \"no\",";
        }
        if (visit.didScoopLitter) {
            moodButton += "\"litter\" : \"yes\"";
        } else {
            moodButton += "\"litter\" : \"no\"";
        }
        moodButton += "}";
        return moodButton;

    }
    public void setMoodButtons(VisitDetail visitDetailMood) {
        setMoodButtonAlpha(visitDetailMood);
        ImageButton pooButton = findViewById(R.id.pooButton);
        ImageButton peeButton = findViewById(R.id.peeButton);
        ImageButton playButton = findViewById(R.id.playButton);
        ImageButton happyButton = findViewById(R.id.happyButton);
        ImageButton sadButton = findViewById(R.id.sadButton);
        ImageButton angryButton = findViewById(R.id.angryButton);
        ImageButton hungryButton = findViewById(R.id.hungryButton);
        ImageButton sickButton = findViewById(R.id.sickButton);
        ImageButton litterButton = findViewById(R.id.litterButton);

        pooButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.didPoo = !visitDetailMood.didPoo;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });
        peeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.didPee = !visitDetailMood.didPee;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.didPlay = !visitDetailMood.didPlay;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });
        happyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.wasHappy = !visitDetailMood.wasHappy;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });
        sadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.wasSad = !visitDetailMood.wasSad;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });
        angryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.wasAngry = !visitDetailMood.wasAngry;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });
        hungryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.wasHungry = !visitDetailMood.wasHungry;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });
        sickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.wasSick = !visitDetailMood.wasSick;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });

        litterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visitDetailMood.didScoopLitter = !visitDetailMood.didScoopLitter;
                setMoodButtonAlpha(visitDetailMood);
                view.requestLayout();
            }
        });

    }
    private void setMoodButtonAlpha(VisitDetail visit) {

        float alphaOn = 1.000f;
        float alphaOff = 0.1000f;

        ImageButton pooButton = findViewById(R.id.pooButton);
        if (visit.didPoo) {
            pooButton.setAlpha(alphaOn);
        } else {
            pooButton.setAlpha(alphaOff);
        }

        ImageButton peeButton = findViewById(R.id.peeButton);
        if (visit.didPee) {
            peeButton.setAlpha(alphaOn);
        } else {
            peeButton.setAlpha(alphaOff);
        }

        ImageButton playButton = findViewById(R.id.playButton);
        if (visit.didPlay) {
            playButton.setAlpha(alphaOn);
        } else {
            playButton.setAlpha(alphaOff);
        }

        ImageButton happyButton = findViewById(R.id.happyButton);
        if (visit.wasHappy) {
            happyButton.setAlpha(alphaOn);
        } else {
            happyButton.setAlpha(alphaOff);
        }

        ImageButton sadButton = findViewById(R.id.sadButton);
        if (visit.wasSad) {
            sadButton.setAlpha(alphaOn);
        } else {
            sadButton.setAlpha(alphaOff);
        }

        ImageButton angryButton = findViewById(R.id.angryButton);
        if (visit.wasAngry) {
            angryButton.setAlpha(alphaOn);
        } else {
            angryButton.setAlpha(alphaOff);
        }

        ImageButton hungryButton = findViewById(R.id.hungryButton);
        if (visit.wasHungry) {
            hungryButton.setAlpha(alphaOn);
        } else {
            hungryButton.setAlpha(alphaOff);
        }

        ImageButton sickButton = findViewById(R.id.sickButton);
        if (visit.wasSick) {
            sickButton.setAlpha(alphaOn);
        } else {
            sickButton.setAlpha(alphaOff);
        }

        ImageButton litterButton = findViewById(R.id.litterButton);
        if (visit.didScoopLitter) {
            litterButton.setAlpha(alphaOn);
        } else {
            litterButton.setAlpha(alphaOff);
        }
    }
    private static String trimTime(String timeValStr) {

        String newString = null;
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(timeValStr);
            newString = new SimpleDateFormat("h:mm", Locale.US).format(date);
        } catch (Exception e2) {
            try {
                Date date = new SimpleDateFormat("HH:mm:ss", Locale.US).parse(timeValStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return newString;
    }
    @Override
    protected void onStop() {
        System.out.println("VISIT REPORT ON STOP");
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        final TextInputEditText visitNoteText = findViewById(R.id.sitterVisitNote);
        currentVisit.visitNoteBySitter = visitNoteText.getText().toString();
        mVisitsAndTracking.writeVisitDataToFile(currentVisit);
        finish();
    }
    public boolean checkNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        if (ni != null) {
            if (ni.isConnectedOrConnecting()) {
                int type = ni.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    return TRUE;
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    return TRUE;
                } else if (type == ConnectivityManager.TYPE_WIMAX) {
                    return TRUE;
                } else {
                    return FALSE;
                }
            }
        }
        return FALSE;
    }
}
