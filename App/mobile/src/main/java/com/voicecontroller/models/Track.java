package com.voicecontroller.models;

import android.os.Bundle;

import com.google.android.gms.wearable.DataMap;

public class Track {

    private String uri;
    private String id;
    private String name;
    private String artist;
    private byte[] image;
    private byte[] blurredImage;

    public static Track fromBundle(Bundle b) {
        if (b == null) {
            return null;
        } else {
            return new Track(b.getString("id"), b.getString("name"), b.getString("artist"),
                    b.getString("uri"), b.getByteArray("image"));
        }
    }

    public Track(String id, String name, String artist, String uri, byte[] imageBytes, byte[] blurredImage) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.uri = uri;
        this.image = imageBytes;
        this.blurredImage = blurredImage;
    }

    public Track(String id, String name, String artist, String uri, byte[] imageBytes) {
        this(id, name, artist, uri, imageBytes, null);
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

    public byte[] getImage() { return image; }

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
        if (blurredImage != null) {
            data.putByteArray("image_blurred", blurredImage);
        }
        return data;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString("id", getId());
        b.putString("artist", getArtist());
        b.putString("name", getName());
        b.putString("uri", getUri());
        b.putByteArray("image", image);
        if (blurredImage != null) {
            b.putByteArray("blurred_image", blurredImage);
        }
        return b;
    }

}
