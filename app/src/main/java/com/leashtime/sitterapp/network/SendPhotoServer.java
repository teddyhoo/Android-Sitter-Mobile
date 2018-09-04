package com.leashtime.sitterapp.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.leashtime.sitterapp.MainApplication;
import com.leashtime.sitterapp.VisitDetail;
import com.leashtime.sitterapp.VisitsAndTracking;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SendPhotoServer {

    public  static final VisitsAndTracking sVisitsAndTracking = VisitsAndTracking.getInstance();
    public VolleyMultipartRequest multiPartRequest;

    public SendPhotoServer(String name, String pass, VisitDetail visitID, String type) {

        String url;
        String username = name;
        String password = pass;
        VisitDetail visitDetail = visitID;
        String requestType = type;
        Response.Listener responseListen = new SendPhotoServer.NetworkResponseListener();
        Response.ErrorListener errorListen = new SendPhotoServer.MyErrorListener();

        if  (type.equals("petPhoto")) {
            String imageFileSend = visitID.petPicFileName;
            url = "https://leashtime.com/appointment-photo-upload.php";
            multiPartRequest = new SendPhotoServer.MyVolleyMultipartRequest(url, responseListen, errorListen, username, password, requestType, visitDetail, imageFileSend);
        } else if (type.equals("map")) {
            String imageFileSend = visitID.mapSnapShotImage;
            url = "https://leashtime.com/appointment-map-upload.php";
            multiPartRequest = new SendPhotoServer.MyVolleyMultipartRequest(url, responseListen, errorListen, username, password, requestType, visitDetail, imageFileSend);
        }

        if (checkNetworkConnection()) {
            if (type.equals("petPhoto")) {
                visitID.imageUploadStatus = "PEND";
            } else if(type.equals("map")) {
                visitID.mapSnapUploadStatus = "PEND";
            }
            sVisitsAndTracking.writeVisitDataToFile(visitID);
            VolleySingleton.getInstance(MainApplication.getAppContext()).addToRequestQueue(multiPartRequest);

        } else {
            System.out.println("Failed " + type);
            if (type.equals("petPhoto")) {
                visitID.imageUploadStatus = "FAIL";
            } else if(type.equals("map")) {
                visitID.mapSnapUploadStatus = "FAIL";
            }
            sVisitsAndTracking.writeVisitDataToFile(visitID);
        }
    }


    private static class MyVolleyMultipartRequest extends VolleyMultipartRequest {
        private final String username;
        private final String password;
        private final String appointmentid;
        private final VisitDetail visitID;
        private final String imageFileSend;
        private final String imageType;

        public MyVolleyMultipartRequest(String url,
                                        Response.Listener responseListen,
                                        Response.ErrorListener errorListener,
                                        String username,
                                        String password,
                                        String type,
                                        VisitDetail visitID,
                                        String imageFileSend) {

            super(Request.Method.POST, url, responseListen, errorListener);
            this.username = username;
            this.password = password;
            this.appointmentid = visitID.appointmentid;
            this.visitID = visitID;
            this.imageFileSend = imageFileSend;
            this.imageType = type;
        }

        @Override
        protected Map<String, String> getParams() {
            Map<String, String> params = new HashMap<>();
            params.put("loginid", username);
            params.put("password", password);
            params.put("appointmentid", appointmentid);
            return params;
        }

        @Override
        protected Map<String, VolleyMultipartRequest.DataPart> getByteData() {
            Map<String, VolleyMultipartRequest.DataPart> params = new HashMap<>();
            params.put("image", new VolleyMultipartRequest.DataPart(visitID.petPicFileName, getByteUploadData(imageFileSend), "image/jpg"));

            return params;
        }

        @Override
        protected byte[] getByteUploadData(String sendFileName) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                if (imageType.equals("map")) {
                    options.inSampleSize = 4;
                }
                Bitmap visitImage = BitmapFactory.decodeFile(sendFileName, options);
                visitImage.compress(Bitmap.CompressFormat.JPEG, 25, byteArrayOutputStream);
            } catch (Exception  e) {
                e.printStackTrace();
            } finally {
                byte [] imageArray = byteArrayOutputStream.toByteArray();
                int sizeImage = imageArray.length;
                for (int i = 0; i < sizeImage; i++) {
                    Byte imgByte = imageArray[i];
                    System.out.println("Image byte: " +  imgByte.toString());
                }
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    private static class MyErrorListener implements Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {

            NetworkResponse networkResponse = error.networkResponse;
            String result = null;
            if (null == networkResponse) {
                if (error.getClass().equals(TimeoutError.class)) {
                    result = "Time out error";
                    for (VisitDetail visitDetail : sVisitsAndTracking.visitData) {
                        if (sVisitsAndTracking.onWhichVisitID.equals(visitDetail.appointmentid)) {
                            if (visitDetail.imageUploadStatus.equals("PEND")) {
                                visitDetail.imageUploadStatus = "FAIL";
                            } else if (visitDetail.mapSnapUploadStatus.equals("PEND")) {
                                visitDetail.mapSnapUploadStatus = "FAIL";
                            }
                            sVisitsAndTracking.writeVisitDataToFile(visitDetail);
                        }
                    }

                } else if (error.getClass().equals(NoConnectionError.class)) {
                    result = "Failed to connect server";
                    for (VisitDetail visitDetail : sVisitsAndTracking.visitData) {
                        if (sVisitsAndTracking.onWhichVisitID.equals(visitDetail.appointmentid)) {
                            if (visitDetail.imageUploadStatus.equals("PEND")) {
                                visitDetail.imageUploadStatus = "FAIL";
                            } else if (visitDetail.mapSnapUploadStatus.equals("PEND")) {
                                visitDetail.mapSnapUploadStatus = "FAIL";
                            }
                            sVisitsAndTracking.writeVisitDataToFile(visitDetail);

                        }
                    }

                }
            } else {
                result = new String(networkResponse.data);
                System.out.println("VOLLEY RESPONSE: " + result);
            }
        }
    }

    private static class NetworkResponseListener implements Response.Listener<NetworkResponse> {

        @Override
        public void onResponse(NetworkResponse response) {
            byte[] responseByte = response.data;
            if (response.statusCode != 200) {
                for (VisitDetail visitDetail : sVisitsAndTracking.visitData) {
                    if (sVisitsAndTracking.onWhichVisitID.equals(visitDetail.appointmentid)) {
                        if (visitDetail.imageUploadStatus.equals("PEND")) {
                            visitDetail.imageUploadStatus = "FAIL";
                        } else if (visitDetail.mapSnapUploadStatus.equals("PEND")) {
                            visitDetail.mapSnapUploadStatus = "FAIL";
                        }
                    }
                }
            }
            else {
                for (VisitDetail visitDetail : sVisitsAndTracking.visitData) {
                    if (sVisitsAndTracking.onWhichVisitID.equals(visitDetail.appointmentid)) {
                        if (visitDetail.imageUploadStatus.equals("PEND")) {
                            visitDetail.imageUploadStatus = "SUCCESS";
                        } else if (visitDetail.mapSnapUploadStatus.equals("PEND")) {
                            visitDetail.mapSnapUploadStatus = "SUCCESS";
                        }
                    }
                }
            }
        }
    }

    public static boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) MainApplication.getInstance().getApplicationContext().getSystemService (Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }
    public static boolean isInternetAvailable() {
        String host = "www.google.com";
        int port = 80;
        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(host, port), 2000);
            socket.close();
            return true;
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException es) {}
            return false;
        }
    }
    public boolean checkNetworkConnection() {

        if (isNetworkConnected()) {
            //System.out.println("NETWORK IS CONNECTED");
            ConnectivityManager cm = (ConnectivityManager) MainApplication.getInstance().getApplicationContext().getSystemService (Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if (ni.isConnectedOrConnecting()) {
                //System.out.println("NETWORK CONNECTION  RECOGNIZED");
                int type = ni.getType();

                if (null != ni) {
                    if(type == ConnectivityManager.TYPE_WIFI) {
                        //System.out.println("WIFI");
                        return true;
                    } else if (type== ConnectivityManager.TYPE_MOBILE) {
                        //System.out.println("MOBILE");
                        return true;
                    } else if (type== ConnectivityManager.TYPE_WIMAX) {
                        //System.out.println("WIMAX");
                        return true;
                    } else {
                        //System.out.println("NO CONNECT type");
                        return false;
                    }
                }

                if (isInternetAvailable()) {
                    //System.out.println("CHECKING SOCKET.... FINE");
                    return true;
                } else {
                    //System.out.println("cannot route internet");
                    return false;
                }

            }
        } else {
            System.out.println("NETWORK IS not CONNECTED");
            return false;
        }

        return false;
    }


}
