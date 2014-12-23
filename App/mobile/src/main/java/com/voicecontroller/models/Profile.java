package com.voicecontroller.models;

import android.util.Base64;

import com.orm.SugarRecord;


public class Profile extends SugarRecord<Profile> {

    public String name;
    public String image;

    public Profile() {
    }

    public Profile(String name, String image) {
        this.name = name;
        this.image = image;
    }


    // The two helper functions below are here because SugarRecord does not handle well byte[] type
    public void setImage(byte[] imgBytes) {
        this.image = Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    public byte[] getImage() {
        if (image != null && !image.isEmpty()) {
            return Base64.decode(this.image, Base64.DEFAULT);
        }
        return null;
    }

}
