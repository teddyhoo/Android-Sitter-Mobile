package com.leashtime.sitterapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.util.DisplayMetrics;

import com.leashtime.sitterapp.events.FailedParseEvent;
import com.leashtime.sitterapp.events.ReloadVisitsEvent;
import com.leashtime.sitterapp.network.SendPhotoServer;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class VisitsAndTracking {

    public static final String PREF_NAME = "sitterPrefs";
    public String lastLoginResponseCode;
    public String onWhichVisitID;
    public Location lastValidLocation;
    public Date showingWhichDate;
    public Date onWhichDate;
    public String todayDateFormat;
    public String showingDateFormat;
    public String networkStatus;
    public ArrayList<VisitDetail> visitData = new ArrayList<>();
    public ArrayList<VisitDetail> tempVisitData = new ArrayList<>();
    public List<VisitDetail> daysBeforeVisit = new ArrayList<>();
    public List<VisitDetail> daysAfterVisit = new ArrayList<>();
    public List<String> onWhichVisits = new ArrayList<>();
    public List<ClientDetail> clientData = new ArrayList<>();
    public List<FlagItem> mFlagData = new ArrayList<>();
    public List<Location> sessionLocationArray = new ArrayList<>();
    public List<Request> resendCoordUploadRequest = new ArrayList<>();
    private static final SimpleDateFormat nextPrevDateFormat = new SimpleDateFormat("M/d/yyyy",new Locale("US"));
    private final OkHttpClient client = new OkHttpClient();

    public boolean isMultiVisitArrive;
    public boolean showClientName;
    public boolean showFlags;
    public boolean showKey;
    public boolean showVisitTimer;
    public boolean showReachability;
    public boolean showPetPic;
    public String USERNAME;
    public String PASSWORD;
    public String USER_AGENT;
    public int EARLY_ARRIVAL_MIN = 90;
    private Context mContext;
    public SimpleDateFormat formatter;
    public SharedPreferences mPreferences;
    public static VisitsAndTracking sVisitsAndTracking;

    public void                     init(Context context) {

        mContext = context;
        onWhichVisitID = "0000";
        formatter = new SimpleDateFormat("M/d/yyyy", Locale.US);
        mPreferences = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        setupPrefs();
        isMultiVisitArrive = false;
        lastLoginResponseCode = "OK";
        showingWhichDate = new Date();
        onWhichDate = showingWhichDate;
        todayDateFormat = formatter.format(onWhichDate);
        showingDateFormat = formatter.format(showingWhichDate);
    }
    public static float convertDpToPixel(float dp){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
    public void   reInit(Context context) {

        mContext = context;
        onWhichVisitID = "0000";
        formatter = new SimpleDateFormat("M/d/yyyy",Locale.US);
        mPreferences = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        setupPrefs();
        isMultiVisitArrive = false;
        lastLoginResponseCode = "OK";
        showingWhichDate = new Date();
        onWhichDate = showingWhichDate;
        todayDateFormat = formatter.format(onWhichDate);
        showingDateFormat = formatter.format(showingWhichDate);
    }
    public static VisitsAndTracking getInstance() {

        if(sVisitsAndTracking == null) {
            sVisitsAndTracking = new VisitsAndTracking();
        }
        return sVisitsAndTracking;
    }
    public void parseFlagInfo(JSONArray flagItems) {
        try {
            List<Object> flagList = toList(flagItems);
            Map visitOrFlagItem;
            int lenFlagList = flagList.size();

            for (int i = 0; i < lenFlagList; i++) {
                //Object flagTypeObj = flagList.get(i);
                visitOrFlagItem = (Map) flagList.get(i);
                addFlagItem(visitOrFlagItem);
            }
        } catch (JSONException jse) {
            jse.printStackTrace();
        }

    }
    public void parsePreferencesInfo(JSONObject preferenceItems) {
        Iterator<String> prefIter = preferenceItems.keys();
        while (prefIter.hasNext()) {
            String key = prefIter.next();
            Object value;
            try {
                value = preferenceItems.get(key);
                Class stringClass = value.getClass();
                String stringForClass = stringClass.getName();
                String valParam;

                if (stringForClass.equals("java.lang.String")) {
                    valParam = value.toString();
                    if (valParam.equals("1")) {
                        isMultiVisitArrive = true;
                    } else if (valParam.equals("0")) {
                        isMultiVisitArrive = false;
                    }
                }
            } catch (JSONException jse) {
                jse.printStackTrace();
            }
        }
    }
    public void parseClientInfo(JSONObject clientItems) {

        HashMap<String,String> regularFields = new HashMap<>();

        try {
            Map<String,Object> clientMap = jsonToMap(clientItems);
            Iterator it = clientMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                Map<String,Object> clientDetail = (Map<String,Object>)pair.getValue();

                String clientID = (String)clientDetail.get("clientid");
                Boolean doesClientExist = FALSE;
                for (ClientDetail oldClientDetail : clientData){
                    if (oldClientDetail.clientID.equals(clientID)) {
                        doesClientExist = TRUE;
                    }
                }
                if (!doesClientExist) {
                    ClientDetail newClientDetail = new ClientDetail();
                    Iterator clientIterator = clientDetail.entrySet().iterator();

                    while(clientIterator.hasNext()) {

                        Map.Entry pair2 = (Map.Entry)clientIterator.next();
                        String key = (String)pair2.getKey();
                        Object value = pair2.getValue();
                        Class valClass = value.getClass();
                        String className = valClass.getName();

                        if (className.equals("java.lang.String")) {

                            String valueString = (String)value;
                            regularFields.put(key,valueString);

                        } else if (className.equals("java.util.ArrayList")) {

                            if(key.equals("flags")) {

                                parseFlagData(newClientDetail, key, value);

                            } else if (key.equals("pets")){

                                parsePetData(newClientDetail, value);
                            }
                        } else if (className.equals("java.util.HashMap")) {

                            parseCustomDict(newClientDetail, key, value);
                        }
                        clientIterator.remove();
                    }
                    newClientDetail.addClientData(regularFields);
                    clientData.add(newClientDetail);
                }
                it.remove();
            }
        } catch (JSONException JE) {
            JE.printStackTrace();
        }

    }
    public void parsePetData(ClientDetail newClientDetail, Object petData) {

        ArrayList<Map<String,Object>> petList = null;
        Class valClass = petData.getClass();

        if (valClass.getName().equals("java.util.ArrayList")) {

            petList = (ArrayList<Map<String, Object>>) petData;

            System.out.println("Pet Data raw: " + petList);
            for (Map<String, Object> petMap : petList) {
                PetsForClient petsForClient = new PetsForClient();
                HashMap<String,String> petFields = new HashMap<>();
                Iterator petIterator = petMap.entrySet().iterator();

                while (petIterator.hasNext()) {
                    Map.Entry petPair = (Map.Entry) petIterator.next();
                    String petKey = (String) petPair.getKey();
                    Object petValue = petPair.getValue();
                    Class petValueClass = petValue.getClass();
                    String petCustomLabel = "NONE";
                    String petCustVal = "NONE";
                    HashMap<String, String> customDoc = new HashMap<>();

                    if (petValueClass.getName().equals("java.lang.String")) {

                        String petValueString = (String)petValue;
                        String petValueStringVal = validateClientDataField(petValueString);
                        if(petValueStringVal == null) {
                            petValueStringVal = "NULL VALUE";
                        }
                        if (petKey.equals("petid")) {
                            String globalPetID = petValueStringVal;
                            customDoc.put("petid",globalPetID);
                        }
                        petFields.put(petKey, petValueStringVal);

                    }   else if (petValueClass.getName().equals("java.util.HashMap")) {
                        HashMap<String,Object> hashDic = (HashMap<String, Object>) petValue;
                        //System.out.println("Pet key: " + petKey);
                        //System.out.println(hashDic.toString());
                        if (hashDic.get("label").getClass().getName().equals("java.lang.String")) {
                            petCustomLabel = (String)hashDic.get("label");
                        }
                        if (hashDic.get("value").getClass().getName().equals("java.lang.String")) {
                            petCustVal = (String)hashDic.get("value");
                            String customPetValueStringVal = validateClientDataField(petCustVal);
                            HashMap <String,String> customPair = new HashMap<>();
                            String newKey = "";
                            for (int i = 1; i < 100; i++) {
                                String keyCompare = "petcustom" + i;
                                if (petKey.length() == 10) {
                                    if (keyCompare.equals(petKey)) {
                                        newKey = "petcustomA" + i;
                                    }
                                } else if (petKey.length() == 11) {
                                    if (keyCompare.equals(petKey)) {
                                        newKey = "petcustomB" + i;
                                    }
                                }
                            }

                            customPair.put(petCustomLabel,customPetValueStringVal);
                            petsForClient.petCustomFields.add(customPair);
                            petsForClient.petCustomFieldsSort.put(newKey,hashDic);

                        } else if (hashDic.get("value").getClass().getName().equals("java.util.HashMap")) {

                            HashMap<String, String> petCustomDoc = (HashMap<String, String>) hashDic.get("value");
                            String url = petCustomDoc.get("url");
                            String mimetype = petCustomDoc.get("mimetype");
                            String label = petCustomDoc.get("label");

                            customDoc.put("url", url);
                            customDoc.put("mimetype", mimetype);
                            customDoc.put("label", label);
                            customDoc.put("type", "docattach");
                            customDoc.put("fieldlabel", petCustomLabel);

                            int errataIndex = petsForClient.petDocsAttach.size() + 1;
                            String errataIndexString = String.valueOf(errataIndex);
                            customDoc.put("errataIndex",errataIndexString);
                            petsForClient.petDocsAttach.add(customDoc);
                        }
                    }

                }
                petsForClient.InitPetsForClient(petFields);
                newClientDetail.petList.add(petsForClient);
            }
        }
    }
    public void parseCustomDict (ClientDetail clientDetail, String key, Object clientDic) {

        if (key.equals("neighbor")) {

            Map<String, Object> neighborDic = (Map<String, Object>) clientDic;
            clientDetail.trustedNeighborName = validateClientDataField(neighborDic.get("name"));
            clientDetail.trustedNeighborCellPhone = validateClientDataField(neighborDic.get("cellphone"));
            clientDetail.trustedNeighborHasKey = validateClientDataField(neighborDic.get("hasKey"));
            clientDetail.trustedNeighborHomePhone = validateClientDataField(neighborDic.get("homephone"));
            clientDetail.trustedNeighborWorkPhone = validateClientDataField(neighborDic.get("workphone"));
            clientDetail.trustedNeighborLocation = validateClientDataField(neighborDic.get("location"));
            clientDetail.trustedNeighborNote = validateClientDataField(neighborDic.get("note"));

        } else if (key.equals("emergency")) {

            Map<String, Object> emergencyDic = (Map<String, Object>) clientDic;
            clientDetail.emergencyName = validateClientDataField(emergencyDic.get("name"));
            clientDetail.emergencyCellPhone = validateClientDataField(emergencyDic.get("cellphone"));
            clientDetail.emergencyHasKey = validateClientDataField(emergencyDic.get("haskey"));
            clientDetail.emergencyHomePhone = validateClientDataField(emergencyDic.get("homephone"));
            clientDetail.emergencyWorkPhone = validateClientDataField(emergencyDic.get("workphone"));
            clientDetail.emergencyLocation = validateClientDataField(emergencyDic.get("location"));
            clientDetail.emergencyNote = validateClientDataField(emergencyDic.get("note"));

        } else {
            HashMap<String, Object> customClientFields = (HashMap<String, Object>) clientDic;
            HashMap <String,String> customDoc = new HashMap<>();
            String valueForLabel = "NULL VALUE";
            String customKey = (String) customClientFields.get("label");
            Object customValue =  customClientFields.get("value");

            if (customValue.getClass().getName().equals("java.lang.String") && customValue != null) {

                valueForLabel = (String) customValue;
                clientDetail.customClientFields.put(customKey,valueForLabel);
                String newKey = "";

                for (int i = 1; i < 100; i++) {
                    String keyCompare = "custom"+i;
                    if(key.length() == 7) {
                        if (keyCompare.equals(key)) {
                            newKey = "customA"+i;
                        }
                    } else if (key.length() == 8) {
                        if(keyCompare.equals(key)) {
                            newKey = "customB"+i;
                        }
                    }
                }
                clientDetail.customClientFieldsSort.put(newKey, customClientFields);

            } else if (customValue.getClass().getName().equals("java.util.HashMap")) {
                HashMap<String,String> customDic = (HashMap<String,String>) customValue;
                String url = customDic.get("url");
                String mimetype = customDic.get("mimetype");
                String label = customDic.get("label");
                customDoc.put("url", url);
                customDoc.put("mimetype", mimetype);
                customDoc.put("label", label);
                customDoc.put("type", "docattach");
                customDoc.put("fieldlabel",customKey);
                customDoc.put("type","docAttach");
                int errataIndexCnt = clientDetail.errataDoc.size() + 1;
                String errataIndexString = String.valueOf(errataIndexCnt);
                customDoc.put("errataIndex",errataIndexString);
                clientDetail.errataDoc.add(customDoc);
            }
        }
    }
    public void parseFlagData(ClientDetail newClientDetail, String key, Object flagData) {
        List<HashMap<String,String>> flagMap = (List<HashMap<String,String>>) flagData;

        for(HashMap<String,String> flagItem : flagMap) {
            String flagID = flagItem.get("flagid");
            String flagNote = flagItem.get("note");
            for (FlagItem flagItemSource : mFlagData) {
                if (flagID.equals(flagItemSource.flagID))
                {
                    FlagItem newFlagItem = new FlagItem();
                    newFlagItem.flagID = flagID;
                    newFlagItem.flagNote = flagNote;
                    newFlagItem.flagImgSrc = flagItemSource.flagImgSrc;
                    newFlagItem.flagTitle = flagItemSource.flagTitle;

                    newClientDetail.clientFlags.add(newFlagItem);
                }
            }
        }
    }

    private static Map<String, Object> toMap(JSONObject object) throws JSONException {

        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }
    private static String       validateClientDataField(Object fieldData) {
        String validInvalid;


        if(null != fieldData) {
            Class stringClass = fieldData.getClass();
            String stringForClass = stringClass.getName();
            if(stringForClass.equals("java.lang.String")) {
                validInvalid = (String)fieldData;
                if(validInvalid.isEmpty()) {
                    validInvalid = "NULL VALUE";
                }
            } else {
                validInvalid = "NULL VALUE";  // was _____
            }
        } else {
            validInvalid = "NULL VALUE";
        }
        return validInvalid;
    }
    public void         parseResponseVisitData(String response) {
        try {
            JSONObject jsonData = new JSONObject(response);
            JSONArray visitItems = jsonData.getJSONArray("visits");
            JSONArray flagItems = jsonData.getJSONArray("flags");
            JSONObject clientItems = jsonData.getJSONObject("clients");
            JSONObject preferenceItems = jsonData.getJSONObject("preferences");

            parsePreferencesInfo(preferenceItems);
            parseFlagInfo(flagItems);
            parseClientInfo(clientItems);

            List<Object> visitList = toList(visitItems);
            Map visitOrFlagItem;
            int lenVisitList = visitList.size();

            onWhichVisits.clear();

            for (int i = 0; i < lenVisitList; i++) {
                visitOrFlagItem = (Map) visitList.get(i);
                addVisitList(visitOrFlagItem);
            }

        } catch (JSONException e) {
            FailedParseEvent failedParseEvent = new FailedParseEvent(USERNAME, PASSWORD);
            EventBus.getDefault().post(failedParseEvent);
            e.printStackTrace();
        }
    }
    public void         parseSecondVisitRequest(String response){

        clearPrevNextData();
        try {
            JSONObject jsonData = new JSONObject(response);
            JSONArray visitItems = jsonData.getJSONArray("visits");
            JSONObject clientItems = jsonData.getJSONObject("clients");

            parseClientInfo(clientItems);

            List<Object> visitList = toList(visitItems);
            Map visitOrFlagItem;
            int lenVisitList = visitList.size();

            for (int i = 0; i < lenVisitList; i++) {
                visitOrFlagItem = (Map) visitList.get(i);
                Map<String,String> visitItemDetail = (Map<String,String>) visitList.get(i);
                if(!visitItemDetail.get("shortNaturalDate").equals(todayDateFormat)) {
                    addVisitList(visitOrFlagItem);
                }
            }

            //System.out.println("PARSE SECOND RESPONSE. NUM VISITS PREVIOUS, NEXT: " +  daysAfterVisit.size() + ", "+ daysBeforeVisit.size());

        } catch (JSONException jsonEx) {
            jsonEx.printStackTrace();
        }
    }
    public void         parsePollingUpdate(String response) {
        clearVisitData();
        parseResponseVisitData(response);
    }
    private void        addVisitList(Map<String,String> visitItemDetail) {
        VisitDetail visit = new VisitDetail();
        visit.appointmentid = visitItemDetail.get("appointmentid");
        visit.clientptr = visitItemDetail.get("clientptr");
        //for(ClientDetail clientName : clientData) {
        for (int i = 0; i < clientData.size(); i++) {
            ClientDetail clientName = clientData.get(i);
            if (visit.clientptr.equals(clientName.clientID)) {
                visit.clientname = clientName.clientName;
                visit.hasKey = clientName.hasKey;
                visit.keyID = clientName.keyID;
                if(clientName.keyDescriptionText != null) {
                    visit.keyDescriptionText = clientName.keyDescriptionText;
                }
                if (clientName.noKeyRequired != null) {
                    visit.noKeyRequired = clientName.noKeyRequired;
                }
                if (clientName.useKeyDescriptionInstead != null) {
                    visit.useKeyDescriptionInstead = clientName.useKeyDescriptionInstead;
                }
                visit.garageGateCode = clientName.garageGateCode;
                visit.alarmInfo = clientName.alarmInfo;
                for(PetsForClient pet: clientName.petList) {
                    visit.petListVisit.add(pet.petid);
                }
            }
        }
        visit.providerptr = visitItemDetail.get("providerptr");
        visit.pets = visitItemDetail.get("pets");
        visit.petNames = visitItemDetail.get("petNames");
        visit.service = visitItemDetail.get("service");
        visit.status = visitItemDetail.get("status");
        visit.starttime = visitItemDetail.get("starttime");
        visit.endtime = validateVisitDetail(visitItemDetail.get("endtime"));
        visit.timeofday = visitItemDetail.get("timeofday");
        visit.longitude = validateVisitDetail(visitItemDetail.get("lon"));
        visit.latitude = validateVisitDetail(visitItemDetail.get("lat"));
        visit.canceled = validateVisitDetail(visitItemDetail.get("canceled"));
        visit.completed = validateVisitDetail(visitItemDetail.get("completed"));
        visit.arrived = validateVisitDetail(visitItemDetail.get("arrived"));
        visit.note = validateVisitDetail(visitItemDetail.get("note"));
        visit.endDateTime = validateVisitDetail(visitItemDetail.get("endDateTime"));
        visit.shortNaturalDate = validateVisitDetail(visitItemDetail.get("shortNaturalDate"));
        visit.mapSnapShotImage = "None";
        addVisitProfile(visit);

    }
    private void        addVisitProfile(VisitDetail visit) {

        if(todayDateFormat.equals(visit.shortNaturalDate)) {
            visitData.add(visit);
            tempVisitData.add(visit);
            if(visit.status.equals("arrived")) {
                this.onWhichVisitID = visit.appointmentid;
                if(isMultiVisitArrive) {
                    onWhichVisits.add(visit.appointmentid );
                }
            }
            syncVisitWithFile(visit);
        }  else {
            try {
                Date date = formatter.parse(visit.shortNaturalDate);
                if (isDateEarlier(date)) {
                    daysBeforeVisit.add(visit);
                } else {
                    daysAfterVisit.add(visit);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void        addFlagItem(Map<String, String> flagItemDetail) {

        FlagItem flag = new FlagItem();
        flag.flagID = flagItemDetail.get("flagid");
        flag.flagImgSrc = flagItemDetail.get("src");
        flag.flagTitle = flagItemDetail.get("title");
        mFlagData.add(flag);

    }
    private static String validateVisitDetail(Object visitInfo) {

        String validInvalid;

        if(visitInfo != null) {
            Class stringClass = visitInfo.getClass();
            String stringForClass = stringClass.getName();

            if(stringForClass.equals("java.lang.String")) {
                validInvalid = (String)visitInfo;
            } else {
                validInvalid = "NONE";
            }

        } else {
            validInvalid = "NULL";
        }
        return validInvalid;
    }
    private boolean     isDateEarlier(Date earlierDate) {

        return earlierDate.compareTo(showingWhichDate) < 0;
    }
    private static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {

        Map<String, Object> retMap = new HashMap<String, Object>();

        if(!Objects.equals(json, JSONObject.NULL)) {
            retMap = toMap(json);
        }
        return retMap;
    }
    private static List<Object>        toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
    public void writeVisitDataToFile(VisitDetail visit) {

        File file;
        ObjectOutputStream oos = null;

        try {
            file = new File(mContext.getFilesDir(),visit.appointmentid);
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(visit);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                oos.flush();
                oos.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
    public void syncVisitWithFile(VisitDetail visit) {
        File file = new File(mContext.getFilesDir(), visit.appointmentid);
        ObjectInputStream inputStream = null;

        if(file.exists()) {
            try {
                inputStream = new ObjectInputStream(new FileInputStream(file));
                VisitDetail visitTemp = (VisitDetail) inputStream.readObject();
                visit.didPoo = visitTemp.didPoo;
                visit.didPee = visitTemp.didPee;
                visit.didPlay = visitTemp.didPlay;
                visit.wasHappy = visitTemp.wasHappy;
                visit.wasSad= visitTemp.wasSad;
                visit.wasAngry= visitTemp.wasAngry;
                visit.wasShy= visitTemp.wasShy;
                visit.wasHungry= visitTemp.wasHungry;
                visit.wasSick= visitTemp.wasSick;
                visit.wasCat= visitTemp.wasCat;
                visit.didScoopLitter= visitTemp.didScoopLitter;

                visit.visitNoteBySitter = visitTemp.visitNoteBySitter;
                visit.petPicFileName = visitTemp.petPicFileName;
                visit.dateTimeVisitReportSubmit = visitTemp.dateTimeVisitReportSubmit;
                visit.mapSnapShotImage = visitTemp.mapSnapShotImage;

                if(visitTemp.currentArriveStatus != null) {
                    visit.currentArriveStatus = visitTemp.currentArriveStatus;
                    if(visit.currentArriveStatus.equals("FAIL")) {
                        visit.status = "arrived";
                        visit.arrived = visitTemp.arrived;
                    }
                } else {
                    visit.currentArriveStatus = "NONE";
                }

                if(visitTemp.currentCompleteStatus != null) {
                    visit.currentCompleteStatus = visitTemp.currentCompleteStatus;
                    if(visit.currentCompleteStatus.equals("FAIL")) {
                        visit.status = "completed";
                        visit.completed = visitTemp.completed;
                    }
                } else {
                    visit.currentCompleteStatus = "NONE";
                }

                if(visitTemp.completedLocationStatus != null) {

                    visit.completedLocationStatus = visitTemp.completedLocationStatus;
                } else {
                    visit.completedLocationStatus = "NONE";
                }

                visit.gpsDicForVisit = visitTemp.gpsDicForVisit;
                //System.out.println("Visit coordinates: " + visit.gpsDicForVisit);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            writeVisitDataToFile(visit);
        } else {
            writeVisitDataToFile(visit);
        }
    }
    private void        removeAllVisitDataElements() {
        System.out.println("REMOVING all the current visit item elements");
        Iterator<VisitDetail> visitDetailListIterator= visitData.listIterator();
        while(visitDetailListIterator.hasNext()) {
            VisitDetail visitDetail = visitDetailListIterator.next();
            visitDetailListIterator.remove();
        }
        System.out.println("REMOVED all the current visit item elements");
    }
    public void         setLastValidLocation(Location location) {
        lastValidLocation = location;
        if(onWhichVisitID.equals("0000")) {

        } else if (location != null) {
            sessionLocationArray.add(location);
            if(isMultiVisitArrive) {
                for(String onVisitIDString : onWhichVisits) {
                    for(VisitDetail visit: visitData) {
                        if(onVisitIDString.equals(visit.appointmentid)) {
                            visit.addCoordinateForVisit(location);
                            writeVisitDataToFile(visit);
                        }
                    }
                }
            } else {
                for(VisitDetail visit : visitData) {
                    if (onWhichVisitID.equals(visit.appointmentid)) {
                        visit.addCoordinateForVisit(location);
                        writeVisitDataToFile(visit);
                    }
                }
            }
        }
    }
    public void  getNextPrevDay(Date theDate, String theDay, String nextOrPrev) {

        showingWhichDate =  theDate;
        showingDateFormat = formatter.format(theDate);
        removeAllVisitDataElements();

        if(theDay.equals(todayDateFormat)) {
            //removeAllVisitDataElements();
            for(VisitDetail visitDetail : tempVisitData) {
                syncVisitWithFile(visitDetail);
                visitData.add(visitDetail);
            }
        } else {

            for(VisitDetail visitDetail : daysBeforeVisit) {
                if(visitDetail.shortNaturalDate.equals(theDay)) {
                    syncVisitWithFile(visitDetail);
                    visitData.add(visitDetail);
                }
            }
            for(VisitDetail visitDetail : daysAfterVisit) {
                if(visitDetail.shortNaturalDate.equals(theDay)) {
                    syncVisitWithFile(visitDetail);
                    visitData.add(visitDetail);
                }
            }

        }
        EventBus.getDefault().post(new ReloadVisitsEvent());
    }
    public void sendMapSnapToServer(VisitDetail visit) {
        SendPhotoServer mapUpload = new SendPhotoServer(USERNAME, PASSWORD, visit, "map");
    }
    private  String getDate() {
        Date transmitDate = new Date();
        SimpleDateFormat rightNowFormatTransmit = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return rightNowFormatTransmit.format(transmitDate);
    }
    public String prefUserName () {
        return mPreferences.getString("username","");
    }
    public void prefSetUserName (String userName) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString("username",userName);
        editor.apply();
        this.USERNAME = userName;
    }
    public void prefSetPass(String pass) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString("password",pass);
        editor.apply();
        this.PASSWORD = pass;

    }
    public void setupPrefs() {
        SharedPreferences.Editor editor = mPreferences.edit();

        if(mPreferences.getString("showClientName", "").isEmpty()) {
            editor.putString("showClientName","YES");
            showClientName = TRUE;
        } else {
            if(mPreferences.getString("showClientName", "").equals("YES")) {
                showClientName = TRUE;
            } else {
                showClientName = FALSE;
            }
        }

        if(mPreferences.getString("showKey", "").isEmpty()) {
            editor.putString("showKey","YES");
            showKey = TRUE;
        } else {
            if(mPreferences.getString("showKey", "").equals("YES")) {
                showKey = TRUE;
            } else {
                showKey = FALSE;
            }
        }

        if(mPreferences.getString("showFlags", "").isEmpty()) {
            editor.putString("showFlags","YES");
            showFlags = TRUE;
        } else {
            if(mPreferences.getString("showFlags", "").equals("YES")) {
                showFlags = TRUE;
            } else {
                showFlags = FALSE;
            }
        }
        if(mPreferences.getString("showVisitTimer", "").isEmpty()) {
            showVisitTimer = TRUE;
            editor.putString("showVisitTimer","YES");
        }  else {
            if(mPreferences.getString("showVisitTimer","").equals("YES")) {
                showVisitTimer = TRUE;
            } else {
                showVisitTimer = FALSE;
            }
        }
        if(mPreferences.getString("showPetPic", "").isEmpty()) {
            showPetPic = TRUE;
            editor.putString("showPetPic","YES");
        } else {
            if(mPreferences.getString("showPetPic","").equals("YES")) {
                showPetPic = TRUE;
            } else {
                showPetPic = FALSE;
            }
        }
        if(mPreferences.getString("showReachability", "").isEmpty()) {
            showReachability = FALSE;
            editor.putString("showReachability","NO");
        }  else {
            showReachability = TRUE;
            showReachability = mPreferences.getString("showReachability", "").equals("YES");
        }

        if(mPreferences.getString("username", "").isEmpty()) {

            editor.putString("username","");
            System.out.println("PREF seek username is EMPTY: ");
            USERNAME = mPreferences.getString("username","");
        } else {
            USERNAME = mPreferences .getString("username","");
            System.out.println("PREF seek USERNAME: " + USERNAME);

        }

        if(mPreferences.getString("password", "").isEmpty()) {
            editor.putString("password","");
            PASSWORD = mPreferences.getString("password","");
            System.out.println("PREF seek password is EMPTY: ");

        } else {
            PASSWORD = mPreferences.getString("password", "");
            System.out.println("PREF seek USERNAME: " + PASSWORD);

        }
        editor.apply();
    }
    public synchronized void clearVisitData () {
        Iterator<FlagItem> flagIterator = mFlagData.listIterator();
        while(flagIterator.hasNext()) {
            FlagItem flagItem = flagIterator.next();
            flagIterator.remove();
            flagItem = null;
        }
        mFlagData.clear();

        Iterator<VisitDetail> visitDetailListIterator= visitData.listIterator();
        while(visitDetailListIterator.hasNext()) {
            VisitDetail visitItem = visitDetailListIterator.next();
            visitDetailListIterator.remove();
        }
        visitData.clear();

        Iterator<VisitDetail> visitDetailListIterator2 = daysAfterVisit.listIterator();
        while(visitDetailListIterator2.hasNext()) {
            VisitDetail visitItem = visitDetailListIterator2.next();
            visitDetailListIterator2.remove();
        }
        daysAfterVisit.clear();

        Iterator<VisitDetail> visitDetailListIterator3 = daysBeforeVisit.listIterator();
        while(visitDetailListIterator3.hasNext()) {
            VisitDetail visitItem = visitDetailListIterator3.next();
            visitDetailListIterator3.remove();
        }
        daysBeforeVisit.clear();

        Iterator<VisitDetail> visitDetailListIterator4 = tempVisitData.listIterator();
        while(visitDetailListIterator4.hasNext()) {
            VisitDetail visitItem = visitDetailListIterator4.next();
            visitDetailListIterator4.remove();
        }
        tempVisitData.clear();

        System.out.println("Visit Data count: " + visitData.size() + ", Days After Visit Count: " + daysAfterVisit.size() + ", Days before visit count: " + daysBeforeVisit.size() + ", Temp: " + tempVisitData.size());
        System.out.println("Flag count: " + mFlagData.size());
    }
    public void clearPrevNextData () {

        Iterator<VisitDetail> visitDetailListIterator2 = daysAfterVisit.listIterator();
        while(visitDetailListIterator2.hasNext()) {
            VisitDetail visitItem = visitDetailListIterator2.next();
            visitDetailListIterator2.remove();
        }
        daysAfterVisit.clear();

        Iterator<VisitDetail> visitDetailListIterator3 = daysBeforeVisit.listIterator();
        while(visitDetailListIterator3.hasNext()) {
            VisitDetail visitItem = visitDetailListIterator3.next();
            visitDetailListIterator3.remove();
        }
        daysBeforeVisit.clear();

    }
}

