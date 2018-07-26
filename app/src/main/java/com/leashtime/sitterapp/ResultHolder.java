package com.leashtime.sitterapp;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.annotation.Nullable;
import com.wonderkiln.camerakit.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultHolder {
    private static WeakReference<Bitmap> image;
    private static Size nativeCaptureSize;
    private static long timeToCallback;
    private static String theCurrentVisitID;
    public static VisitsAndTracking visitsAndTracking;

    public static void setVisitID (String currentVisitID) {
        theCurrentVisitID =  currentVisitID;
    }

    public static void setImage(@Nullable Bitmap image) {
        if(image != null) {
            System.out.println("size image: " +image.getWidth() + ", " + image.getHeight());
            ResultHolder.image = new WeakReference<Bitmap>(image);
            File finalFileNameImage = getPictureFile();
            visitsAndTracking = VisitsAndTracking.getInstance();

            for (VisitDetail visit : visitsAndTracking.visitData) {
                if (visit.appointmentid.equals(theCurrentVisitID)) {
                    visit.petPicFileName = finalFileNameImage.getAbsolutePath();
                    try {
                        finalFileNameImage.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {

                    }
                }
            }

            int size = image.getByteCount();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(size);
            image.compress(Bitmap.CompressFormat.JPEG, 50, bos);
            FileOutputStream fileOutputStream = null;

            try {
                fileOutputStream = new FileOutputStream(finalFileNameImage);
                fileOutputStream.write(bos.toByteArray());
                int sizeImg = bos.size();

            } catch (FileNotFoundException fnf) {
                fnf.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bos.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {

        }
        ResultHolder.image = image != null ? new WeakReference<>(image) : null;
    }

    public static File getPictureFile() {
        SimpleDateFormat rightNowDateFormat = new SimpleDateFormat("MM-dd-yy", Locale.US);
        SimpleDateFormat rightNowFormatTransmit = new SimpleDateFormat("HH:mm:ss", Locale.US);
        Date transmitDate = new Date();
        String nowTime = rightNowFormatTransmit.format(transmitDate);
        String nowDate = rightNowDateFormat.format(transmitDate);
        String concatDate = nowDate + "_" + nowTime;
        String fileName = "PHOTO_" + theCurrentVisitID + "_" + concatDate + ".jpg";
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
    }


    @Nullable
    public static Bitmap getImage() {
        return image != null ? image.get() : null;
    }

    public static void setNativeCaptureSize(@Nullable Size nativeCaptureSize) {
        ResultHolder.nativeCaptureSize = nativeCaptureSize;
    }

    @Nullable
    public static Size getNativeCaptureSize() {
        return nativeCaptureSize;
    }

    public static void setTimeToCallback(long timeToCallback) {
        ResultHolder.timeToCallback = timeToCallback;
    }

    public static long getTimeToCallback() {
        return timeToCallback;
    }

    public static void dispose() {
        setImage(null);
        setNativeCaptureSize(null);
        setTimeToCallback(0);
    }
}

