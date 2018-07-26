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
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.leashtime.sitterapp.Exif.ExifInterface;
import com.leashtime.sitterapp.events.ReloadVisitsEvent;
import com.leashtime.sitterapp.events.SavePhotoEvent;
import com.leashtime.sitterapp.network.SendPhotoServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.result.PendingResult;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.view.CameraView;

import static com.leashtime.sitterapp.ResultHolder.getPictureFile;

public class PhotoActivity extends AppCompatActivity{
    private ImageButton capturePhotoButton;
    private ImageButton chooseImage;
    private ImageButton backButton;
    public Button usePhoto;
    public Button takeAgain;
    private String visitID;
    private VisitsAndTracking sVisitsAndTracking;
    private Context mContext;
    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    public static final int MY_PERMISSIONS_REQUEST_FILE = 200;
    public Fotoapparat gFotoapparat;
    public PhotoResult photoResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getApplicationContext();
        setContentView(R.layout.photo_activity);
        EventBus.getDefault().register(this);
        Bundle getData = getIntent().getExtras();

        sVisitsAndTracking = VisitsAndTracking.getInstance();
        visitID = getData.getString("visitID");
        usePhoto = findViewById(R.id.usePhoto);
        takeAgain = findViewById(R.id.takeAgain);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCameraWithPermissions();
                } else {
                    //code for deny
                }
                break;
            case MY_PERMISSIONS_REQUEST_FILE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeImagePermissionGranted();
                } else {
                    //code for deny
                }
                break;
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (checkCameraPermission()) {
            startCameraWithPermissions();
        }
    }
    @Override
    protected  void onStop() {
        if (gFotoapparat != null) gFotoapparat.stop();
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void sendPhotoToServer(VisitDetail visit) {
        SendPhotoServer photoUpload = new SendPhotoServer(sVisitsAndTracking.mPreferences.getString("username",""), sVisitsAndTracking.mPreferences.getString("password",""), visit, "petPhoto");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSavePhotoEvent(SavePhotoEvent event) {
        int pos = 0;
        for(VisitDetail visitItem : sVisitsAndTracking.visitData) {
            if (event.appointmentID.equals(visitItem.appointmentid)) {
                sVisitsAndTracking.writeVisitDataToFile(visitItem);
                sendPhotoToServer(visitItem);
                ReloadVisitsEvent visitReportEvent = new ReloadVisitsEvent();
                EventBus.getDefault().post(visitReportEvent);
            }
            pos++;
        }
    }

    public Boolean checkCameraPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;

        if(currentAPIVersion>= Build.VERSION_CODES.M)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission request");
                    alertBuilder.setMessage("Please grant permission to use the camera.");
                    alertBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        // @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions((Activity)getApplicationContext(), new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
    public Boolean checkFileWritePermssion() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if(currentAPIVersion>= Build.VERSION_CODES.M)
        {
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
                            ActivityCompat.requestPermissions((Activity)getApplicationContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_FILE);
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
    public void writeImagePermissionGranted() {

        ImageView previewImage = findViewById(R.id.previewImage);
        File saveFilename = getImageFileName();
        for (VisitDetail visit : sVisitsAndTracking.visitData) {
            if (visit.appointmentid.equals(visitID)) {
                visit.petPicFileName = saveFilename.getAbsolutePath();
                sVisitsAndTracking.writeVisitDataToFile(visit);
            }
        }

        photoResult.saveToFile(saveFilename).whenAvailable(new PendingResult.Callback<Void>() {
            @Override
            public void onResult(Void aVoid) {

                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                //System.out.println("Display width: " + size.x + " height: "  + size.y);
                int width = size.x;

                for (VisitDetail visitDetail : sVisitsAndTracking.visitData) {
                    if (visitID.equals(visitDetail.appointmentid)) {
                        int orientation = 0;
                        Matrix matrix = new Matrix();
                        ExifInterface exifInterface = new ExifInterface();
                        try {
                            exifInterface.readExif(visitDetail.petPicFileName);
                            Integer val = exifInterface.getTagIntValue(ExifInterface.TAG_ORIENTATION);
                            if (val != null) {
                                orientation = ExifInterface.getRotationForOrientationValue(val.shortValue());
                                if (orientation  ==  90) {
                                    matrix.setRotate(90);
                                } else if (orientation == 180) {
                                } else if (orientation == 270) {
                                    matrix.setRotate(270);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        BitmapFactory.Options bmOpt = new BitmapFactory.Options();
                        bmOpt.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(visitDetail.petPicFileName, bmOpt);
                        float imgWidth = bmOpt.outWidth;
                        float imgHeight = bmOpt.outHeight;
                        int sampleSize = 2;
                        if (width > 600) {
                            sampleSize = 4;
                        }

                        BitmapFactory.Options imageOptions = new BitmapFactory.Options();
                        imageOptions.inSampleSize = sampleSize;
                        Bitmap bmFile = BitmapFactory.decodeFile(visitDetail.petPicFileName,imageOptions);

                        int widthBM = bmFile.getWidth();
                        int heightBM = bmFile.getHeight();
                        int netWidth = 0;
                        int netHeight = 0;
                        int dimension = 0;

                        if (widthBM >  heightBM && orientation == 90) {
                            netWidth  = (widthBM - heightBM)/2;
                            dimension = heightBM;
                        } else if (widthBM > heightBM && orientation == 0) {
                            netHeight = (widthBM - heightBM) / 2;
                            dimension = heightBM;
                        }

                        /*System.out.println("Bitmap size: " +widthBM + ", "
                                + heightBM +  ",  netWidthHeight: "
                                + netHeight + ", " +  netWidth
                                + " ---> " + "orientation: "
                                +  orientation + ", dimension: " + dimension);*/

                        try {
                            Bitmap bmSheared = Bitmap.createBitmap(bmFile, netWidth, netHeight, dimension,dimension);
                            Bitmap bmRotated = Bitmap.createBitmap(bmSheared, 0, 0, bmSheared.getWidth(), bmSheared.getHeight(),matrix, true);
                            // System.out.println("Sheared bitmap x: " + bmSheared.getWidth() + ", y: "  + bmSheared.getHeight() + " Byte count: " + bmSheared.getByteCount());
                            writeImageToFileAndSend(bmRotated,visitDetail,saveFilename);
                            // bmFile.recycle();
                            //bmSheared.recycle();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    previewImage.setImageBitmap(bmRotated);
                                }
                            });
                        } catch (OutOfMemoryError e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }
    public void startCameraWithPermissions () {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        // handleFoto.start();
        //chooseImage = (ImageButton) findViewById(R.id.photoStack);
        // backButton = (ImageButton) findViewById(R.id.backButton);
       /* backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });*/
       /* chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent().setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                System.out.println("Intent for image picker");
                startActivityForResult(Intent.createChooser(i, "Select File"), SELECT_FILE);
            }
        });*/

        CameraView cameraView = findViewById(R.id.camera_view);
        cameraView.getLayoutParams().height = width+40;
        cameraView.requestLayout();

        gFotoapparat = Fotoapparat.with(mContext)
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .build();
        gFotoapparat.start();

        Fotoapparat handleFoto = gFotoapparat;
        capturePhotoButton = findViewById(R.id.takePhoto);
        ImageButton capturePhotoFinal = capturePhotoButton;
        ImageView previewImage = findViewById(R.id.previewImage);
        Button usePhotoFinal = usePhoto;
        Button takeAgainFinal = takeAgain;

        previewImage.setVisibility(View.INVISIBLE);

        usePhotoFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhotoFinal.setVisibility(View.VISIBLE);
                takeAgainFinal.setVisibility(View.INVISIBLE);
                usePhotoFinal.setVisibility(View.INVISIBLE);
                previewImage.setVisibility(View.INVISIBLE);
                finish();

            }
        });

        takeAgainFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhotoFinal.setVisibility(View.VISIBLE);
                takeAgainFinal.setVisibility(View.INVISIBLE);
                usePhotoFinal.setVisibility(View.INVISIBLE);
                previewImage.setVisibility(View.INVISIBLE);
            }
        });

        capturePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usePhotoFinal.setVisibility(View.VISIBLE);
                takeAgainFinal.setVisibility(View.VISIBLE);
                previewImage.setVisibility(View.VISIBLE);
                capturePhotoButton.setVisibility(View.INVISIBLE);
                File saveFilename = getImageFileName();
                for (VisitDetail visit : sVisitsAndTracking.visitData) {
                    if (visit.appointmentid.equals(visitID)) {
                        visit.petPicFileName = saveFilename.getAbsolutePath();
                        sVisitsAndTracking.writeVisitDataToFile(visit);
                    }
                }
                photoResult = handleFoto.takePicture();
                if (checkFileWritePermssion()) {
                    photoResult.saveToFile(saveFilename).whenAvailable(new PendingResult.Callback<Void>() {
                        @Override
                        public void onResult(Void aVoid) {
                            for (VisitDetail visitDetail : sVisitsAndTracking.visitData) {
                                if (visitID.equals(visitDetail.appointmentid)) {
                                    int orientation = 0;
                                    Matrix matrix = new Matrix();
                                    ExifInterface exifInterface = new ExifInterface();
                                    try {
                                        exifInterface.readExif(visitDetail.petPicFileName);
                                        Integer val = exifInterface.getTagIntValue(ExifInterface.TAG_ORIENTATION);
                                        if (val != null) {
                                            orientation = ExifInterface.getRotationForOrientationValue(val.shortValue());
                                            if (orientation  ==  90) {
                                                matrix.setRotate(90);
                                            } else if (orientation == 180) {
                                            } else if (orientation == 270) {
                                                matrix.setRotate(270);
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    BitmapFactory.Options bmOpt = new BitmapFactory.Options();
                                    bmOpt.inJustDecodeBounds = true;
                                    BitmapFactory.decodeFile(visitDetail.petPicFileName, bmOpt);
                                    float imgWidth = bmOpt.outWidth;
                                    float imgHeight = bmOpt.outHeight;
                                    int sampleSize = 2;
                                    if (width > 600) {
                                        sampleSize = 4;
                                    }  else if (width > 2000) {
                                        sampleSize = 8;
                                    }

                                    BitmapFactory.Options imageOptions = new BitmapFactory.Options();
                                    imageOptions.inSampleSize = sampleSize;
                                    Bitmap bmFile = BitmapFactory.decodeFile(visitDetail.petPicFileName,imageOptions);

                                    int widthBM = bmFile.getWidth();
                                    int heightBM = bmFile.getHeight();
                                    int netWidth = 0;
                                    int netHeight = 0;
                                    int dimension = 0;

                                    if (widthBM >  heightBM && orientation == 90) {
                                        netWidth  = (widthBM - heightBM)/2;
                                        dimension = heightBM;
                                    } else if (widthBM > heightBM && orientation == 0) {
                                        netHeight = (widthBM - heightBM) / 2;
                                        dimension = heightBM;
                                    }
                                    try {
                                        Bitmap bmSheared = Bitmap.createBitmap(bmFile, netWidth, netHeight, dimension,dimension);
                                        Bitmap bmRotated = Bitmap.createBitmap(bmSheared, 0, 0, bmSheared.getWidth(), bmSheared.getHeight(),matrix, true);
                                        bmFile.recycle();
                                        bmSheared.recycle();
                                        writeImageToFileAndSend(bmRotated,visitDetail,saveFilename);

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                previewImage.setImageBitmap(bmRotated);
                                            }
                                        });
                                    } catch (OutOfMemoryError e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }
    public void writeImageToFileAndSend(Bitmap image, VisitDetail visitDetail, File imageFileName) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream(image.getByteCount());
        image.compress(Bitmap.CompressFormat.JPEG,100, bos);

        try {
            imageFileName.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(imageFileName);
            fileOutputStream.write(bos.toByteArray());
        } catch (FileNotFoundException fnf) {
            fnf.printStackTrace();
        }  catch  (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
                bos.close();
                SavePhotoEvent photoTake = new SavePhotoEvent(visitDetail.appointmentid, "file");
                EventBus.getDefault().post(photoTake);
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if(resultCode == AppCompatActivity.RESULT_OK) {
            VisitsAndTracking mVisitsTracking = VisitsAndTracking.getInstance();
            ImageView previewImage = findViewById(R.id.previewImage);
            File finalFileNameImage = getPictureFile();
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;

            try {
                finalFileNameImage.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }
            for (VisitDetail currentVisit : mVisitsTracking.visitData) {
                if (currentVisit.appointmentid.equals(visitID)) {
                    currentVisit.petPicFileName = finalFileNameImage.getAbsolutePath();
                }
            }
            if (resultData != null) {
                try {
                    Uri resource = resultData.getData();
                    InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(resource);
                    ExifInterface exifInterface = new ExifInterface();
                    int orientation = 0;
                    Matrix matrix = new Matrix();

                    try {
                        exifInterface.readExif(inputStream);
                        Integer val = exifInterface.getTagIntValue(ExifInterface.TAG_ORIENTATION);
                        if (val != null) {
                            orientation = ExifInterface.getRotationForOrientationValue(val.shortValue());
                            if (orientation  ==  90) {
                                matrix.setRotate(90);
                            } else if (orientation == 180) {

                            } else if (orientation == 270) {
                                matrix.setRotate(270);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    InputStream inputStream2 = getApplicationContext().getContentResolver().openInputStream(resource);
                    BitmapFactory.Options imageFileOptions = new BitmapFactory.Options();
                    imageFileOptions.inJustDecodeBounds = true;
                    BitmapFactory.Options newOpts = new BitmapFactory.Options();
                    if (imageFileOptions.outWidth > 600)
                        newOpts.inSampleSize = 4;
                    else
                        newOpts.inSampleSize = 2;
                    Bitmap bm = BitmapFactory.decodeStream(inputStream2, null, newOpts);
                    int widthBM = bm.getWidth();
                    int heightBM = bm.getHeight();
                    int netWidth = 0;
                    int netHeight = 0;
                    int dimension = 0;

                    if (widthBM >  heightBM && orientation == 90) {
                        netWidth  = (widthBM - heightBM)/2;
                        dimension = heightBM;
                    } else if (widthBM > heightBM && orientation == 0) {
                        netWidth = (widthBM - heightBM) / 2;
                        dimension = heightBM;
                    } else if (heightBM > widthBM && orientation == 0) {
                        netHeight = (heightBM - widthBM)/2;
                        dimension = widthBM;
                    } else if (heightBM == widthBM && orientation == 0) {
                        netHeight = 0;
                        netWidth = 0;
                        dimension = widthBM;
                    }


                    // System.out.println("Bitmap size: " +widthBM + ", " + heightBM +  ",  netWidthHeight: " + netHeight + ", " +  netWidth + " ---> " + "orientation: " +  orientation + ", dimension: " + dimension);
                    ByteArrayOutputStream bos = null;

                    try {
                        Bitmap bmSheared = Bitmap.createBitmap(bm,netWidth, netHeight, dimension, dimension);
                        Bitmap bmRotated = Bitmap.createBitmap(bmSheared, 0, 0, bmSheared.getWidth(), bmSheared.getHeight(),matrix, true);
                        bos = new ByteArrayOutputStream(bmRotated.getByteCount());
                        bmRotated.compress(Bitmap.CompressFormat.JPEG, 90, bos);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                previewImage.setImageBitmap(bmRotated);
                            }
                        });
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                    }

                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = new FileOutputStream(finalFileNameImage);
                        fileOutputStream.write(bos.toByteArray());
                    } catch (FileNotFoundException fnf) {
                        fnf.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            bos.close();
                            fileOutputStream.close();
                            SavePhotoEvent photoTake = new SavePhotoEvent(visitID, "file");
                            EventBus.getDefault().post(photoTake);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException fe) {
                    fe.printStackTrace();
                }
            }
        }
    }
    public File getImageFileName() {

        SimpleDateFormat rightNowDateFormat = new SimpleDateFormat("MM-dd-yy", Locale.US);
        SimpleDateFormat rightNowFormatTransmit = new SimpleDateFormat("HH:mm:ss", Locale.US);
        Date transmitDate = new Date();
        String nowTime = rightNowFormatTransmit.format(transmitDate);
        String nowDate = rightNowDateFormat.format(transmitDate);
        String concatDate = nowDate + "_" + nowTime;
        String fileName = "PHOTO_" + visitID + "_" + concatDate + ".jpg";
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
    }

}
