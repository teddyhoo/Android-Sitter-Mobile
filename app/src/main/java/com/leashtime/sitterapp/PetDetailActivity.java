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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PetDetailActivity extends AppCompatActivity {

    private Drawable femaleIcon;
    private Drawable maleIcon;
    private Drawable docAttachIcon;
    private Drawable checkIcon;
    private Drawable xIcon;
    public Context mContext;
    private PetsForClient chosenPet;
    private LinearLayout layout;


    @Override
    protected void onCreate (Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        mContext = this.getApplicationContext();
        //TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Lato-Regular.ttf"); // font from assets: "assets/fonts/Roboto-Regular.ttf
        setContentView(R.layout.pet_profile_view);
        femaleIcon = ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.female_icon_3x);
        docAttachIcon = ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.file_folder_line_3x);
        checkIcon =ContextCompat.getDrawable(this.getApplicationContext(),R.drawable.check_mark_green_3x);
        xIcon = ContextCompat.getDrawable(this.getApplicationContext(),R.drawable.x_mark_red_3x);
        maleIcon = ContextCompat.getDrawable(this.getApplicationContext(),R.drawable.male_icon_3x);

        layout = (LinearLayout) findViewById(R.id.petLinearLayout);

        VisitsAndTracking mVisitsTracking = VisitsAndTracking.getInstance();
        Bundle getData = getIntent().getExtras();
        String petName = getData.getString("petID");
        String clientID = getData.getString("clientID");

        for (ClientDetail client : mVisitsTracking.clientData) {
            if (clientID.equals(client.clientID)) {
                for (PetsForClient pet : client.petList) {
                    if (pet.name.equals(petName)) {
                        chosenPet = pet;
                    }
                }
            }
        }
        TextView petNameField = (TextView) findViewById(R.id.petName);
        TextView petBreedField = (TextView) findViewById(R.id.petTypeBreed);
        TextView petAgeField = (TextView) findViewById(R.id.petAge);
        TextView petColorField = (TextView) findViewById(R.id.petColor);
        TextView petGenderField = (TextView) findViewById(R.id.petGender);
        TextView petFixed = (TextView) findViewById(R.id.petFixed);
        ImageView petPicDetail = (ImageView) findViewById(R.id.petPic1);
        ImageView genderImage = (ImageView) findViewById(R.id.genderIcon);
        ImageButton backButton = (ImageButton) findViewById(R.id.backButton);

        String imgID = chosenPet.petid;
        String url = "https://leashtime.com/pet-photo-sessionless.php?id=" + imgID + "&loginid=" + mVisitsTracking.USERNAME + "&password=" + mVisitsTracking.PASSWORD + "&version=display";
        Glide.with(getApplicationContext()).load(url).into(petPicDetail);
        String descStr = chosenPet.description;
        String noteStr = chosenPet.notes;
        if(!chosenPet.name.equals("NULL VALUE")) {
            petNameField.setText(chosenPet.name);
        } else {
            petNameField.setText("NO NAME");
        }
        if (!chosenPet.fixed.equals("NULL VALUE")) {
            petFixed.setText(chosenPet.fixed);

        } else {
            petFixed.setText("FIXED UNKNOWN");
        }
        if (!chosenPet.breed.equals("NULL VALUE")) {
            petBreedField.setText(chosenPet.breed);
        } else {
            petBreedField.setText("NO BREED");
        }
        if (!chosenPet.age.equals("NULL VALUE")) {
            petAgeField.setText(chosenPet.age);
        } else {
            petAgeField.setText("NO AGE");
        }
        if (!chosenPet.color.equals("NULL VALUE")) {
            petColorField.setText(chosenPet.color);
        } else {
            petColorField.setText(" NO COLOR");
        }
        if (!chosenPet.gender.equals("NULL VALUE")) {
            petGenderField.setText(chosenPet.gender);
            if (chosenPet.gender.equals("f")) {
                genderImage.setImageDrawable(femaleIcon);
            } else {
                genderImage.setImageDrawable(maleIcon);
            }
        } else {
            petGenderField.setText("NO GENDER");
        }
        if(!chosenPet.fixed.equals("NULL VALUE")) {
            petFixed.setText(chosenPet.fixed);
        } else {
            petFixed.setText("FIXED UNKNOWN");
        }
        if (noteStr.equals("NULL VALUE")) {
            // removeView(petNoteField);
        } else {
            TextView notesLabel = new TextView(this);
            TextView notesField = new TextView(this);
            notesField.setText(chosenPet.notes);
            notesField.setTextSize(18);
            notesField.setVisibility(View.VISIBLE);
            notesField.setTextColor(Color.BLACK);
            notesField.setPadding(40,10,10,30);;
            notesLabel.setText("PET NOTES");
            notesLabel.setTextSize(18);
            notesLabel.setVisibility(View.VISIBLE);
            notesLabel.setTextColor(Color.WHITE);
            notesLabel.setPadding(40,10,10,30);
            layout.addView(notesLabel);
            layout.addView(notesField);

        }
        if (descStr.equals("NULL VALUE")) {
            //removeView(petDescField);
        } else {
            TextView descrLabel = new TextView(this);
            TextView descriptionField = new TextView(this);
            descriptionField.setText(chosenPet.description);
            descriptionField.setTextSize(18);
            descriptionField.setVisibility(View.VISIBLE);
            descriptionField.setTextColor(Color.BLACK);
            descriptionField.setPadding(40,10,10,30);
            descrLabel.setText("PET DESCRIPTION");
            descrLabel.setTextSize(18);
            descrLabel.setVisibility(View.VISIBLE);
            descrLabel.setTextColor(Color.WHITE);
            descrLabel.setPadding(40,10,10,30);
            layout.addView(descrLabel);
            layout.addView(descriptionField);

        }
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        addDocAttach();
        buildCustomLayoutFields();
    }

    private void addDocAttach() {
        int numberDocs = chosenPet.petDocsAttach.size();

        VisitsAndTracking sVisitsAndTracking = VisitsAndTracking.getInstance();

        if (chosenPet.petDocsAttach.size() > 0) {
            for (int i = 0; i < chosenPet.petDocsAttach.size(); i++) {
                final Map<String, String> petDocAttach = chosenPet.petDocsAttach.get(i);

                LinearLayout docAttachLayout = new LinearLayout(this);
                docAttachLayout.setOrientation(LinearLayout.HORIZONTAL);
                docAttachLayout.setPadding(40,10,10,20);
                ImageButton petDoc = new ImageButton(this);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                petDoc.setLayoutParams(params);
                petDoc.requestLayout();
                int height = (int)sVisitsAndTracking.convertDpToPixel(32);
                int width = (int)sVisitsAndTracking.convertDpToPixel(32);
                petDoc.getLayoutParams().height = height;
                petDoc.getLayoutParams().width = width;
                petDoc.setBackground(docAttachIcon);
                petDoc.setScaleType(ImageView.ScaleType.FIT_XY);
                petDoc.setVisibility(View.VISIBLE);
                petDoc.setTag(chosenPet.petDocsAttach.get(i));
                petDoc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle basket = new Bundle();
                        basket.putString("url", petDocAttach.get("url"));
                        basket.putString("label", petDocAttach.get("label"));
                        basket.putString("errataIndex", petDocAttach.get("errataIndex"));
                        basket.putString("mimetype", petDocAttach.get("mimetype"));
                        basket.putString("fieldLabel", petDocAttach.get("fieldlabel"));
                        Intent webViewIntent = new Intent(PetDetailActivity.this, WebViewActivity.class);
                        webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        webViewIntent.putExtras(basket);
                        mContext.startActivity(webViewIntent);
                    }
                });

                TextView petDocText = new TextView(this);
                petDocText.setTextSize(18);
                petDocText.setTextColor(ContextCompat.getColor(mContext, R.color.yellow_200));
                petDocText.setPadding(40,20,20,20);
                //petDocText.setText(petDocAttach.get("label"));
                petDocText.setVisibility(View.VISIBLE);
                petDocText.setText(petDocAttach.get("fieldlabel"));

                docAttachLayout.addView(petDoc);
                docAttachLayout.addView(petDocText);

                layout.addView(docAttachLayout);
            }
        }
    }

    private void buildCustomLayoutFields () {

        TreeMap<String,Object> sorted = new TreeMap<>();
        sorted.putAll(chosenPet.petCustomFieldsSort);
        for (Map.Entry<String,Object> entry : sorted.entrySet()) {
            String dictKey = entry.getKey();
            //System.out.println("Entry key: " + dictKey);
            HashMap<String,Object> petCustom = chosenPet.petCustomFieldsSort.get(dictKey);
            String keyPet = (String)petCustom.get("label");
            String valPet = (String)petCustom.get("value");
            //System.out.println("Key pet: " + keyPet + ", Val pet: " + valPet);
            if (valPet.equals("1"))
                valPet = "YES";
            else if (valPet.equals("0"))
                valPet = "NO";


            TextView customLabel = new TextView(this);
            TextView customValue = new TextView(this);
            if (!valPet.equals("NULL VALUE") || valPet != null) {
                customLabel.setText(keyPet);
                customLabel.setTextSize(18);
                customLabel.setVisibility(View.VISIBLE);
                customLabel.setTextColor(ContextCompat.getColor(mContext, R.color.yellow_200));

                customValue.setText(valPet);
                customValue.setTextSize(18);
                customValue.setVisibility(View.VISIBLE);
                customValue.setTextColor(Color.BLACK);
                customValue.setPadding(40, 20, 20, 20);

                if (valPet.equals("YES") || valPet.equals("NO")) {
                    LinearLayout yesNoLayout = new LinearLayout(this);
                    yesNoLayout.setOrientation(LinearLayout.HORIZONTAL);
                    yesNoLayout.setPadding(40, 30, 40, 40);

                    ImageView valueIcon = new ImageView(this);
                    if (valPet.equals("YES")) {
                        valueIcon.setImageDrawable(checkIcon);
                    } else {
                        valueIcon.setImageDrawable(xIcon);
                    }
                    valueIcon.setMaxHeight(16);
                    valueIcon.setMaxHeight(16);
                    valueIcon.setPadding(40, 30, 20, 20);
                    yesNoLayout.addView(valueIcon);
                    yesNoLayout.addView(customLabel);
                    yesNoLayout.addView(customValue);
                    layout.addView(yesNoLayout);
                } else {
                    LinearLayout customFieldValLayout = new LinearLayout(this);
                    customFieldValLayout.setOrientation(LinearLayout.HORIZONTAL);
                    customFieldValLayout.setPadding(40, 30, 40, 40);
                    customLabel.setPadding(40, 30, 20, 20);
                    customValue.setPadding(40, 30, 20, 20);
                    layout.addView(customLabel);
                    layout.addView(customValue);
                }
            }
        }

        /*for (Map<String, String> customPetMap : chosenPet.petCustomFields) {
            String label = "";
            String valLabel = "";
            Set<Map.Entry<String, String>> entrySet = customPetMap.entrySet();
            for (Map.Entry entry : entrySet) {
                label = (String) entry.getKey();
                valLabel = (String) entry.getValue();
                if (valLabel.equals("1"))
                    valLabel = "YES";
                else if (valLabel.equals("0"))
                    valLabel = "NO";
            }

            TextView customLabel = new TextView(this);
            TextView customValue = new TextView(this);

            if (!valLabel.equals("NULL VALUE") || valLabel != null) {
                customLabel.setText(label);
                customLabel.setTextSize(18);
                customLabel.setVisibility(View.VISIBLE);
                customLabel.setTextColor(ContextCompat.getColor(mContext, R.color.yellow_200));

                customValue.setText(valLabel);
                customValue.setTextSize(18);
                customValue.setVisibility(View.VISIBLE);
                customValue.setTextColor(Color.BLACK);
                customValue.setPadding(40,20,20,20);

                if (valLabel.equals("YES") || valLabel.equals("NO")) {
                    LinearLayout yesNoLayout = new LinearLayout(this);
                    yesNoLayout.setOrientation(LinearLayout.HORIZONTAL);
                    yesNoLayout.setPadding(40,30,40,40);

                    ImageView valueIcon = new ImageView(this);
                    if (valLabel.equals("YES")) {
                        valueIcon.setImageDrawable(checkIcon);
                    } else {
                        valueIcon.setImageDrawable(xIcon);
                    }
                    valueIcon.setMaxHeight(16);
                    valueIcon.setMaxHeight(16);
                    valueIcon.setPadding(40,30,20,20);
                    yesNoLayout.addView(valueIcon);
                    yesNoLayout.addView(customLabel);
                    yesNoLayout.addView(customValue);
                    layout.addView(yesNoLayout);
                } else {
                    LinearLayout customFieldValLayout = new LinearLayout(this);
                    customFieldValLayout.setOrientation(LinearLayout.HORIZONTAL);
                    customFieldValLayout.setPadding(40,30,40,40);
                    customLabel.setPadding(40,30,20,20);
                    customValue.setPadding(40,30,20,20);
                    layout.addView(customLabel);
                    layout.addView(customValue);
                }
            }
        }*/
    }
}




