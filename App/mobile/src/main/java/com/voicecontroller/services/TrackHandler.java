package com.voicecontroller.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.voicecontroller.models.Track;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.utils.SpotifyWebAPI;


public class TrackHandler {

    public static Track lookForTrack(String queryName, WearableConnection connection, Context context) {

        try {

            Track track = null;
            int retries = 0;
            while (track == null && retries < Settings.SEARCH_TRACK_MAXIMUM_RETRIES) {
                track = SpotifyWebAPI.searchTrack(queryName, context);
                retries++;
            }

            if (track != null) {
                connection.requestConfirmation(track);
            } else {
                throw new Exception("Failed to find track with maximum number of retries...");
            }
            return track;

        } catch (Exception exception) {
            Log.w("TrackHandler", "Failed to Search Track: " + exception.getLocalizedMessage());
            connection.errorOccurred();
            Crashlytics.logException(exception);
        }
        return null;
    }

    public static void playTrack(String trackUri, WearableConnection connection, Context context) {
        Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse(trackUri) );
        intent.setFlags((Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND));
        context.startActivity(intent);
    }
}
