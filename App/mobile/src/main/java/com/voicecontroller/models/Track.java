package com.voicecontroller.models;

import com.google.android.gms.wearable.DataMap;

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

    public DataMap toDataMap() {

        DataMap data = new DataMap();

        data.putString("artist", getArtist());
        data.putString("name", getName());
        data.putString("uri", getUri());
        data.putByteArray("image", image);

        return data;

    }

}
