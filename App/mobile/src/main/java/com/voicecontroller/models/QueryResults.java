package com.voicecontroller.models;


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

    public void fetchTracks() {
        // If it is artists or playlist, fetch tracks.
    }

    public VoiceQueryResult toQueryResult() {
        return new VoiceQueryResult(id, name, subtitle, image);
    }
}
