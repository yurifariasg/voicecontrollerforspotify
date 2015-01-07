package com.voicecontroller.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.orm.query.Condition;
import com.orm.query.Select;
import com.voicecontroller.R;
import com.voicecontroller.models.Playlist;
import com.voicecontroller.models.Profile;
import com.voicecontroller.models.QueryResults;
import com.voicecontroller.models.QueryType;
import com.voicecontroller.models.VoiceQuery;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.utils.SpotifyWebAPI;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;


public class TrackHandler {

    public static QueryResults lookForTrack(VoiceQuery query, WearableConnection connection, Context context) throws JSONException, UnsupportedEncodingException {

        QueryResults results = null;
        if (query.getType().equals(QueryType.PLAYLIST)) {
            results = lookProfilePlaylists(query);
        } else if (query.getType().equals(QueryType.DEFAULT)) {
            results = lookProfilePlaylists(query);
            if (results == null) {
                results = searchTrack(query);
            }
        } else {
            results = searchTrack(query);
        }

        if (results != null) {
            results.setVoiceQuery(query);
            connection.requestConfirmation(results.toQueryResult(), context);
        } else {
            connection.sendAlert(context.getString(R.string.no_results_title), context.getString(R.string.no_results_description));
        }
        return results;
    }

    private static QueryResults lookProfilePlaylists(VoiceQuery query) {
        OAuthRecord record = OAuthService.getOAuthToken();
        if (!record.isValid()) {
            SpotifyWebAPI.refreshOAuth(record);
        }
        Profile profile = SpotifyWebAPI.getUserProfile(record, false);

        List<Playlist> playlists = Select.from(Playlist.class).where(Condition.prop("PROFILE").eq(profile.getId())).list();

        Playlist mostSimilarPlaylist = null;
        float mostSimilarVal = 0;
        float minimumSimilarityVal = Settings.getSimilarityValue();

        AbstractStringMetric similarityMetric = new Levenshtein();
        String queryWithAlphaOnly = query.getQuery().replaceAll("[^a-zA-Z]+"," ").toLowerCase();

        for (Playlist p : playlists) {
            float similarityVal = similarityMetric.getSimilarity(queryWithAlphaOnly, p.nameForQuery.toLowerCase());

            if (similarityVal >= minimumSimilarityVal && similarityVal > mostSimilarVal) {
                mostSimilarPlaylist = p;
                mostSimilarVal = similarityVal;
            }
        }

        if (mostSimilarPlaylist != null) {

            String subtitle = (mostSimilarPlaylist.name.equals(mostSimilarPlaylist.nameForQuery)) ? null : mostSimilarPlaylist.nameForQuery;

            return new QueryResults(mostSimilarPlaylist.spotifyId, mostSimilarPlaylist.spotifyUri,
                    mostSimilarPlaylist.name, subtitle, mostSimilarPlaylist.getImage(), QueryType.PLAYLIST);
        } else {
            return null;
        }
    }

    private static QueryResults searchTrack(VoiceQuery query) throws JSONException {
        int retries = 0;
        QueryResults results = null;
        boolean shouldRetry = true;
        while (shouldRetry && retries < Settings.SEARCH_TRACK_MAXIMUM_RETRIES) {
            try {
                results = SpotifyWebAPI.search(query.getQuery(), query.getType());
                shouldRetry = false;
            } catch (IOException e) {
                retries++;
            }
        }
        return results;
    }

    public static void playTrack(String trackUri, WearableConnection connection, Context context) {
        Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse(trackUri) );
        intent.setFlags((Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND));
        context.startActivity(intent);
    }
}
