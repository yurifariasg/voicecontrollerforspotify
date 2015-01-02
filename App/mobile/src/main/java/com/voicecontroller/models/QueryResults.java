package com.voicecontroller.models;

import android.os.Bundle;
import android.os.Parcelable;

import com.voicecontroller.utils.SpotifyWebAPI;
import org.json.JSONException;
import java.io.IOException;

public class QueryResults {

    private String id;
    private String uri;
    private String name; // Artist name or Track name or Playlist name
    private String subtitle; // Artist name, if a track or other optional info
    private byte[] image; // Artist image or track image or playlist image
    private QueryType type;
    private VoiceQuery query;

    private Track[] tracks;

    public QueryResults(String id, String uri, String name, byte[] image, QueryType type) {
        this(id, uri, name, null, image, type, null);
    }

    public QueryResults(String id, String uri, String name, String subtitle, byte[] image, QueryType type) {
        this(id, uri, name, subtitle, image, type, null);
    }

    public QueryResults(String id, String uri, String name, String subtitle, byte[] image, QueryType type, Track[] tracks) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.subtitle = subtitle;
        this.image = image;
        this.type = type;
        this.tracks = tracks;
    }

    public void setVoiceQuery(VoiceQuery query) {
        this.query = query;
    }

    public VoiceQuery getQuery() {
        return query;
    }

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public QueryType getType() {
        return type;
    }

    public Track[] getTracks() {
        return tracks;
    }

    public void fetchTracks(Profile profile) throws IOException, JSONException {
        // If it is artists or playlist, fetch tracks.
        if (type.equals(QueryType.ARTIST)) {
            tracks = SpotifyWebAPI.getTopTracksForArtist(id, profile.countryCode);
        } else if (type.equals(QueryType.PLAYLIST)) {
            tracks = SpotifyWebAPI.getPlaylistTracks(getId(), profile);
        }
    }

    public VoiceQueryResult toQueryResult() {
        return new VoiceQueryResult(id, name, subtitle, image);
    }

    public static QueryResults fromBundle(Bundle b) {
        String id = b.getString("id");
        String uri = b.getString("uri");
        String name = b.getString("name");
        String subtitle = b.getString("subtitle");
        QueryType type = QueryType.valueOf(b.getString("type"));
        byte[] image = b.getByteArray("image");
        Parcelable[] parsedTracks = b.getParcelableArray("tracks");
        Track[] tracks = null;
        if (parsedTracks != null) {
            tracks = new Track[parsedTracks.length];
            for (int i = 0; i < tracks.length; i++) {
                tracks[i] = Track.fromBundle((Bundle) parsedTracks[i]);
            }
        }
        QueryResults results = new QueryResults(id, uri, name, subtitle, image, type, tracks);

        Bundle queryBundle = b.getBundle("query");
        if (queryBundle != null) {
            results.setVoiceQuery(VoiceQuery.fromBundle(queryBundle));
        }
        return results;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString("id", id);
        b.putString("uri", uri);
        b.putString("name", name);
        b.putString("subtitle", subtitle);
        b.putByteArray("image", image);
        b.putString("type", type.toString());
        if (query != null) {
            b.putBundle("query", query.toBundle());
        }

        if (tracks != null) {
            Parcelable[] parsedTracks = new Parcelable[tracks.length];
            for (int i = 0 ; i < tracks.length ; i++) {
                parsedTracks[i] = tracks[i].toBundle();
            }
            b.putParcelableArray("tracks", parsedTracks);
        }

        return b;
    }
}
