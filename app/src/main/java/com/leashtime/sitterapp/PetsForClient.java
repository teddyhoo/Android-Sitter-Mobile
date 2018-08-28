package com.leashtime.sitterapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PetsForClient {

    public String petid;
    public String name;
    public String breed;
    public String age;
    public String color;
    public String description;
    public String notes;
    public String fixed;
    public String gender;

    public List<Map<String,String>> petDocsAttach = new ArrayList<>();
    public List<Map<String,String>> petCustomFields = new ArrayList<>();
    public HashMap<String,HashMap<String,Object>> petCustomFieldsSort = new HashMap<>();

    public static String validatePetField(Object field) {

        if(null != field) {
            Class stringClass = field.getClass();
            String stringForClass = stringClass.getName();
            if(stringForClass.equals("java.lang.String")) {
                return (String)field;
            } else {
                return "NULL VALUE";
            }
        } else {
            return "NULL VALUE";
        }

    }

    public void InitPetsForClient(Map<String,String> petInfo) {
        this.petid = petInfo.get("petid");
        this.name = petInfo.get("name");
        this.description= validatePetField(petInfo.get("description"));
        this.notes = validatePetField(petInfo.get("notes"));
        this.breed = validatePetField(petInfo.get("breed"));
        this.age = validatePetField(petInfo.get("birthday"));
        this.color = validatePetField(petInfo.get("color"));
        this.gender = validatePetField(petInfo.get("sex"));
        String isFixed = petInfo.get("fixed");

        if(isFixed.equals("Yes")) {
            this.fixed = "Fixed";
        } else {
            this.fixed = "Not Fixed";
        }

    }

    public void addPetBasicField(String key, String val) {}


    public void addPetDocuments(Map<String,Map<String,String>> petDocs) {

        HashMap<String,String> customPetDic = new HashMap<>();

        Iterator customDocIterator = petDocs.entrySet().iterator();
        int errataCounter = 1;
        while (customDocIterator.hasNext()) {
            Map.Entry customDocMap = (Map.Entry) customDocIterator.next();
            String customDocKey = (String)customDocMap.getKey();
            Object customDocVal = customDocMap.getValue();
            HashMap<String,String> customDocInfo = (HashMap)customDocVal;
            customPetDic.put("url",customDocInfo.get("url"));
            customPetDic.put("mimetype", customDocInfo.get("mimetype"));
            customPetDic.put("label", customDocInfo.get("label"));
            customPetDic.put("petid", this.petid);
            customPetDic.put("type","docAttach");
            customPetDic.put("fieldlabel",customDocKey);
            String errataIndexString = String.valueOf(errataCounter);
            customPetDic.put("errataIndex",errataIndexString);
            petDocsAttach.add(customDocInfo);
            errataCounter = errataCounter + 1;
        }

    }


}

