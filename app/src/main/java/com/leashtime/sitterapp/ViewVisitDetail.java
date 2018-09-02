package com.leashtime.sitterapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.leashtime.sitterapp.events.DismissDetailVisitViewEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ViewVisitDetail extends AppCompatActivity {

    private VisitsAndTracking sVisitsAndTracking;
    private VisitDetail currentVisit;
    private ClientDetail clientDetails;
    public Context mContext;
    private LinearLayout clientViewScrollView;
    private Drawable docAttachIcon;
    private Drawable checkIcon;
    private Drawable xIcon;
    private Drawable whiteLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Lato-Regular.ttf"); // font from assets: "assets/fonts/Roboto-Regular.ttf
        mContext = this.getApplicationContext();
        setContentView(R.layout.view_visit_details);
        Bundle getData = getIntent().getExtras();
        String visit = getData.getString("detailVisit");
        sVisitsAndTracking = VisitsAndTracking.getInstance();
        clientViewScrollView = findViewById(R.id.clientDetailLinLay);
        docAttachIcon = ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.file_folder_line_3x);
        checkIcon =ContextCompat.getDrawable(this.getApplicationContext(),R.drawable.check_mark_green_3x);
        xIcon = ContextCompat.getDrawable(this.getApplicationContext(),R.drawable.x_mark_red_3x);
        whiteLine = ContextCompat.getDrawable(this.getApplicationContext(),R.drawable.white_line_1px_3x);

        for (VisitDetail visitItem : sVisitsAndTracking.visitData) {
            if (visitItem.appointmentid.equals(visit)) {
                currentVisit = visitItem;
            }
        }

        if(null != currentVisit) {
            for(ClientDetail client : sVisitsAndTracking.clientData) {
                if(currentVisit.clientptr.equals(client.clientID)) {
                    clientDetails = client;
                }
            }
        }
        addPetImages(clientDetails);
        addVisitSummary(clientDetails, currentVisit);
        addClientFlags(clientDetails);
        addDocumentAttach(clientDetails);
        addBasicInfo(clientDetails);
        addHomeInfoData(clientDetails);
        addEmergencyData(clientDetails);
        addVetData(clientDetails);
        addCustomFields(clientDetails);
    }
    private void addClientFlags(ClientDetail clientDetails) {

        int numFlags = clientDetails.clientFlags.size();

        if (numFlags > 0) {
            LinearLayout flagLay  = new LinearLayout(this);
            flagLay.setOrientation(LinearLayout.HORIZONTAL);
            flagLay.setMinimumHeight(32);
            flagLay.setPadding(10,10,10,30);
            final TextView flagText = new TextView(this);
            flagText.setTextColor(Color.YELLOW);
            flagText.setTextSize(16);
            flagText.setText("TAP FLAG ICON TO SEE TITLE AND NOTES");
            flagText.setPadding(10,0,10,10);

            for (int i = 0; i < numFlags; i++) {
                FlagItem flagItem = clientDetails.clientFlags.get(i);
                String imgID = flagItem.flagImgSrc;
                String url = "https://leashtime.com/art/" + imgID + ".jpg";
                final String flagNoteText = flagItem.flagNote;
                final String flagTitleText = flagItem.flagTitle;

                ImageView flagImg = new ImageView(this);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                flagImg.setLayoutParams(params);
                flagImg.requestLayout();
                int height = (int)sVisitsAndTracking.convertDpToPixel(32);
                int width = (int)sVisitsAndTracking.convertDpToPixel(32);
                flagImg.getLayoutParams().height = height;
                flagImg.getLayoutParams().width = width;
                flagImg.setScaleType(ImageView.ScaleType.FIT_XY);
                flagImg.setBackgroundColor(Color.TRANSPARENT);
                GlideApp.with(this)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .priority(Priority.IMMEDIATE)
                        .skipMemoryCache(false)
                        .into(flagImg);
                flagImg.setPadding(10,10,10,10);
                flagImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String flagNoteConsolidate = flagTitleText + ": " + flagNoteText;
                        flagText.setText(flagNoteConsolidate);
                    }
                });
                flagLay.addView(flagImg);
            }
            clientViewScrollView.addView(flagLay);
            clientViewScrollView.addView(flagText);

        }
    }
    private void addDocumentAttach(ClientDetail clientDetail) {
        int numAttach = clientDetail.errataDoc.size();

        for (int i = 0; i < numAttach; i++) {
            final Map<String,String> errataDic = clientDetail.errataDoc.get(i);
            String fieldLabel = errataDic.get("fieldlabel");
            LinearLayout docAttachLay  = new LinearLayout(this);
            docAttachLay.setOrientation(LinearLayout.HORIZONTAL);
            ImageButton docButton = new ImageButton(this);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            docButton.setLayoutParams(params);
            docButton.requestLayout();
            int height = (int)sVisitsAndTracking.convertDpToPixel(32);
            int width = (int)sVisitsAndTracking.convertDpToPixel(32);
            docButton.setBackgroundColor(Color.TRANSPARENT);
            docButton.getLayoutParams().height = height;
            docButton.getLayoutParams().width = width;
            docButton.setBackground(docAttachIcon);
            docButton.setScaleType(ImageView.ScaleType.FIT_XY);
            docButton.setPadding(60,50,30,10);

            TextView docText = new TextView(this);
            docText.setText(fieldLabel);
            docText.setTextColor(Color.WHITE);
            docText.setTextSize(18);
            docText.setPadding(10,10,10,10);
            docAttachLay.addView(docButton);
            docAttachLay.addView(docText);

            docButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle basket = new Bundle();
                    basket.putString("url",errataDic.get("url"));
                    basket.putString("label",errataDic.get("label"));
                    basket.putString("errataIndex",errataDic.get("errataIndex"));
                    basket.putString("mimetype",errataDic.get("mimetype"));
                    basket.putString("fieldLabel",errataDic.get("fieldlabel"));

                    Intent webViewIntent = new Intent(ViewVisitDetail.this, WebViewActivity.class);
                    webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    webViewIntent.putExtras(basket);
                    mContext.startActivity(webViewIntent);
                }
            });
            clientViewScrollView.addView(docAttachLay);
        }
    }
    private void addBasicInfo(ClientDetail clientDetail) {
        LinearLayout basicInfoLay  = new LinearLayout(this);
        basicInfoLay.setOrientation(LinearLayout.VERTICAL);

        if(!clientDetails.clientNotes.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("CLIENT NOTES",clientDetails.clientNotes, Color.WHITE, 18));
        }
        if (!clientDetails.firstName.equals("NULL VALUE") &&
                !clientDetails.lastName.equals("NULL VALUE")) {
            String clientName = clientDetails.firstName + " " + clientDetails.lastName;
            basicInfoLay.addView(createLabelContent("Pet Owner", clientName, Color.WHITE, 18));
        }
        if (!clientDetails.firstName2.equals("NULL VALUE") && !clientDetails.lastName2.equals("NULL VALUE")) {
            String altName = clientDetails.firstName2 + " " + clientDetails.lastName2;
            basicInfoLay.addView(createLabelContent("Pet Owner (Alt)", altName, Color.WHITE, 18));
        }
        if(!clientDetails.street1.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("Address", clientDetails.street1, Color.WHITE,18));
        }
        if(!clientDetails.street2.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("Address2" ,  clientDetails.street2, Color.WHITE,18));
        }
        if(!clientDetails.city.equals("NULL VALUE") &&
                !clientDetails.state.equals("NULL VALUE") &&
                !clientDetails.zip.equals("NULL VALUE")) {
            String cityStateZip = clientDetails.city + ", " + clientDetails.state + "  " + clientDetails.zip;
            basicInfoLay.addView(createLabelContent("City, State, Zip", cityStateZip, Color.WHITE,18));
        }
        if(!clientDetails.cellphone.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("Cell Phone: ", clientDetails.cellphone,Color.WHITE,18));
        }
        if(!clientDetails.cellphone2.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("Cell Phone (Alt): ", clientDetails.cellphone2,Color.WHITE,18));
        }
        if(!clientDetails.workphone.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("Work Phone: ", clientDetails.workphone,Color.WHITE,18));
        }
        if(!clientDetails.homePhone.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("Home Phone: ", clientDetails.homePhone,Color.WHITE,18));
        }
        if(!clientDetails.email.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("Email: ", clientDetails.email,Color.WHITE,18));
        }
        if(!clientDetails.email2.equals("NULL VALUE")) {
            basicInfoLay.addView(createLabelContent("Email (alt): ", clientDetails.email2,Color.WHITE,18));
        }
        ImageView separator = new ImageView(this);

        clientViewScrollView.addView(basicInfoLay);
        separator.setLayoutParams(new LinearLayout.LayoutParams(basicInfoLay.getWidth(), 2));
        separator.setImageDrawable(whiteLine);
        clientViewScrollView.addView(separator);
    }
    private void addHomeInfoData(ClientDetail clientDetails) {

        LinearLayout homeInfoLinLay  = new LinearLayout(this);
        homeInfoLinLay.setOrientation(LinearLayout.VERTICAL);

        if(!clientDetails.garageGateCode.equals("NULL VALUE")) {
            homeInfoLinLay.addView(createLabelContent("Garage/Gate Code", clientDetails.garageGateCode, Color.WHITE, 18));
        }
        if(!clientDetails.leashLocation.equals("NULL VALUE")) {
            homeInfoLinLay.addView(createLabelContent("Leash location:", clientDetails.leashLocation, Color.WHITE, 18));
        }
        if(!clientDetails.parkingInfo.equals("NULL VALUE")) {
            homeInfoLinLay.addView(createLabelContent("Parking info:", clientDetails.parkingInfo, Color.WHITE, 18));
        }
        if(!clientDetails.directions.equals("NULL VALUE")) {
            homeInfoLinLay.addView(createLabelContent("Directions:", clientDetails.directions, Color.WHITE, 18));
        }
        if(!clientDetails.foodLocation.equals("NULL VALUE")) {
            homeInfoLinLay.addView(createLabelContent("Food location:", clientDetails.foodLocation, Color.WHITE, 18));
        }
        if(!clientDetails.alarmInfo.equals("NULL VALUE")) {
            homeInfoLinLay.addView(createLabelContent("Alarm Info: ", clientDetails.alarmInfo, Color.WHITE, 18));
        }
        if(!clientDetails.alarmCompany.equals("NULL VALUE")) {
            homeInfoLinLay.addView(createLabelContent("Alarm Details:", clientDetails.alarmCompany, Color.WHITE, 18));
        }
        if(!clientDetails.alarmCompanyPhone.equals("NULL VALUE")) {
            homeInfoLinLay.addView(createLabelContent("Alarm Phone:", clientDetails.alarmCompanyPhone, Color.WHITE, 18));
        }

        clientViewScrollView.addView(homeInfoLinLay);

    }
    private void addEmergencyData(ClientDetail clientDetails) {

        LinearLayout emergencyInfoLay  = new LinearLayout(this);
        emergencyInfoLay.setOrientation(LinearLayout.VERTICAL);

        if(!clientDetails.emergencyName.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("EMERGENCY NAME",clientDetails.emergencyName,Color.WHITE, 18));
        }
        if(!clientDetails.emergencyCellPhone.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("EMERGENCY CELL PHONE",clientDetails.emergencyCellPhone,Color.WHITE, 18));
        }
        if(!clientDetails.emergencyWorkPhone.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("EMERGENCY WORK PHONE",clientDetails.emergencyWorkPhone,Color.WHITE, 18));
        }
        if(!clientDetails.emergencyHomePhone.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("EMERGENCY HOME PHONE",clientDetails.emergencyHomePhone,Color.WHITE, 18));
        }
        if(!clientDetails.emergencyHasKey.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("EMERGENCY HAS KEY",clientDetails.emergencyHasKey,Color.WHITE, 18));
        }
        if(!clientDetails.emergencyLocation.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("EMERGENCY LOCATION",clientDetails.emergencyLocation,Color.WHITE, 18));
        }
        if(!clientDetails.emergencyNote.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("EMERGENCY NOTE",clientDetails.emergencyNote,Color.WHITE, 18));
        }
        if(!clientDetails.trustedNeighborName.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("TRUSTED NEIGHBOR NAME",clientDetails.trustedNeighborName,Color.WHITE, 18));
        }
        if(!clientDetails.trustedNeighborCellPhone.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("TRUSTED NEIGHBOR CELL PHONE",clientDetails.trustedNeighborCellPhone,Color.WHITE, 18));
        }
        if(!clientDetails.trustedNeighborWorkPhone.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("TRUSTED NEIGHBOR WORK",clientDetails.trustedNeighborWorkPhone,Color.WHITE, 18));
        }
        if(!clientDetails.trustedNeighborHomePhone.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("TRUSTED NEIGHBOR HOME PHONE",clientDetails.trustedNeighborHomePhone,Color.WHITE, 18));
        }
        if(!clientDetails.trustedNeighborHasKey.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("TRUSTED NEIGHBOR CELL PHONE",clientDetails.emergencyCellPhone,Color.WHITE, 18));
        }
        if(!clientDetails.trustedNeighborLocation.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("TRUSTED NEIGHBOR LOCATION",clientDetails.trustedNeighborLocation,Color.WHITE, 18));
        }
        if(!clientDetails.trustedNeighborNote.equals("NULL VALUE")) {
            emergencyInfoLay.addView(createLabelContent("TRUSTED NEIGHBOR NOTE",clientDetails.trustedNeighborNote,Color.WHITE, 18));
        }

        clientViewScrollView.addView(emergencyInfoLay);
    }
    private void addVetData(ClientDetail clientDetails) {

        LinearLayout vetInfoLay  = new LinearLayout(this);
        vetInfoLay.setOrientation(LinearLayout.VERTICAL);

        if(!clientDetails.vetName.equals("NULL VALUE")) {
            vetInfoLay.addView(createLabelContent("VET NAME",clientDetails.vetName,Color.WHITE, 18));
        }
        if(!clientDetails.vetPhone.equals("NULL VALUE")) {
            vetInfoLay.addView(createLabelContent("VET PHONE",clientDetails.vetPhone,Color.WHITE, 18));
        }
        if(!clientDetails.vetCity.equals("NULL VALUE")) {
            String vetCityStateZip = clientDetails.vetCity + ", " + clientDetails.vetState + "  " + clientDetails.vetZip;
            vetInfoLay.addView(createLabelContent("VET CITY, STATE ZIP",vetCityStateZip,Color.WHITE, 18));
        }
        if(!clientDetails.clinicName.equals("NULL VALUE")) {
            vetInfoLay.addView(createLabelContent("CLINIC NAME",clientDetails.clinicName,Color.WHITE, 18));
        }
        if(!clientDetails.clinicStreet1.equals("NULL VALUE")) {
            vetInfoLay.addView(createLabelContent("CLINIC STREET",clientDetails.clinicStreet1,Color.WHITE, 18));
        }
        if(!clientDetails.clinicPhone.equals("NULL VALUE")) {
            vetInfoLay.addView(createLabelContent("CLINIC PHONE",clientDetails.clinicPhone,Color.WHITE, 18));
        }
        if(!clientDetails.clinicCity.equals("NULL VALUE")) {
            String clinicCityZipState = clientDetails.clinicCity + ", " + clientDetails.clinicState + "  " + clientDetails.clinicZip;
            vetInfoLay.addView(createLabelContent("CLINIC CITY, ZIP",clinicCityZipState,Color.WHITE, 18));
        }
        clientViewScrollView.addView(vetInfoLay);
    }
    private void addCustomFields(ClientDetail clientDetails) {

        LinearLayout customLinLay  = new LinearLayout(this);
        customLinLay.setOrientation(LinearLayout.VERTICAL);

        Map<String,String> clientCustom = clientDetails.customClientFields;

        TreeMap<String,Object> sorted = new TreeMap<>();
        sorted.putAll(clientDetails.customClientFieldsSort);
        for (Map.Entry<String,Object> entry : sorted.entrySet()) {
            String dictKey =  entry.getKey();
            HashMap dictVal = (HashMap)entry.getValue();

            Set allKeys = dictVal.keySet();
            String key = "";
            String val = "";
            for (Object keyItem : allKeys) {
                String keyString = (String) keyItem;

                if (keyString.equals("label")) {
                    key = (String) dictVal.get(keyString);
                } else {
                    val = (String) dictVal.get(keyString);
                }
            }


            if(!val.equals("NULL VALUE")) {
                if(val.equals("1")) {
                    clientViewScrollView.addView(createCheckOrX(key,"YES",Color.CYAN,18));
                } else if (val.equals("0")) {
                    clientViewScrollView.addView(createCheckOrX(key,"NO",Color.CYAN,18));
                } else if (!val.equals("1") && !val.equals("0")) {
                    clientViewScrollView.addView(createLabelContent(key,val,Color.WHITE,18));
                }
            }
        }
        clientViewScrollView.addView(customLinLay);
    }
    private void addPetImages(ClientDetail clientDetail) {
        Context context = this.getApplicationContext();
        int numberImages = clientDetails.petList.size();

        LinearLayout petPhotoLinLay = new LinearLayout(this);
        petPhotoLinLay.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout petPhotoLinLay2 = new LinearLayout(this);
        petPhotoLinLay2.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout petPhotoLinLay3 = new LinearLayout(this);
        petPhotoLinLay3.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout petPhotoLinLay4 = new LinearLayout(this);
        petPhotoLinLay4.setOrientation(LinearLayout.HORIZONTAL);


        for (int i = 0; i < numberImages; i++) {
            PetsForClient pet = clientDetails.petList.get(i);
            String petName = pet.name;
            String url = "https://leashtime.com/pet-photo-sessionless.php?id=" + pet.petid + "&loginid=" + sVisitsAndTracking.mPreferences.getString("username","") + "&password=" + sVisitsAndTracking.mPreferences.getString("password","") + "&version=display";
            Intent startIntent = new Intent(context, PetDetailActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("visitID", currentVisit.appointmentid);
            startIntent.putExtra("clientID", clientDetails.clientID);
            startIntent.putExtra("petID", petName);

            LinearLayout imageTitle = new LinearLayout(this);
            imageTitle.setOrientation(LinearLayout.VERTICAL);
            imageTitle.setDividerDrawable(whiteLine);

            ImageView petImageView = new ImageView(this);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            petImageView.setLayoutParams(params);
            petImageView.requestLayout();
            int height = (int)sVisitsAndTracking.convertDpToPixel(100);
            int width = (int)sVisitsAndTracking.convertDpToPixel(100);
            petImageView.getLayoutParams().height = height;
            petImageView.getLayoutParams().width = width;
            petImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            petImageView.setPadding(10,10,10,10);
            GlideApp.with(getApplicationContext()).
                    load(url).
                    diskCacheStrategy(DiskCacheStrategy.ALL).
                    priority(Priority.HIGH).
                    skipMemoryCache(false).
                    into(petImageView);
            petImageView.setTag(petName);
            petImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    context.startActivity(startIntent);
                }
            });

            imageTitle.addView(petImageView);
            TextView nameTextField = new TextView(this);
            nameTextField.setTextColor(Color.WHITE);
            nameTextField.setTextSize(14);
            nameTextField.setText(petName);
            imageTitle.addView(nameTextField);

            if (i >2 && i < 6) {
                petPhotoLinLay2.addView(imageTitle);
            } else if (i > 5 && i <9) {
                petPhotoLinLay3.addView(imageTitle);
            } else if (i > 8) {
                petPhotoLinLay4.addView(imageTitle);
            } else if (i < 3) {
                petPhotoLinLay.addView(imageTitle);
            }
        }
        clientViewScrollView.addView(petPhotoLinLay);

        if (numberImages > 3) {
            clientViewScrollView.addView(petPhotoLinLay2);
        }
        if(numberImages > 6) {
            clientViewScrollView.addView(petPhotoLinLay3);
        }
        if(numberImages > 9) {
            clientViewScrollView.addView(petPhotoLinLay4);
        }
    }
    private LinearLayout createLabelContent (String label, String content, int fieldColor, int fontSize) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(44,10,44,10);

        TextView labelLabel = new TextView(this);
        labelLabel.setText(label);
        labelLabel.setTextSize(fontSize);
        labelLabel.setTextColor(Color.BLUE);

        TextView labelContent = new TextView(this);
        labelContent.setText(content);
        labelContent.setTextSize(fontSize);
        labelContent.setTextColor(fieldColor);
        linearLayout.addView(labelLabel);
        linearLayout.addView(labelContent);
        return linearLayout;
    }
    private LinearLayout createCheckOrX  (String label, String content, int fieldColor, int fontSize) {

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setPadding(44,10,44,10);
        ImageView onOff = new ImageView(this);

        if (content.equals("YES")) {
            onOff.setImageDrawable(checkIcon);
        } else {
            onOff.setImageDrawable(xIcon);
        }

        linearLayout.addView(onOff);

        TextView labelLabel = new TextView(this);
        labelLabel.setText(label);
        labelLabel.setTextSize(fontSize);
        labelLabel.setTextColor(Color.BLUE);
        labelLabel.setPadding(32,10,32,10);

        TextView labelContent = new TextView(this);
        labelContent.setText(content);
        labelContent.setTextSize(fontSize);
        labelContent.setTextColor(fieldColor);
        labelContent.setPadding(32,10,32,10);

        linearLayout.addView(labelLabel);
        linearLayout.addView(labelContent);

        return linearLayout;
    }
    private void addVisitSummary(ClientDetail clientDetails, VisitDetail currentVisit) {

        Button tableView = findViewById(R.id.dismissDetail);
        String visitAppointmentID = currentVisit.appointmentid;
        String currVisSum = currentVisit.pets + ' ' + currentVisit.starttime + '-' + currentVisit.endtime;

        tableView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DismissDetailVisitViewEvent event = new DismissDetailVisitViewEvent(visitAppointmentID);
                EventBus.getDefault().post(event);
                finish();
            }
        });
    }
}


