package com.voicecontroller.models;


import android.os.Bundle;
import android.util.Log;

import com.voicecontroller.settings.Settings;
import com.voicecontroller.utils.GeneralUtils;

import java.util.HashMap;

public class VoiceQuery {

    private static String[] toArray(String... params) {
        return params;
    }

    public static final String ENQUEUE_KEYWORD_KEY = "enqueue";
    public static final String ARTIST_KEYWORD_KEY = "artist";
    public static final String PLAYLIST_KEYWORD_KEY = "playlist";
    public static final String TRACK_KEYWORD_KEY = "track";
    public static final String REPEAT_KEYWORD_KEY = "repeat";
    public static final String SHUFFLE_KEYWORD_KEY = "shuffle";

    public static final HashMap<String, String[]> KEYWORDS;
    static {
        KEYWORDS = new HashMap<>();

        /* These should be updated depending on language */
        /* Always remember to put composed-phrases before */
        KEYWORDS.put(ENQUEUE_KEYWORD_KEY, toArray("next"));
        KEYWORDS.put(ARTIST_KEYWORD_KEY, toArray("artist"));
        KEYWORDS.put(PLAYLIST_KEYWORD_KEY, toArray("playlist"));
        KEYWORDS.put(TRACK_KEYWORD_KEY, toArray("song", "track"));
        KEYWORDS.put(REPEAT_KEYWORD_KEY, toArray("on repeat", "repeated", "repeat"));
        KEYWORDS.put(SHUFFLE_KEYWORD_KEY, toArray("on shuffle", "shuffled", "shuffle"));
    }

    private String query = null;
    private QueryType type = QueryType.DEFAULT;
    private boolean enqueue = false;
    private boolean shuffle = false;
    private boolean repeat = false;

    public VoiceQuery(String rawQuery) {
        parseRawQuery(rawQuery);
    }

    private VoiceQuery() {}

    public static VoiceQuery fromBundle(Bundle b) {
        VoiceQuery query = new VoiceQuery();
        query.enqueue = b.getBoolean("enqueue");
        query.query = b.getString("query");
        query.shuffle = b.getBoolean("shuffle");
        query.repeat = b.getBoolean("repeat");
        query.type = QueryType.valueOf(b.getString("type"));
        return query;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString("query", query);
        b.putString("type", type.toString());
        b.putBoolean("enqueue", enqueue);
        b.putBoolean("repeat", repeat);
        b.putBoolean("shuffle", shuffle);
        return b;
    }

    public String getQuery() {
        return query;
    }

    public QueryType getType() {
        return type;
    }

    public boolean shouldEnqueue() {
        return enqueue;
    }

    public boolean shouldRepeat() { return repeat; }

    public boolean shouldShuffle() { return shuffle; }

    private void parseRawQuery(String rawQuery) {
        this.query = rawQuery.toLowerCase().trim();

        boolean keepExtracting = true;
        while (keepExtracting) {
            keepExtracting = extractAndUpdate();
        }

    }

    private boolean extractAndUpdate() {
        for (String key : KEYWORDS.keySet()) {
            String matchedPrefix = GeneralUtils.startWithAny(query, KEYWORDS.get(key));
            if (matchedPrefix != null) {
                handleNewProperty(key);
                query = query.substring(matchedPrefix.length()).trim();
                return true;
            } else {
                // Try to get suffix
                String matchedSuffix = GeneralUtils.endWithAny(query, KEYWORDS.get(key));
                if (matchedSuffix != null) {
                    handleNewProperty(key);
                    query = query.substring(0, query.lastIndexOf(matchedSuffix)).trim();
                    return true;
                }
            }
        }
        return false;
    }

    private void updateType(QueryType newType) {
        if (type == null || type.equals(QueryType.DEFAULT)) {
            type = newType;
        }
    }

    private void handleNewProperty(String keyword_key) {
        switch (keyword_key) {
            case ENQUEUE_KEYWORD_KEY:
                enqueue = true;
                break;
            case ARTIST_KEYWORD_KEY:
                updateType(QueryType.ARTIST);
                break;
            case TRACK_KEYWORD_KEY:
                updateType(QueryType.TRACK);
                break;
            case PLAYLIST_KEYWORD_KEY:
                updateType(QueryType.PLAYLIST);
                break;
            case REPEAT_KEYWORD_KEY:
                repeat = true;
                break;
            case SHUFFLE_KEYWORD_KEY:
                shuffle = true;
                break;
            default:
                break;
        }
    }

}
