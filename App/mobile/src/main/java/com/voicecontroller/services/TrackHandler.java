package com.voicecontroller.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.voicecontroller.exceptions.NoTrackFoundException;
import com.voicecontroller.models.QueryResults;
import com.voicecontroller.models.Track;
import com.voicecontroller.models.VoiceQuery;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.utils.SpotifyWebAPI;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class TrackHandler {

    public static QueryResults lookForTrack(VoiceQuery query, WearableConnection connection, Context context) throws NoTrackFoundException, JSONException, UnsupportedEncodingException {

        QueryResults results = null;
        int retries = 0;
        boolean shouldRetry = true;
        while (shouldRetry && retries < Settings.SEARCH_TRACK_MAXIMUM_RETRIES) {
            try {
                results = SpotifyWebAPI.search(query.getQuery(), query.getType());
                shouldRetry = false;
            } catch (IOException e) {
                retries++;
            }
        }

        if (results != null) {
            results.setVoiceQuery(query);
            connection.requestConfirmation(results.toQueryResult(), context);
        } else {
            throw new NoTrackFoundException();
        }
        return results;
    }

    public static void playTrack(String trackUri, WearableConnection connection, Context context) {
        Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse(trackUri) );
        intent.setFlags((Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND));
        context.startActivity(intent);
    }
}
