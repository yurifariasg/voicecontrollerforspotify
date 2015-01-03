package com.voicecontroller.models;


import android.os.Bundle;

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

    public static final String RESUME_KEYWORD_KEY = "resume";
    public static final String PAUSE_KEYWORD_KEY = "pause";
    public static final String SKIP_KEYWORD_KEY = "skip";
    public static final String PREVIOUS_KEYWORD_KEY = "previous";

    public static final HashMap<String, String[]> KEYWORDS_COMMANDS;
    static {
        KEYWORDS_COMMANDS = new HashMap<>();

        KEYWORDS_COMMANDS.put(RESUME_KEYWORD_KEY, toArray("resume", "play", "resume track", "resume song"));
        KEYWORDS_COMMANDS.put(PAUSE_KEYWORD_KEY, toArray("pause", "stop"));
        KEYWORDS_COMMANDS.put(SKIP_KEYWORD_KEY, toArray("skip", "next"));
        KEYWORDS_COMMANDS.put(PREVIOUS_KEYWORD_KEY, toArray("previous", "go back"));
    }

    public static final String[] COMMAND_SUFFIX = toArray("", " track", " music", " song", " playback");

    public static final HashMap<String, String[]> KEYWORDS_MODIFIERS;
    static {
        KEYWORDS_MODIFIERS = new HashMap<>();

        /* These should be updated depending on language */
        /* Always remember to put composed-phrases before */
        KEYWORDS_MODIFIERS.put(ENQUEUE_KEYWORD_KEY, toArray("next"));
        KEYWORDS_MODIFIERS.put(ARTIST_KEYWORD_KEY, toArray("artist"));
        KEYWORDS_MODIFIERS.put(PLAYLIST_KEYWORD_KEY, toArray("playlist"));
        KEYWORDS_MODIFIERS.put(TRACK_KEYWORD_KEY, toArray("song", "track"));
        KEYWORDS_MODIFIERS.put(REPEAT_KEYWORD_KEY, toArray("on repeat", "repeated", "repeat"));
        KEYWORDS_MODIFIERS.put(SHUFFLE_KEYWORD_KEY, toArray("on shuffle", "shuffled", "shuffle"));
    }

    private String query = null;
    private QueryType type = QueryType.DEFAULT;
    private boolean enqueue = false;
    private boolean shuffle = false;
    private boolean repeat = false;

    private boolean isMediaCommand = false;
    private MediaCommandType mediaCommandType = null;

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

    public boolean isMediaCommand() {
        return isMediaCommand;
    }

    public boolean shouldRepeat() { return repeat; }

    public boolean shouldShuffle() { return shuffle; }

    public MediaCommandType getMediaCommand() {
        return mediaCommandType;
    }

    private void parseRawQuery(String rawQuery) {
        this.query = rawQuery.toLowerCase().trim();

        extractMediaCommands();

        if (!isMediaCommand) {
            boolean keepExtracting = true;
            while (keepExtracting) {
                keepExtracting = extractAndUpdate();
            }
        }

    }

    private void extractMediaCommands() {
        for (String key : KEYWORDS_COMMANDS.keySet()) {
            for (String command : KEYWORDS_COMMANDS.get(key)) {
                for (String suffix : COMMAND_SUFFIX) {
                    if (query.equalsIgnoreCase(command + suffix)) {
                        handleMediaCommand(key);
                    }
                }
            }
        }
    }

    private void handleMediaCommand(String commandKey) {
        if (mediaCommandType == null) {
            switch (commandKey) {
                case RESUME_KEYWORD_KEY:
                    mediaCommandType = MediaCommandType.RESUME;
                    break;
                case PAUSE_KEYWORD_KEY:
                    mediaCommandType = MediaCommandType.PAUSE;
                    break;
                case SKIP_KEYWORD_KEY:
                    mediaCommandType = MediaCommandType.SKIP;
                    break;
                case PREVIOUS_KEYWORD_KEY:
                    mediaCommandType = MediaCommandType.PREVIOUS;
                    break;
                default:
                    break;
            }
        }
        if (mediaCommandType != null) {
            isMediaCommand = true;
        }
    }

    private boolean extractAndUpdate() {
        for (String key : KEYWORDS_MODIFIERS.keySet()) {
            String matchedPrefix = GeneralUtils.startWithAny(query, KEYWORDS_MODIFIERS.get(key));
            if (matchedPrefix != null) {
                handleNewProperty(key);
                query = query.substring(matchedPrefix.length()).trim();
                return true;
            } else {
                // Try to get suffix
                String matchedSuffix = GeneralUtils.endWithAny(query, KEYWORDS_MODIFIERS.get(key));
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
