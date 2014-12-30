package com.voicecontroller.models;


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

    public static final HashMap<String, String[]> KEYWORDS;
    static {
        KEYWORDS = new HashMap<>();

        /* These should be updated depending on language */
        KEYWORDS.put(ENQUEUE_KEYWORD_KEY, toArray("next"));
        KEYWORDS.put(ARTIST_KEYWORD_KEY, toArray("artist"));
        KEYWORDS.put(PLAYLIST_KEYWORD_KEY, toArray("playlist"));
        KEYWORDS.put(TRACK_KEYWORD_KEY, toArray("song", "track"));
    }

    private String query = null;
    private QueryType type = QueryType.DEFAULT;
    private boolean enqueue = false;

    public VoiceQuery(String rawQuery) {
        parseRawQuery(rawQuery);
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
            default:
                break;
        }
    }

}
