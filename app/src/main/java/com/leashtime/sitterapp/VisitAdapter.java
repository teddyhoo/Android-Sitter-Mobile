package com.leashtime.sitterapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bcgdv.asia.lib.ticktock.TickTockView;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.danimahardhika.cafebar.CafeBar;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class VisitAdapter extends RecyclerView. Adapter<VisitAdapter.ViewHolder> {
    private ArrayList<VisitDetail> visitData;
    private final ViewBinderHelper binderHelper = new ViewBinderHelper();
    private Context mContext;
    private LayoutInflater mInflater;
    private Drawable envelope;
    private Drawable checkMark;
    private Drawable fileFolder;
    private Drawable visitReportIcon;
    private Drawable takePhotoIcon;
    private Drawable arriveIcon;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd",new Locale("US"));
    private final SimpleDateFormat coordinateDateFormat =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final int ONE_DAY = 1000 * 60 * 60 * 24;
    private static final int ONE_HOUR = ONE_DAY / 24;
    private static final int ONE_MINUTE = ONE_HOUR / 60;
    private static final int ONE_SECOND = ONE_MINUTE / 60;


    public VisitAdapter(Context c, ArrayList<VisitDetail> theVisitData) {
        mContext = c;
        mInflater = LayoutInflater.from(mContext);
        visitData = theVisitData;
        envelope = ContextCompat.getDrawable(mContext,R.drawable.envelope128x128_3x);
        checkMark = ContextCompat.getDrawable(mContext, R.drawable.check_mark_green_3x);
        visitReportIcon = ContextCompat.getDrawable(mContext, R.drawable.flag_handwrite_3x);
        fileFolder =  ContextCompat.getDrawable(mContext,R.drawable.file_folder_line_3x);
        takePhotoIcon = ContextCompat.getDrawable(mContext, R.drawable.camera128x128_3x);
        arriveIcon = ContextCompat.getDrawable(mContext, R.drawable.arrive_pink_button_3x);

        binderHelper.setOpenOnlyOne(true);

    }

    @Override
    public VisitAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_visit_view, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }
    @Override
    public void onBindViewHolder(VisitAdapter.ViewHolder vH, int position) {
        final ViewHolder holder = vH;
        if (position >= 0 && position < visitData.size()) {
            VisitDetail visitSelected = visitData.get(position);
            String uniqueVisitID  = visitSelected.appointmentid;
            binderHelper.bind(holder.swipeLayout,uniqueVisitID);
            holder.bind(uniqueVisitID);
        }
    }
    @Override
    public int getItemCount() {
        return visitData.size();
    }
    private static class MySnackbar extends Thread {

        WeakReference<VisitAdapter> activity;
        WeakReference <CafeBar> snackMain;
        WeakReference<View>snackView;
        WeakReference<VisitDetail>snackVisitTag;

        public MySnackbar(VisitAdapter activity, CafeBar snackFrom) {
            this.activity = new WeakReference<>(activity);
            this.snackMain = new WeakReference<>(snackFrom);
            this.snackView = new WeakReference<>(snackFrom.getCafeBarView());
            this.snackVisitTag = new WeakReference<>((VisitDetail)snackFrom.getCafeBarView().getTag());
        }

        @Override
        public void run() {
            CafeBar myFinalSnack = this.snackMain.get();
            if (null != myFinalSnack) {
                myFinalSnack.show();
            }
        }
        public void dismiss() {
            VisitAdapter myActivity = this.activity.get();
            CafeBar dSnackBar = this.snackMain.get();
            dSnackBar.dismiss();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ConstraintLayout row;
        private final ArrayList<CafeBar> snackBarItems = new ArrayList<>();
        private ClientDetail currentClient;
        VisitsAndTracking sVisitsAndTracking;

        public CafeBar mgrNoteCafeBar;
        public SwipeRevealLayout swipeLayout;

        private View frontLayout;
        private View arriveLayout;
        private View currentRow;
        public ImageView arriveImage;
        public ImageView petPhotoImage;
        public ImageView keyIconButton;
        public ImageButton visitNoteManagerButton;
        public ImageButton writeVisitReportButton;

        public TextView petNames;
        public TextView clientName;
        public TextView serviceName;
        public TextView timeWindow;
        public TextView keyIDField;
        private TextView statusText;
        public TextView flagText;

        public TickTockView timerCounter;

        private final SimpleDateFormat nextPrevDateFormat = new SimpleDateFormat("M/d/yyyy",new Locale("US"));
        private final OkHttpClient client = new OkHttpClient();

        public ViewHolder (View itemRow) {

            super(itemRow);
            sVisitsAndTracking = VisitsAndTracking.getInstance();
            currentRow = itemRow;

            SwipeRevealLayout.SimpleSwipeListener listener = new SwipeRevealLayout.SimpleSwipeListener() {
                @Override
                public void onClosed(SwipeRevealLayout view) {
                    System.out.println("Swipe layout ON close method called");
                    VisitDetail swipeTag = (VisitDetail)view.getTag();
                    configureRowView(swipeTag);
                    bind(swipeTag.appointmentid);
                }
                @Override
                public void onOpened(SwipeRevealLayout view) {
                }
                @Override
                public void onSlide(SwipeRevealLayout view, float slideOffset) {
                }
            };
            swipeLayout = itemView.findViewById(R.id.swipe_layout);
            swipeLayout.setSwipeListener(listener);
            frontLayout =itemRow.findViewById(R.id.front_layout);
            arriveLayout = itemRow.findViewById(R.id.arrive_layout);
            statusText = itemRow.findViewById(R.id.statusText);
            petNames = itemRow.findViewById(R.id.petName);
            clientName = itemRow.findViewById(R.id.clientName);
            serviceName = itemRow.findViewById(R.id.serviceName);
            timeWindow = itemRow.findViewById(R.id.visitTimeWindowText);
            keyIDField = itemRow.findViewById(R.id.keyID);
            keyIconButton = itemRow.findViewById(R.id.keyIcon);
            visitNoteManagerButton = itemRow.findViewById(R.id.visitNoteMgr);
            writeVisitReportButton = itemRow.findViewById(R.id.visitReportButton);
            petPhotoImage = itemRow.findViewById(R.id.listViewPetPhoto);
            arriveImage = itemRow.findViewById(R.id.arriveImg);
            timerCounter = itemRow.findViewById(R.id.countdown);

        }

        public void bind(final String uniqueVisitID) {
            for (VisitDetail visitDetail : visitData) {
                if (visitDetail.appointmentid.equals(uniqueVisitID)) {
                    currentRow.setTag(visitDetail);
                    for (ClientDetail clientDetail : sVisitsAndTracking.clientData) {
                        if (visitDetail.clientptr.equals(clientDetail.clientID)) {
                            currentClient = clientDetail;
                        }
                    }

                    frontLayout.setTag(visitDetail);
                    arriveLayout.setTag(visitDetail);
                    statusText.setTag(visitDetail);
                    petNames.setText(visitDetail.petNames);
                    serviceName.setText(visitDetail.service);
                    timeWindow.setText(visitDetail.timeofday);
                    if(sVisitsAndTracking.showClientName) {
                        clientName.setText(visitDetail.clientname);
                        clientName.setVisibility(View.VISIBLE);
                    } else {
                        clientName.setVisibility(View.INVISIBLE);
                    }

                    if(!sVisitsAndTracking.showKey)  {
                        keyIconButton.setVisibility(View.INVISIBLE);
                        keyIDField.setVisibility(View.INVISIBLE);
                    } else {
                        keyIconButton.setVisibility(View.VISIBLE);
                        keyIDField.setVisibility(View.VISIBLE);
                        if (visitDetail.noKeyRequired.equals("1")) {
                            if(visitDetail.noKeyRequired != null) {
                                keyIDField.setText("NO KEY REQUIRED");
                                keyIDField.setTextColor(ContextCompat.getColor(mContext,R.color.grey_200));
                                keyIconButton.setAlpha(0.24f);
                            }
                        } else if(visitDetail.useKeyDescriptionInstead.equals("Yes")) {
                            keyIDField.setText(visitDetail.keyDescriptionText);
                        } else {
                            String keyIDString = visitDetail.keyID;
                            if (visitDetail.hasKey.equals("Yes")) {
                                keyIDField.setText(keyIDString);
                                keyIDField.setTextColor(ContextCompat.getColor(mContext, R.color.grey_200));
                            } else {
                                keyIDField.setText("NEED KEY:" + keyIDString);
                                keyIDField.setTextColor(ContextCompat.getColor(mContext, R.color.grey_200));
                            }
                        }
                    }

                    if(sVisitsAndTracking.showPetPic) {
                        addPetPics(visitDetail,currentRow);
                    }
                    configureRowView(visitDetail);
                    createActionButtons(visitDetail);
                }
            }
        }
        public void configureRowView(VisitDetail visit) {

            if (visit.status.equals("arrived")) {
                currentRow.setBackgroundColor(ContextCompat.getColor(mContext, R.color.green_A400));
                frontLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.green_A400));
                frontLayout.setBackgroundColor(Color.BLUE);
                statusText.setText("COMPLETE");
                arriveLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.arrive_2_green));

                arriveImage.setVisibility(View.INVISIBLE);
                arriveImage.setImageDrawable(null);
                arriveImage.setBackground(null);
                arriveImage.setImageBitmap(null);
                arriveImage.setImageDrawable(arriveIcon);
                arriveImage.setVisibility(View.VISIBLE);

                petNames.setTextColor(Color.WHITE);
                serviceName.setTextColor(Color.WHITE);
                timeWindow.setTextColor(Color.WHITE);
                clientName.setTextColor(Color.WHITE);
                if(sVisitsAndTracking.showClientName) {
                    clientName.setText(visit.clientname);
                    clientName.setVisibility(View.VISIBLE);
                } else {
                    clientName.setVisibility(View.INVISIBLE);
                }
                writeVisitReportButton.setImageDrawable(visitReportIcon);
                String cleanArrive = trimTime(visit.arrived);
                timeWindow.setText(cleanArrive);

                if (sVisitsAndTracking.showVisitTimer) {
                    timerCounter.setVisibility(View.VISIBLE);
                    setupTimerView();
                }  else {
                    timerCounter.setVisibility(View.INVISIBLE);
                }
                if(sVisitsAndTracking.showVisitTimer) {
                    Calendar c2= Calendar.getInstance();
                    c2.add(Calendar.HOUR, 1);
                    c2.set(Calendar.MINUTE, 0);
                    c2.set(Calendar.SECOND, 0);
                    c2.set(Calendar.MILLISECOND, 0);
                    if (timerCounter != null) {
                        timerCounter.setVisibility(View.VISIBLE);
                        timerCounter.start(c2);
                    }
                }

                if (null != visit.dateTimeVisitReportSubmit) {
                    keyIDField.setText("SENT: " + prettyDateOnlyTime(visit.dateTimeVisitReportSubmit));
                    writeVisitReportButton.setBackground(null);
                    writeVisitReportButton.setVisibility(View.INVISIBLE);
                    writeVisitReportButton.setImageDrawable(null);
                    writeVisitReportButton.setBackground(null);
                    writeVisitReportButton.setImageDrawable(envelope);
                    writeVisitReportButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    writeVisitReportButton.setVisibility(View.VISIBLE);
                }
            } else if (visit.status.equals("completed")) {
                currentRow.setBackgroundColor(ContextCompat.getColor(mContext, R.color.complete_green));
                frontLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.complete_green));
                petNames.setTextColor(Color.WHITE);
                serviceName.setTextColor(Color.WHITE);
                clientName.setTextColor(Color.WHITE);
                if(sVisitsAndTracking.showClientName) {
                    clientName.setText(visit.clientname);
                    clientName.setVisibility(View.VISIBLE);
                } else {
                    clientName.setVisibility(View.INVISIBLE);
                }
                CharSequence startFinish = new String(' ' + prettyDateOnlyTime(visit.arrived) + " to " + prettyDateOnlyTime(visit.completed));
                timeWindow.setTextColor(Color.YELLOW);
                timeWindow.setText(startFinish);
                keyIDField.setVisibility(View.INVISIBLE);
                keyIconButton.setVisibility(View.INVISIBLE);
                writeVisitReportButton.setImageDrawable(visitReportIcon);
                arriveImage.setVisibility(View.INVISIBLE);
                arriveImage.setImageDrawable(null);
                arriveImage.setBackground(null);
                arriveImage.setImageBitmap(null);
                arriveImage.setImageDrawable(checkMark);
                arriveImage.setVisibility(View.VISIBLE);
                swipeLayout.setLockDrag(TRUE);

                if (null != visit.dateTimeVisitReportSubmit) {
                    System.out.println("Visit ID: " + visit.appointmentid + ": " + visit.dateTimeVisitReportSubmit);
                    keyIDField.setText("SENT: " + prettyDateOnlyTime(visit.dateTimeVisitReportSubmit));
                    writeVisitReportButton.setBackground(null);
                    writeVisitReportButton.setVisibility(View.INVISIBLE);
                    writeVisitReportButton.setImageDrawable(null);
                    writeVisitReportButton.setImageDrawable(envelope);
                    writeVisitReportButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    writeVisitReportButton.setVisibility(View.VISIBLE);
                }

                if (timerCounter != null) {
                    timerCounter.setVisibility(View.INVISIBLE);
                }

            } else if (visit.status.equals("canceled")) {
                currentRow.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red_900));
                frontLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red_900));
                arriveImage.setVisibility(View.INVISIBLE);
                keyIconButton.setVisibility(View.INVISIBLE);
                visitNoteManagerButton.setVisibility(View.INVISIBLE);
                writeVisitReportButton.setVisibility(View.INVISIBLE);
                keyIDField.setVisibility(View.INVISIBLE);
                if (timerCounter != null) {
                    timerCounter.setVisibility(View.INVISIBLE);
                }
                if(sVisitsAndTracking.showClientName) {
                    clientName.setText(visit.clientname);
                    clientName.setVisibility(View.VISIBLE);
                } else {
                    clientName.setVisibility(View.INVISIBLE);
                }
            } else if (visit.status.equals("future")) {

                currentRow.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_blue_200));
                frontLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_blue_200));
                arriveLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.cardview_light_background));
                petNames.setTextColor(Color.BLACK);
                serviceName.setTextColor(Color.BLACK);
                timeWindow.setTextColor(Color.BLACK);
                arriveImage.setVisibility(View.INVISIBLE);
                if (timerCounter != null) {
                    timerCounter.setVisibility(View.INVISIBLE);
                }
                writeVisitReportButton.setBackground(null);
                writeVisitReportButton.setVisibility(View.INVISIBLE);
                writeVisitReportButton.setImageDrawable(null);
                if (null != visit.dateTimeVisitReportSubmit) {
                    writeVisitReportButton.setImageDrawable(envelope);
                    writeVisitReportButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    writeVisitReportButton.setVisibility(View.VISIBLE);
                } else {
                    writeVisitReportButton.setImageDrawable(visitReportIcon);
                    writeVisitReportButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    writeVisitReportButton.setVisibility(View.VISIBLE);
                }

                } else if (visit.status.equals("late")) {

                currentRow.setBackgroundColor(ContextCompat.getColor(mContext, R.color.yellow_200));
                frontLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.yellow_200));
                arriveLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.cardview_light_background));
                petNames.setTextColor(Color.BLACK);
                serviceName.setTextColor(Color.BLACK);
                timeWindow.setTextColor(Color.BLACK);
                keyIDField.setTextColor(Color.BLACK);
                clientName.setTextColor(Color.BLACK);
                if(sVisitsAndTracking.showClientName) {
                    clientName.setText(visit.clientname);
                    clientName.setVisibility(View.VISIBLE);
                } else {
                    clientName.setVisibility(View.INVISIBLE);
                }
                arriveImage.setVisibility(View.INVISIBLE);
                if (timerCounter != null) {
                    timerCounter.setVisibility(View.INVISIBLE);
                }
                writeVisitReportButton.setBackground(null);
                writeVisitReportButton.setVisibility(View.INVISIBLE);
                writeVisitReportButton.setImageDrawable(null);
                if (null != visit.dateTimeVisitReportSubmit) {
                    writeVisitReportButton.setImageDrawable(envelope);
                    writeVisitReportButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    writeVisitReportButton.setVisibility(View.VISIBLE);
                } else {
                    writeVisitReportButton.setImageDrawable(visitReportIcon);
                    writeVisitReportButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    writeVisitReportButton.setVisibility(View.VISIBLE);
                }
            }
        }
        public void setupTimerView() {
            if (timerCounter != null) {
                timerCounter.setOnTickListener(new TickTockView.OnTickListener() {
                    Date currentTime = new Date();
                    @Override
                    public String getText(long timeRemaining) {
                        SimpleDateFormat hourMinFormat = new SimpleDateFormat("mm:ss", Locale.US);
                        currentTime.setTime(System.currentTimeMillis());
                        String hourMinString = "";
                        Date compareDate = null;
                        Date rightNow = new Date();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

                        for (VisitDetail visitDetail : sVisitsAndTracking.visitData) {
                            if (visitDetail.appointmentid.equals(sVisitsAndTracking.onWhichVisitID)) {
                                try {
                                    compareDate = simpleDateFormat.parse(visitDetail.arrived);
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(rightNow);
                                    long t1 = cal.getTimeInMillis();
                                    cal.setTime(compareDate);
                                    long diff = Math.abs(cal.getTimeInMillis() - t1);
                                    long d = diff / ONE_DAY;
                                    diff %= ONE_DAY;
                                    long hh = diff / ONE_HOUR;
                                    diff %= ONE_HOUR;
                                    long mm = diff / ONE_MINUTE;
                                    diff %= ONE_MINUTE;
                                    long ss = diff / ONE_SECOND;
                                    String timeText = "00:00";
                                    if (hh > 0) {
                                        timeText = hh + ":" + mm + ':' + ss;
                                    } else if (hh == 0) {
                                        timeText = mm + ":" + ss;
                                    }
                                    return timeText;
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        return "00:00";
                    }
                });
            }
        }
        public void createActionButtons(VisitDetail visit) {

            final VisitDetail visitDetail = visit;
            Bundle basket = new Bundle();
            basket.putString("detailVisit",visit.appointmentid);
            final Intent visitDetailIntent = new Intent(mContext,ViewVisitDetail.class);
            visitDetailIntent.putExtras(basket);
            frontLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(visitDetailIntent);
                }
            });
            petNames.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(visitDetailIntent);
                }
            });
            keyIDField.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(visitDetailIntent);
                }
            });
            clientName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(visitDetailIntent);
                }
            });
            serviceName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mContext.startActivity(visitDetailIntent);
                    }
            });
            timeWindow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(visitDetailIntent);
                }
            });
            statusText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VisitDetail visit = (VisitDetail)v.getTag();
                    System.out.println("Should mark arrive for visit: " + visit.appointmentid);
                    if (visit.status.equals("future") || visit.status.equals("late")) {
                        markVisitArrive(visit);
                        swipeLayout.close(TRUE);
                    } else if (visit.status.equals("arrived")) {
                        markVisitComplete(visit);
                        swipeLayout.close(TRUE);
                    }
                }
            });
            petPhotoImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent startIntent = new Intent(mContext,PhotoActivity.class);
                    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startIntent.putExtra("visitID",visit.appointmentid);
                    mContext.startActivity(startIntent);
                }
            });
            visitNoteManagerButton.setTag(visitDetail);

            if (visitDetail.note != "NONE" || currentClient.errataDoc.size() > 0) {
                visitNoteManagerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        VisitDetail tagVisitDetail = (VisitDetail)v.getTag();
                        buildVisitNoteView(tagVisitDetail);
                    }
                });
            } else {
                visitNoteManagerButton.setVisibility(View.INVISIBLE);
            }

            writeVisitReportButton.setTag(visitDetail);
            writeVisitReportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VisitDetail visit = (VisitDetail)v.getTag();
                    Bundle basket = new Bundle();
                    basket.putString("visit",visit.appointmentid);
                    Intent visitReportIntent = new Intent(mContext, VisitReport.class);
                    visitReportIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    visitReportIntent.putExtras(basket);
                    System.out.println("VISIT REPORT INTENT PREPARED AND LAUNCHING");
                    mContext.startActivity(visitReportIntent);
                }
            });
        }
        private void addPetPics (VisitDetail visit, View rowNum) {

            ImageView petPic = rowNum.findViewById(R.id.listViewPetPhoto);

            int numberImages = visit.petListVisit.size();

            //System.out.println("Number of images from <Visit>.petListVisit: " + numberImages);

            if(visit.petPicFileName != null) {
                File file = new File(visit.petPicFileName);
                petPic.setBackground(null);
                petPic.setVisibility(View.INVISIBLE);
                petPic.setImageDrawable(null);
                petPic.setImageBitmap(null);
                Uri uri = Uri.fromFile(file);
                GlideApp.with(mContext.getApplicationContext()).load(uri).into(petPic);
                petPic.setVisibility(View.VISIBLE);

            } else if (visit.petPicFileName == null) {
                if (numberImages > 0) {
                    petPic.setBackground(null);
                    petPic.setVisibility(View.INVISIBLE);
                    petPic.setImageDrawable(null);
                    petPic.setImageBitmap(null);
                    petPic.setVisibility(View.VISIBLE);
                    for (int i = 0; i < numberImages; i++) {
                        String url = "https://leashtime.com/pet-photo-sessionless.php?id=" +
                                visit.petListVisit.get(i) + "&loginid=" +
                                sVisitsAndTracking.mPreferences.getString("username","") + "&password=" +
                                sVisitsAndTracking.mPreferences.getString("password","") + "&version=display";
                        if (2 == i) {
                            GlideApp.with(mContext.getApplicationContext()).load(url).into(petPic);
                        }
                        if (1 == i) {
                            GlideApp.with(mContext.getApplicationContext()).load(url).into(petPic);
                        }
                        if (0 == i) {
                            GlideApp.with(mContext.getApplicationContext()).load(url).into(petPic);
                        }
                    }
                } else {
                    petPic.setVisibility(View.INVISIBLE);
                    petPic.setImageDrawable(null);
                    petPic.setImageBitmap(null);
                    petPic.setImageDrawable(takePhotoIcon);
                    petPic.setVisibility(View.VISIBLE);
                }

            }
        }
        public void buildVisitNoteView(VisitDetail visit) {
            CafeBar.Builder builder = new CafeBar.Builder(mContext);
            builder.customView(R.layout.visit_manager_note, true);
            builder.autoDismiss(false);
            CafeBar managerNoteCafeBar = builder.build();
            mgrNoteCafeBar = managerNoteCafeBar;
            View v = managerNoteCafeBar.getCafeBarView();

            MySnackbar mySnack = new MySnackbar(VisitAdapter.this, managerNoteCafeBar);
            snackBarItems.add(managerNoteCafeBar);
            ImageButton dismissDetail = v.findViewById(R.id.mgrNoteExit);
            dismissDetail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mySnack.dismiss();
                }
            });
            LinearLayout linearLayout = v.findViewById(R.id.managerScrollView);
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            TextView clientPetNameTxt = new TextView(mContext.getApplicationContext());
            TextView visitTimeText = new TextView(mContext.getApplicationContext());
            TextView managerNote = new TextView(mContext.getApplicationContext());

            clientPetNameTxt.setTextSize(16);
            clientPetNameTxt.setTextColor(Color.BLACK);
            clientPetNameTxt.setText(visit.clientname + ", " + visit.petNames);
            clientPetNameTxt.setPadding(5,5,5,5);

            visitTimeText.setTextSize(16);
            visitTimeText.setTextColor(Color.BLACK);
            visitTimeText.setText(visit.starttime + " - " + visit.endtime);
            visitTimeText.setPadding(5,5,5,5);

            managerNote.setTextSize(16);
            managerNote.setTextColor(Color.BLACK);
            managerNote.setText(visit.note);
            managerNote.setPadding(5,5,5,5);

            linearLayout.addView(clientPetNameTxt);
            linearLayout.addView(visitTimeText);
            linearLayout.addView(managerNote);

            int numAttach = currentClient.errataDoc.size();

            for (int i = 0; i < numAttach; i++) {
                final Map<String, String> errataDic = currentClient.errataDoc.get(i);
                String fieldLabel = errataDic.get("fieldlabel");
                LinearLayout docAttachLay = new LinearLayout(mContext.getApplicationContext());
                docAttachLay.setOrientation(LinearLayout.HORIZONTAL);
                ImageButton docButton = new ImageButton(mContext.getApplicationContext());
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                docButton.setLayoutParams(params);
                docButton.requestLayout();
                int height = (int)sVisitsAndTracking.convertDpToPixel(32);
                int width = (int)sVisitsAndTracking.convertDpToPixel(32);
                docButton.getLayoutParams().height = height;
                docButton.getLayoutParams().width = width;
                docButton.setBackground(fileFolder);
                docButton.setScaleType(ImageView.ScaleType.FIT_XY);
                docButton.setPadding(10,10,10,10);
                docButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle basket = new Bundle();
                        basket.putString("url",errataDic.get("url"));
                        basket.putString("label",errataDic.get("label"));
                        basket.putString("errataIndex",errataDic.get("errataIndex"));
                        basket.putString("mimetype",errataDic.get("mimetype"));
                        basket.putString("fieldLabel",errataDic.get("fieldlabel"));
                        Intent webViewIntent = new Intent(mContext, WebViewActivity.class);
                        webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        webViewIntent.putExtras(basket);
                        mContext.startActivity(webViewIntent);
                    }
                });
                TextView docText = new TextView(mContext.getApplicationContext());
                docText.setText(fieldLabel);
                docText.setTextColor(Color.BLACK);
                docText.setTextSize(18);
                docText.setPadding(10,10,10,10);
                docAttachLay.addView(docButton);
                docAttachLay.addView(docText);
                linearLayout.addView(docAttachLay);
            }

            LinearLayout flagLay  = new LinearLayout(mContext.getApplicationContext());
            flagLay.setOrientation(LinearLayout.HORIZONTAL);
            int numFlags = currentClient.clientFlags.size();
            flagText = new TextView(mContext);
            flagText.setPadding(20,20,20,20);
            flagText.setTextColor(Color.MAGENTA);
            flagText.setTextSize(16);

            for (int i = 0; i < numFlags; i++) {
                FlagItem flagItem = currentClient.clientFlags.get(i);
                String imgID = flagItem.flagImgSrc;
                String url = "https://leashtime.com/art/" + imgID + ".jpg";
                final String flagNoteText = flagItem.flagNote;
                final String flagTitleText = flagItem.flagTitle;
                ImageView flagImg = new ImageView(mContext.getApplicationContext());

                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                flagImg.setLayoutParams(params);
                flagImg.requestLayout();
                int height = (int)sVisitsAndTracking.convertDpToPixel(32);
                int width = (int)sVisitsAndTracking.convertDpToPixel(32);
                flagImg.getLayoutParams().height = height;
                flagImg.getLayoutParams().width = width;
                flagImg.setScaleType(ImageView.ScaleType.FIT_XY);
                flagImg.setPadding(10,10,10,10);
                flagImg.setBackgroundColor(Color.TRANSPARENT);

                Context currentContext = mContext.getApplicationContext();
                GlideApp.with(currentContext)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .priority(Priority.IMMEDIATE)
                        .skipMemoryCache(false)
                        .into(flagImg);
                flagLay.addView(flagImg);
                flagImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String flagNoteConsolidate = flagTitleText + ": " + flagNoteText;
                        flagText.setText(flagNoteConsolidate);
                    }
                });
                String flagNoteConsolidate = flagTitleText + ": " + flagNoteText;
                flagText.setText(flagNoteConsolidate);
            }
            linearLayout.addView(flagLay);
            linearLayout.addView(flagText);
            mySnack.start();
        }
        private  String trimTime(String timeValStr) {
            SimpleDateFormat fullDate = new SimpleDateFormat("yyyy-MM-DD hh:mm:ss",Locale.US);
            SimpleDateFormat visitTime = new SimpleDateFormat("hh:mm",Locale.US);
            String newString = "";
            try {
                Date date = fullDate.parse(timeValStr);
                newString = visitTime.format(date);
            } catch (ParseException e2) {
                e2.printStackTrace();
            }
            return newString;
        }
        private  String    getDate() {
            Date transmitDate = new Date();
            SimpleDateFormat rightNowFormatTransmit = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            return rightNowFormatTransmit.format(transmitDate);
        }
        private Boolean checkIfCanMarkArrive(VisitDetail visit) {
            boolean canMarkMoreThanArrive = FALSE;
            if(sVisitsAndTracking.isMultiVisitArrive) {
                canMarkMoreThanArrive = TRUE;
            } else {
                for(VisitDetail multiVisit : sVisitsAndTracking.visitData) {
                    if(multiVisit.status.equals("arrived")) {
                        Toast.makeText(MainApplication.getAppContext(), "CANNOT MARK ARRIVE. Only one visit may be marked arrive at a time.", Toast.LENGTH_SHORT).show();
                        return FALSE;
                    }
                }
            }

            Date today = new Date();
            Date visitDate;
            try {
                visitDate = nextPrevDateFormat.parse(visit.shortNaturalDate);
                int comparison = today.compareTo(visitDate);

                if (comparison < 0 ){
                    canMarkMoreThanArrive = FALSE;
                    Toast.makeText(MainApplication.getAppContext(), "CANNOT MARK ARRIVE. Visit is in the future.", Toast.LENGTH_SHORT).show();
                    return FALSE;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if(visit.status.equals("canceled")) {
                Toast.makeText(mContext, "CANNOT MARK ARRIVE. Visit is canceled.", Toast.LENGTH_SHORT).show();
                return FALSE;
            } else if (visit.status.equals("completed")) {
                Toast.makeText(mContext, "CANNOT MARK ARRIVE. Visit is completed.", Toast.LENGTH_SHORT).show();
                return FALSE;
            }  else if (visit.appointmentid.equals(sVisitsAndTracking.onWhichVisitID)) {
                Toast.makeText(mContext,"VISIT IS ALREADY MARKED ARRIVED.", Toast.LENGTH_SHORT).show();
                return FALSE;
            } else if  (visit.status.equals("future") || visit.status.equals("late")) {
                Toast.makeText(mContext, "successfully MARKED ARRIVE", Toast.LENGTH_SHORT).show();

                sVisitsAndTracking.onWhichVisitID = visit.appointmentid;
                if(sVisitsAndTracking.isMultiVisitArrive) {
                    sVisitsAndTracking.onWhichVisits.add(visit.appointmentid);
                }
                return TRUE;
            } else if (!canMarkMoreThanArrive) {
                return FALSE;
            }
            return FALSE;
        }
        private void        markVisitArrive(VisitDetail visitDetail) {
            String dateTimeStringHTTP = getDate();
            if (checkIfCanMarkArrive(visitDetail)) {
                visitDetail.status = "arrived";
                visitDetail.arrived = dateTimeStringHTTP;
                String lastValidLat;
                String lastValidLon;
                if (sVisitsAndTracking.lastValidLocation != null) {
                    lastValidLat = String.valueOf(sVisitsAndTracking.lastValidLocation.getLatitude());
                    lastValidLon = String.valueOf(sVisitsAndTracking.lastValidLocation.getLongitude());
                } else {
                    lastValidLat = "0.00000";
                    lastValidLon = "0.00000";
                }
                visitDetail.coordinateLatitudeMarkArrive = lastValidLat;
                visitDetail.coordinateLongitudeMarkArrive = lastValidLon;

                sVisitsAndTracking.onWhichVisitID = visitDetail.appointmentid;
                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.leashtime.com/native-visit-action.php")
                        .newBuilder();
                urlBuilder.addQueryParameter("loginid", sVisitsAndTracking.mPreferences.getString("username",""));
                urlBuilder.addQueryParameter("password", sVisitsAndTracking.mPreferences.getString("password",""));
                urlBuilder.addQueryParameter("datetime", dateTimeStringHTTP);
                urlBuilder.addQueryParameter("coords", "{\"appointmentptr\" : \"" + visitDetail.appointmentid +
                        "\", \"lat\" : \"" + lastValidLat + "\", " +
                        "\"lon\" : \"" + lastValidLon + "\"," +
                        " \"event\" : \"arrived\", " +
                        "\"accuracy\" : \"5.0\"}");

                String url = urlBuilder.toString();
                VisitDetail visitOn = visitDetail;

                if(sVisitsAndTracking.USER_AGENT == null) {
                    sVisitsAndTracking.USER_AGENT = "LeashTime Android / null user agent";
                }
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", sVisitsAndTracking.USER_AGENT)
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    Response theResponse;
                    @Override
                    public void onFailure(Call call, IOException e) {
                        visitOn.currentArriveStatus = "FAIL";
                        sVisitsAndTracking.writeVisitDataToFile(visitOn);
                    }
                    @Override
                    public void onResponse(Call call, Response response) {
                        visitOn.currentArriveStatus = "SUCCCESS";
                        sVisitsAndTracking.writeVisitDataToFile(visitOn);
                        theResponse = response;
                        theResponse.close();
                    }
                });
            }
        }
        private void  markVisitComplete(VisitDetail visit) {
            String dateTimeStringHTTP = getDate();

            if(checkIfCanMarkComplete(visit)) {
                visit.status = "completed";
                visit.completed = dateTimeStringHTTP;
                String lastValidLat;
                String lastValidLon;

                if(sVisitsAndTracking.lastValidLocation != null) {
                    lastValidLat = String.valueOf(sVisitsAndTracking.lastValidLocation.getLatitude());
                    lastValidLon = String.valueOf(sVisitsAndTracking.lastValidLocation.getLongitude());
                }  else  {
                    lastValidLat = "0.00000";
                    lastValidLon = "0.00000";
                }

                visit.coordinateLatitudeMarkComplete = lastValidLat;
                visit.coordinateLongitudeMarkComplete = lastValidLon;

                HttpUrl.Builder urlBuilder = HttpUrl.parse("https://www.leashtime.com/native-visit-action.php")
                        .newBuilder();
                urlBuilder.addQueryParameter("loginid",sVisitsAndTracking.mPreferences.getString("username",""));
                urlBuilder.addQueryParameter("password", sVisitsAndTracking.mPreferences.getString("password",""));
                urlBuilder.addQueryParameter("datetime", dateTimeStringHTTP);
                urlBuilder.addQueryParameter("coords",
                        "{\"appointmentptr\" : \"" + visit.appointmentid +
                                "\", \"lat\" : \"" + lastValidLat +
                                "\", \"lon\" : \"" + lastValidLon +
                                "\", \"event\" : \"completed\", " +
                                "\"accuracy\" : \"5.0\"}");

                String url = urlBuilder.toString();
                if(sVisitsAndTracking.USER_AGENT == null) {
                    sVisitsAndTracking.USER_AGENT = "LeashTime Android / null user agent";
                }

                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent",sVisitsAndTracking.USER_AGENT)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        visit.currentCompleteStatus = "FAIL";
                        sVisitsAndTracking.writeVisitDataToFile(visit);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        visit.currentCompleteStatus = "SUCCESS";
                        sVisitsAndTracking.writeVisitDataToFile(visit);
                    }
                });

                String getStringOpen = "[\n";
                String visitID = visit.appointmentid;
                int locArrayCount = sVisitsAndTracking.sessionLocationArray.size();

                if(locArrayCount > 0) {
                    for (int i = 0; i < locArrayCount; i++) {
                        Location loc = sVisitsAndTracking.sessionLocationArray.get(i);
                        String latString = String.valueOf(loc.getLatitude());
                        String lonString = String.valueOf(loc.getLongitude());
                        String accuracy = String.valueOf(loc.getAccuracy());
                        long coordTime = loc.getTime();
                        Date date = new Date(coordTime);
                        String coordTimeStamp = coordinateDateFormat.format(date);

                        String event = "mv";
                        String heading = "0";
                        String error = "0";

                        getStringOpen += "{\"appointmentptr\" : " + '"' + visitID + "\",\n";
                        getStringOpen += "\"date\" : " + '"' + coordTimeStamp + "\",\n";
                        getStringOpen += "\"lat\" : " + '"' + latString + "\",\n";
                        getStringOpen += "\"lon\" : " + '"' + lonString + "\",\n";
                        getStringOpen += "\"accuracy\" : " + '"' + accuracy + "\",\n";
                        getStringOpen += "\"event\" : " + '"' + event + "\",\n";
                        getStringOpen += "\"heading\" : " + '"' + heading + "\",\n";

                        if (i < locArrayCount - 1) {
                            getStringOpen += "\"error\" : " + '"' + error + "\"\n},\n";

                        } else {
                            getStringOpen += "\"error\" : " + '"' + error + "\"\n}\n]";
                        }
                    }
                }

                if(sVisitsAndTracking.isMultiVisitArrive && sVisitsAndTracking.onWhichVisits.isEmpty()) {
                    sVisitsAndTracking.sessionLocationArray.clear();
                } else if (!sVisitsAndTracking.isMultiVisitArrive) {
                    sVisitsAndTracking.sessionLocationArray.clear();
                }

                RequestBody formBody = new FormBody.Builder()
                        .add("loginid",sVisitsAndTracking.mPreferences.getString("username",""))
                        .add("password",sVisitsAndTracking.mPreferences.getString("password",""))
                        .add("coords",getStringOpen)
                        .build();
                String postCoordURL = "https://leashtime.com/native-sitter-location.php";
                if(sVisitsAndTracking.USER_AGENT == null) {
                    sVisitsAndTracking.USER_AGENT = "LeashTime Android / null user agent";
                }
                Request coordRequest = new Request.Builder()
                        .url(postCoordURL)
                        .header("User-Agent",sVisitsAndTracking.USER_AGENT)
                        .post(formBody)
                        .build();
                client.newCall(coordRequest).enqueue(new Callback() {
                    ResponseBody responseBodyClose;

                    @Override
                    public void onFailure(Call call, IOException e) {
                        sVisitsAndTracking.resendCoordUploadRequest.add(coordRequest);
                        visit.completedLocationStatus = "FAIL";
                        System.out.println(" MARK COMPLETE : FAIL");
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        visit.completedLocationStatus = "SUCCESS";
                        System.out.println(" MARK COMPLETE : SUCCESS");
                        System.out.println(" COMPLETE SUCCESS RESPONSE: "  + response.body());
                        responseBodyClose = response.body();
                        responseBodyClose.close();

                    }
                });
            }
        }

        public boolean isNetworkConnected(Context ctx) {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService (mContext.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnectedOrConnecting()) {
                return true;
            } else {
                return false;
            }
        }
        public boolean   checkNetworkConnection() {
            if (isNetworkConnected(mContext)) {
                    return true;
                } else {
                    return false;
            }
        }
        private Boolean                 checkIfCanMarkComplete(VisitDetail visit) {
            System.out.println("checking if can MARK COMPLETE");
            switch (visit.status) {
                case "canceled":
                    Toast.makeText(MainApplication.getAppContext(), "CANNOT MARK CANCELED VISIT COMPLETE.", Toast.LENGTH_SHORT).show();
                    System.out.println("NO MARK COMPLETE: canceled");

                    return FALSE;
                case "completed":
                    Toast.makeText(MainApplication.getAppContext(), "VISIT IS ALREADY COMPLETED", Toast.LENGTH_SHORT).show();
                    System.out.println("NO MARK COMPLETE: already completed");

                    return FALSE;
                case "future":
                    Toast.makeText(MainApplication.getAppContext(), "THIS VISIT HAS NOT BEEN MARKED ARRIVE YET  ", Toast.LENGTH_SHORT).show();
                    System.out.println("NO MARK COMPLETE: has not been marked arrive yet");

                    return FALSE;
                case "late":
                    Toast.makeText(MainApplication.getAppContext(), "CANNOT MARK VISIT COMPLETE THAT HAS NOT ARRIVED", Toast.LENGTH_SHORT).show();
                    System.out.println("NO MARK COMPLETE: not arrived yet");
                    return FALSE;
                case "arrived":
                    if (sVisitsAndTracking.isMultiVisitArrive) {
                        int lenmultiArrive = sVisitsAndTracking.onWhichVisits.size();
                        if (lenmultiArrive <= 1) {
                            sVisitsAndTracking.onWhichVisits.clear();
                            sVisitsAndTracking.onWhichVisitID = "0000";
                        } else if (lenmultiArrive > 0) {
                            int removeIndex = 10000;
                            for (int i = 0; i < lenmultiArrive; i++) {
                                String visitIDArrive = sVisitsAndTracking.onWhichVisits.get(i);
                                if (visitIDArrive.equals(visit.appointmentid)) {
                                    removeIndex = i;
                                }
                            }
                            if (removeIndex < 10000) {
                                sVisitsAndTracking.onWhichVisits.remove(removeIndex);
                            }
                            if (!sVisitsAndTracking.onWhichVisits.isEmpty()) {
                                sVisitsAndTracking.onWhichVisitID = sVisitsAndTracking.onWhichVisits.get(0);
                            } else if (sVisitsAndTracking.onWhichVisits.isEmpty()) {
                                sVisitsAndTracking.onWhichVisitID = "0000";
                            }
                        }
                    } else {
                        sVisitsAndTracking.onWhichVisitID = "0000";
                    }
                    return TRUE;
            }
            return TRUE;
        }
        private String prettyDateOnlyTime(String dateTimeString) {
            String newString = "";
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(dateTimeString);
                newString = new SimpleDateFormat("h:mm",Locale.US).format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return newString;
        }

    }
}
