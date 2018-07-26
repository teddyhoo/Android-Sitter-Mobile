package com.leashtime.sitterapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ClientDetail {

    public final Collection<String> listOfPetIDs = new ArrayList<>();


    public String workphone;
    public String cellphone;
    public String cellphone2;
    public String homePhone;
    public String clientID;
    public String sortName;
    public String clientName;

    public String firstName;
    public String firstName2;
    public String lastName;
    public String lastName2;
    public String email;
    public String email2;

    public String street1;
    public String street2;
    public String city;
    public String state;
    public String zip;
    public String garageGateCode;
    public String alarmCompany;
    public String alarmCompanyPhone;
    public String alarmInfo;
    public String hasKey;
    public String noKeyRequired;
    public String useKeyDescriptionInstead;
    public String keyDescriptionText;
    public String keyID;
    public String emergencyName;
    public String emergencyCellPhone;
    public String emergencyWorkPhone;
    public String emergencyHomePhone;
    public String emergencyLocation;
    public String emergencyNote;
    public String emergencyHasKey;
    public String trustedNeighborName;
    public String trustedNeighborCellPhone;
    public String trustedNeighborWorkPhone;
    public String trustedNeighborHomePhone;
    public String trustedNeighborLocation;
    public String trustedNeighborNote;
    public String trustedNeighborHasKey;
    public String leashLocation;
    public String foodLocation;
    public String parkingInfo;
    public String clinicName;
    public String clinicStreet1;
    public String clinicStreet2;
    public String clinicPhone;
    public String clinicCity;
    public String clinicState;
    public String clinicLat;
    public String clinicLon;
    public String clinicZip;
    public String clinicPtr;
    public String vetPtr;
    public String vetName;
    public String vetCity;
    public String vetPhone;
    public String vetStreet1;
    public String vetStreet2;
    public String vetZip;
    public String vetState;
    public String vetLat;
    public String vetLon;
    public String vetEmail;
    public String clientNotes;
    public String directions;

    public ArrayList<FlagItem> clientFlags = new ArrayList<>();
    public List<PetsForClient> petList = new ArrayList<>();
    public Map<String,String> customClientFields = new HashMap<>();
    public ArrayList<Map<String,Map<String,String>>> customClientFieldsArray = new ArrayList<>();
    public Map<String,Map<String,Object>> customClientFieldsSort = new HashMap<>();
    public ArrayList<Map<String,String>> errataDoc = new ArrayList<>();

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


    public void                     addClientData(Map<String,String> clientInformation) {

        clientID =clientInformation.get("clientid");
        clientNotes = validateClientDataField(clientInformation.get("notes"));
        directions = validateClientDataField(clientInformation.get("directions"));
        parkingInfo = validateClientDataField(clientInformation.get("parkinginfo"));
        cellphone = validateClientDataField(clientInformation.get("cellphone"));
        cellphone2 = validateClientDataField(clientInformation.get("cellphone2"));
        workphone = validateClientDataField(clientInformation.get("workphone"));
        homePhone = validateClientDataField(clientInformation.get("homephone"));

        clientName = validateClientDataField(clientInformation.get("clientname"));

        //System.out.println("Client name: " + clientName + ", id: " + clientID);
        sortName = validateClientDataField(clientInformation.get("sortname"));
        firstName = validateClientDataField(clientInformation.get("fname"));
        firstName2 = validateClientDataField(clientInformation.get("fname2"));
        lastName = validateClientDataField(clientInformation.get("lname"));
        lastName2 = validateClientDataField(clientInformation.get("lname2"));
        street1 = validateClientDataField(clientInformation.get("street1"));
        street2 = validateClientDataField(clientInformation.get("street2"));
        city = validateClientDataField(clientInformation.get("city"));
        state =validateClientDataField(clientInformation.get("state"));
        zip = validateClientDataField(clientInformation.get("zip"));
        garageGateCode = validateClientDataField(clientInformation.get("garagegatecode"));
        alarmCompany = validateClientDataField(clientInformation.get("alarmcompany"));
        alarmCompanyPhone = validateClientDataField(clientInformation.get("alarmcophone"));
        alarmInfo = validateClientDataField(clientInformation.get("alarminfo"));
        // clinicptr
        // vetptr
        leashLocation = validateClientDataField(clientInformation.get("leashloc"));
        parkingInfo = validateClientDataField(clientInformation.get("parkinginfo"));
        foodLocation = validateClientDataField(clientInformation.get("foodloc"));

        noKeyRequired = validateClientDataField(clientInformation.get("nokeyrequired"));
        keyID = validateClientDataField(clientInformation.get("keyid"));
        keyDescriptionText = validateClientDataField(clientInformation.get("keydescription"));
        useKeyDescriptionInstead = validateClientDataField(clientInformation.get("showkeydescriptionnotkeyid"));
        hasKey = validateClientDataField(clientInformation.get("hasKey"));

        email = validateClientDataField(clientInformation.get("email"));
        email2 = validateClientDataField(clientInformation.get("email2"));

        clinicPtr = validateClientDataField(clientInformation.get("clinicptr"));
        vetPtr = validateClientDataField(clientInformation.get("vetptr"));
        clinicName = validateClientDataField(clientInformation.get("clinicname"));
        clinicStreet1 = validateClientDataField(clientInformation.get("clinicstreet1"));
        clinicStreet2 = validateClientDataField(clientInformation.get("clinicstreet2"));
        clinicZip = validateClientDataField(clientInformation.get("cliniczip"));
        clinicCity = validateClientDataField(clientInformation.get("cliniccity"));
        clinicState = validateClientDataField(clientInformation.get("clinicState"));
        clinicPhone = validateClientDataField(clientInformation.get("clinicphone"));
        clinicLat = validateClientDataField(clientInformation.get("clinicLat"));
        clinicLon = validateClientDataField(clientInformation.get("clinicLon"));

        vetName = validateClientDataField(clientInformation.get("vetname"));
        vetStreet1 = validateClientDataField(clientInformation.get("vetstreet1"));
        vetStreet2 = validateClientDataField(clientInformation.get("vetstreet2"));
        vetCity = validateClientDataField(clientInformation.get("vetcity"));
        vetState = validateClientDataField(clientInformation.get("vetstate"));
        vetZip = validateClientDataField(clientInformation.get("vetzip"));
        vetPhone = validateClientDataField(clientInformation.get("vetphone"));
        vetLat = validateClientDataField(clientInformation.get("vetLat"));
        vetLon = validateClientDataField(clientInformation.get("vetLon"));

    }
    public Map<String, Object>  jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if(!Objects.equals(json, JSONObject.NULL)) {
            retMap = toMap(json);
        }
        return retMap;
    }
    private Map<String, Object> toMap(JSONObject object) throws JSONException {
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
    private List<Object> toList(JSONArray array) throws JSONException {
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

}



