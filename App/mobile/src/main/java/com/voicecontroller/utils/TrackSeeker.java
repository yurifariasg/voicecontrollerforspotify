package com.voicecontroller.utils;


import android.os.AsyncTask;
import android.util.Log;

import com.voicecontroller.callbacks.OnTrackFoundCallback;
import com.voicecontroller.models.Track;

import org.json.JSONArray;
import org.json.JSONObject;

public class TrackSeeker extends AsyncTask<String, String, Track> {

    private OnTrackFoundCallback trackFoundCallback;

    public TrackSeeker(OnTrackFoundCallback trackFoundCallback) {
        this.trackFoundCallback = trackFoundCallback;
    }

    @Override
    protected Track doInBackground(String... params) {

        Track track = null;
        try {
//            JSONObject response = SpotifyWebAPI.searchTrack(params[0]);

//            JSONArray items = response.getJSONObject("tracks").getJSONArray("items");

//            if (items.length() > 0) {
//                track = new Track(items.getJSONObject(0));
//            }


        } catch (Exception exception) {
            Log.w("TrackSeeker", "Failed to Search Track: " + exception.getLocalizedMessage());
        }

        return track;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Track s) {
        trackFoundCallback.onTrackFound(s);
    }

    @Override
    protected void onCancelled(Track s) {
        super.onCancelled(s);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }
}
