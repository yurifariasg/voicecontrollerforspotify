package com.voicecontroller.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.voicecontroller.models.Track;
import com.voicecontroller.utils.SpotifyWebAPI;


public class TrackHandler {

    public static void lookForTrack(String queryName, WearableConnection connection, Context context) {

        try {
            Track track = SpotifyWebAPI.searchTrack(queryName, context);
            connection.requestConfirmation(track);

        } catch (Exception exception) {
            Log.w("TrackHandler", "Failed to Search Track: " + exception.getLocalizedMessage());
            connection.errorOccurred();
        }
    }

    public static void playTrack(String trackUri, WearableConnection connection, Context context) {
        Intent launcher = new Intent( Intent.ACTION_VIEW, Uri.parse(trackUri) );
        launcher.setFlags((Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND));
        context.startActivity(launcher);
        Log.i("TrackHandler", "Sending tune to Spotify.");
    }
}
