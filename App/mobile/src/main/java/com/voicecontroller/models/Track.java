package com.voicecontroller.models;

import android.os.Bundle;
import android.util.Base64;
import com.google.android.gms.wearable.DataMap;
import com.orm.SugarRecord;

public class Track extends SugarRecord<Track> {

    String uri;
    String spotifyId;
    String name;
    String artist;
    String image;

    // Optional value for a playlist.
    public Playlist playlist;

    public static Track fromBundle(Bundle b) {
        if (b == null) {
            return null;
        } else {
            return new Track(b.getString("spotifyId"), b.getString("name"), b.getString("artist"),
                    b.getString("uri"), b.getByteArray("image"));
        }
    }

    public Track(String spotifyId, String name, String artist, String uri, byte[] imageBytes) {
        this.spotifyId = spotifyId;
        this.name = name;
        this.artist = artist;
        this.uri = uri;
        setImage(imageBytes);
    }

    public Track() {
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

    public String getSpotifyId() {
        return spotifyId;
    }

    public String getUri() {
        return uri;
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
        data.putString("spotifyId", getSpotifyId());
        data.putString("artist", getArtist());
        data.putString("name", getName());
        data.putString("uri", getUri());
        data.putByteArray("image", getImage());
        return data;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString("spotifyId", getSpotifyId());
        b.putString("artist", getArtist());
        b.putString("name", getName());
        b.putString("uri", getUri());
        b.putByteArray("image", getImage());
        return b;
    }
}
