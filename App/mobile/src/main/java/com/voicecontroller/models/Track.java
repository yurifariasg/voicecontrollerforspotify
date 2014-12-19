package com.voicecontroller.models;


import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;


public class Track {

    private String uri;
    private String id;
    private String name;
    private String artist;
    private byte[] image;


    public Track(String id, String name, String artist, String uri, byte[] imageBytes) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.uri = uri;
        this.image = imageBytes;
    }

    public String getUri() {
        return uri;
    }

    public String getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getArtist() + " - " + getName();
    }

    public String toJSONString() throws JSONException {

        String imgStr = "";
        if (image != null) {
            imgStr = Base64.encodeToString(image, Base64.DEFAULT);
        }

        JSONObject json = new JSONObject();
        json.put("artist", getArtist());
        json.put("name", getName());
        json.put("image", imgStr);
        json.put("uri", getUri());

        return json.toString();
    }
}
