package com.leashtime.sitterapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import im.delight.android.webview.AdvancedWebView;

public class WebViewActivity extends AppCompatActivity implements AdvancedWebView.Listener {
    private AdvancedWebView advancedWebView;
    String documentMimeType;
    String mCurrentDocPath;
    String theMimeType;

    private class getFileNameAsync extends AsyncTask<String, Void, String> {
        String guessMimeType;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... params) {
            URL url;
            String filename = null;
            HttpURLConnection conn = null;
            try {
                url = new URL(params[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoOutput(true);
                conn.connect();
                conn.setInstanceFollowRedirects(false);
                try {
                    for(int i = 0; i < 100; i++)
                    {
                        String stringURL = conn.getHeaderField("Location");
                        if (stringURL != null) {
                            url = new URL(stringURL);
                            conn = (HttpURLConnection) url.openConnection();
                            conn.connect();
                            conn.setInstanceFollowRedirects(false);
                        } else {
                            i = 100;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String depo = conn.getHeaderField("Content-Disposition");
                if (depo != null) {
                    String depoSplit[] = depo.split(";");
                    int size = depoSplit.length;
                    for(int i = 0; i < size; i++)
                    {
                        if(depoSplit[i].startsWith("filename=") || depoSplit[i].startsWith(" filename="))
                        {
                            filename = depoSplit[i].replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1").trim();
                            guessMimeType = URLConnection.guessContentTypeFromName(filename);
                            System.out.println(guessMimeType);
                            i = size;
                        }
                    }
                }
                String SDCard;
                SDCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"";
                File file = new File(SDCard,filename);
                System.out.println("Writing to file system: " + file);
                FileOutputStream fileOutput = null;
                fileOutput = new FileOutputStream(file,true);
                InputStream inputStream = null;
                inputStream = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int count;
                long total = 0;
                while ((count = inputStream.read(buffer)) != -1) {
                    total += count;
                    fileOutput.write(buffer,0, count);
                }
                fileOutput.flush();
                fileOutput.close();
                inputStream.close();

            } catch (MalformedURLException e){e.printStackTrace();
            } catch (ProtocolException e){e.printStackTrace();
            } catch (FileNotFoundException e){e.printStackTrace();
            } catch (IOException e){e.printStackTrace();
            } catch (Exception e){e.printStackTrace();
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
            return filename;
        }
        @Override
        protected void onPostExecute(String filename) {
            super.onPostExecute(filename);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

            File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),filename);
            Uri docAttachUri = FileProvider.getUriForFile(WebViewActivity.this,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    storageDir);

            try {
                intent.setDataAndTypeAndNormalize(docAttachUri,guessMimeType);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                TextView textView = findViewById(R.id.fileOpenError);
                textView.setTextColor(Color.RED);
                textView.setTextSize(18);
                textView.setText("COULD NOT FIND APPLICATION TO OPEN ATTACHMENT, PLEASE INSTALL APPROPRIATE APPLICATION AND TRY TO OPEN AGAIN");
            }
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        advancedWebView = (AdvancedWebView) findViewById(R.id.advancedWebViewDoc);
        advancedWebView.setListener(this,this);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Bundle getData = getIntent().getExtras();
        String docRef = getData.getString("url");

        advancedWebView.getSettings().setJavaScriptEnabled(true);
        advancedWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        advancedWebView.getSettings().setAllowFileAccess(true);
        advancedWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url,
                                        String userAgent,
                                        String contentDisposition,
                                        String mimetype,
                                        long contentLength) {

                theMimeType = mimetype;
                System.out.println("The mime type is: " + theMimeType);
                if (mimetype.equals("application/pdf")){
                    documentMimeType = ".pdf";
                } else if (mimetype.equals("image/jpeg")) {
                    documentMimeType = ".jpg";
                } else if (mimetype.equals("text/plain")) {
                    documentMimeType = ".txt";
                } else if (mimetype.equals("image/png")) {
                    documentMimeType = ".png";
                } else if (mimetype.equals("application/vnd.ms-excel")){
                    documentMimeType = ".xls";
                } else if (mimetype.equals("application/vnd.ms-office")) {
                    documentMimeType = ".doc";
                } else {

                }
                System.out.println("Mime type: "  + mimetype);
                if(Build.VERSION.SDK_INT >=23){
                    Context nContext = WebViewActivity.this.getApplicationContext();
                    if(nContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        WebViewActivity.this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        return;
                    }
                }
                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                String filetype = mimeTypeMap.getMimeTypeFromExtension(".doc");
                System.out.println("Mime type map: " +  filetype);
                filetype = mimeTypeMap.getMimeTypeFromExtension(".xls");
                System.out.println("Mime type map: " +  filetype);

                AsyncTask<String, Void, String> asyncTask = new getFileNameAsync();
                asyncTask.execute(url);
            }
        });
        advancedWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                advancedWebView.loadUrl(docRef);
                return true;
            }
        });
        advancedWebView.loadUrl(docRef);
        /*
            advancedWebView.loadUrl("http://drive.google.com/viewerng/viewer?embedded=true&url=" + docRef);
        } */

        Button dismissButton = findViewById(R.id.exitButton);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    private File createAttachFile(String mimeType) throws IOException{
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"");
        File attachment = File.createTempFile("LTattach",mimeType,storageDir);
        mCurrentDocPath = "file:/" + attachment;
        return attachment;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        advancedWebView.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        advancedWebView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        advancedWebView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        advancedWebView.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onBackPressed() {
        if (!advancedWebView.onBackPressed()) { return; }
        super.onBackPressed();
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) { }

    @Override
    public void onPageFinished(String url) { }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) { }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) { }

    @Override
    public void onExternalPageRequest(String url) { }

}
