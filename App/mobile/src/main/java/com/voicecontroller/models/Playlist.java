package com.voicecontroller.models;


import android.util.Base64;

import com.orm.SugarRecord;

public class Playlist extends SugarRecord<Playlist> {

    public Profile profile;
    public String name;
    public String image;
    public String spotifyId;
    public String spotifyUri;
    public int tracksCount;

    public Playlist() {
    }

    // The two helper functions below are here because SugarRecord does not handle well byte[] type
    public void setImage(byte[] imgBytes) {
        if (imgBytes != null) {
            this.image = Base64.encodeToString(imgBytes, Base64.DEFAULT);
        }
    }

    public byte[] getImage() {
        if (image != null && !image.isEmpty()) {
            return Base64.decode(this.image, Base64.DEFAULT);
        }
        return null;
    }

}
