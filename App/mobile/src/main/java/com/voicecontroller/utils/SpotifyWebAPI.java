package com.voicecontroller.utils;


import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.voicecontroller.callbacks.OnOAuthTokenRefreshed;
import com.voicecontroller.models.Playlist;
import com.voicecontroller.models.QueryResults;
import com.voicecontroller.models.QueryType;
import com.voicecontroller.callbacks.OnProfileAcquired;
import com.voicecontroller.models.Profile;
import com.voicecontroller.models.Track;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;
import com.voicecontroller.settings.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SpotifyWebAPI {

    public static final String CLIENT_ID = "a4a0cd5140774f89afd25d2e4dfd4e19";
    public static final String ENCODED = "YTRhMGNkNTE0MDc3NGY4OWFmZDI1ZDJlNGRmZDRlMTk6ZGUzY2FmZDUxMDg4NGRmMTlhZGQ0MWJhNjFjODk4NGE=";
    private static final String REDIRECT_URI = "voice-controller-spotify://callback";
    private static final String BASE_URL = "https://api.spotify.com/v1/";
    private static final String ACCOUNTS_URL = "https://accounts.spotify.com/api/token";
    private static final String ARTISTS_BASE = "artists/";
    private static final String USERS_BASE = "users/";
    private static final String TOP_TRACKS_ENDPOINT = "/top-tracks";
    private static final String PLAYLISTS_ENDPOINT = "/playlists";
    private static final String SEARCH_API = "search";
    private static final String USER_PROFILE = "me";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final int MINIMUM_WIDTH = 200;

    public static void refreshOAuth(OAuthRecord record, final OnOAuthTokenRefreshed callback) {
        new AsyncTask<OAuthRecord, OAuthRecord, OAuthRecord>() {

            @Override
            protected OAuthRecord doInBackground(OAuthRecord... params) {
                refreshOAuth(params[0]);
                return params[0];
            }

            @Override
            protected void onPostExecute(OAuthRecord newRecord) {
                callback.onOAuthTokenRefreshed(newRecord);
            }
        }.execute(record);
    }

    public static void refreshOAuth(OAuthRecord record, final OnProfileAcquired callback) {

        new AsyncTask<OAuthRecord, OAuthRecord, Profile>() {

            @Override
            protected Profile doInBackground(OAuthRecord... params) {

                refreshOAuth(params[0]);
                if (!params[0].isValid()) {
                    return null; // Nothing we can do here...
                }

                Profile p = Select.from(Profile.class).where(Condition.prop("oauth").eq(params[0].getId())).first();
                getAndSaveUserPlaylistAsync(p);
                return p;
            }

            @Override
            protected void onPostExecute(Profile profile) {
                callback.onProfileAcquired(profile);
            }
        }.execute(record);
    }

    public static void refreshOAuth(OAuthRecord record) {
        try {
            String authCode = "Basic " + ENCODED;
            String encodedParams = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(record.refresh_token, DEFAULT_ENCODING);

            // GET Access Code and Refresh Token
            String response = post(ACCOUNTS_URL, authCode, encodedParams);

            JSONObject json = new JSONObject(response);
            String accessToken = json.getString("access_token");
            int expiresIn = json.getInt("expires_in");

            record.expiration = System.currentTimeMillis() / 1000 + expiresIn;
            record.access_token = accessToken;
            record.save();
        } catch (Exception e) {
            Log.w("SpotifyWebAPI", "Failed to refresh token...", e);
            Crashlytics.logException(e);
        }
    }

    public static void callOAuthWindow(Activity activity) {
        // Initiate new OAuth Request...
        SpotifyAuthentication.openAuthWindow(CLIENT_ID, "code", REDIRECT_URI,
                Settings.SPOTIFY_USER_PERMISSIONS, null, activity);

    }

    public static void onOAuthCallback(Uri uri, final OnProfileAcquired callback) {
        new AsyncTask<Uri, OAuthRecord, OAuthRecord>() {

            @Override
            protected OAuthRecord doInBackground(Uri... params) {
                OAuthRecord record = null;
                String encodedParams = null;
                String code = null;
                String response = null;
                try {
                    code = params[0].getQueryParameter("code");
                    if (code != null) {
                        String authCode = "Basic " + ENCODED;

                        encodedParams = "grant_type=authorization_code&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, DEFAULT_ENCODING);
                        encodedParams += "&code=" + URLEncoder.encode(code, DEFAULT_ENCODING);

                        // GET Access Code and Refresh Token
                        response = post(ACCOUNTS_URL, authCode, encodedParams);

                        JSONObject json = new JSONObject(response);
                        String accessToken = json.getString("access_token");
                        String refresh_token = json.getString("refresh_token");
                        int expiresIn = json.getInt("expires_in");

                        record = new OAuthRecord();
                        record.expiration = System.currentTimeMillis() / 1000 + expiresIn;
                        record.access_token = accessToken;
                        record.refresh_token = refresh_token;
                        record.setPermissions(Settings.SPOTIFY_USER_PERMISSIONS);
                        record.save();
                    }

                } catch (Exception e) {
                    Log.w("SpotifyWebAPI", "Failed to get access and refresh tokens", e);
                    if (encodedParams != null) {
                        Crashlytics.setString("encodedParams", encodedParams);
                    }
                    if (code != null) {
                        Crashlytics.setString("code", code);
                    }
                    if (response != null) {
                        Crashlytics.setString("response", response);
                    }
                    Crashlytics.logException(e);
                }

                return record;
            }

            @Override
            protected void onPostExecute(OAuthRecord record) {
                if (record != null) {
                    getProfileAsync(record, callback);
                }
            }
        }.execute(uri);
    }

    public static void getProfileAsync(final OAuthRecord oauth, final OnProfileAcquired callback) {
        new AsyncTask<Void, String, Profile>() {

            @Override
            protected Profile doInBackground(Void... params) {
                Profile p = getUserProfile(oauth, false);
                if (p != null) {
                    getAndSaveUserPlaylistAsync(p);
                }
                return p;
            }

            @Override
            protected void onPostExecute(Profile profile) {
                super.onPostExecute(profile);
                if (profile != null) {
                    callback.onProfileAcquired(profile);
                }
            }
        }.execute();
    }

    public static Profile getUserProfile(OAuthRecord oauth, boolean forceRequest) {

        if (oauth == null) {
            oauth = OAuthService.getOAuthToken();
        }

        if (oauth != null) {
            Profile p = Select.from(Profile.class).where(Condition.prop("oauth").eq(oauth.getId())).first();
            boolean updateRequest = (p != null && forceRequest);
            if (p == null || forceRequest) {
                try {
                    String response = get(BASE_URL + USER_PROFILE, oauth.access_token);
                    JSONObject json = new JSONObject(response);

                    String name = json.getString("display_name");
                    if (name == null || json.isNull("display_name")) {
                        name = json.getString("id");
                    }
                    String product = json.getString("product");
                    String countryCode = json.getString("country");

                    String imgUrl = null;
                    JSONArray imgs = json.getJSONArray("images");
                    if (imgs.length() > 0) {
                        imgUrl = imgs.getJSONObject(0).getString("url");
                    }

                    if (p == null) {
                        p = new Profile();
                    }
                    p.name = name;
                    p.spotifyId = json.getString("id");
                    p.oauth = oauth;
                    p.product = product;
                    p.countryCode = countryCode;
                    if (imgUrl != null) {
                        p.setImage(downloadImage(imgUrl));
                    }
                    if (updateRequest) {
                        p.save();
                    }
                } catch (Exception e) {
                    Log.w("SpotifyWebAPI", "Could not get user profile", e);
                }
            }
            return p;
        }
        return null;
    }

    public static QueryResults search(String query, QueryType type) throws JSONException, IOException {
        Profile profile = null;
        OAuthRecord oauth = OAuthService.getOAuthToken();
        if (oauth != null) {
            if (!oauth.isValid()) {
                refreshOAuth(oauth);
            }
        }

        String url = BASE_URL + SEARCH_API + "?q=" + URLEncoder.encode(query, DEFAULT_ENCODING);

        switch (type) {
            case ARTIST:
                url += "&type=artist";
                break;
            case TRACK:
                url += "&type=track";
                break;
            default:
                url += "&type=track,artist";
                break;
        }

        String result;
        if (oauth != null) {
            result = get(url + "&market=from_token", oauth.access_token);
        } else {
            result = get(url, null);
        }
        JSONObject json = new JSONObject(result);

        if (json.has("artists") && json.getJSONObject("artists").getJSONArray("items").length() > 0) {

            JSONObject artistJson = json.getJSONObject("artists").getJSONArray("items").getJSONObject(0);

            byte[] img = getImageFromArray(artistJson.getJSONArray("images"));

            return new QueryResults(artistJson.getString("id"), artistJson.getString("uri"), artistJson.getString("name"), img, QueryType.ARTIST);

        } else if (json.has("tracks") && json.getJSONObject("tracks").getJSONArray("items").length() > 0) {

            JSONObject trackJson = json.getJSONObject("tracks").getJSONArray("items").getJSONObject(0);

            Track track = createTrackFromJSON(trackJson, null);
            Track[] tracks = new Track[] {track};
            return new QueryResults(track.getSpotifyId(), track.getUri(), track.getName(), track.getArtist(), track.getImage(), QueryType.TRACK, tracks);
        } else {
            return null;
        }
    }

    private static boolean isCountryInMarketsAvailable(String countryCode, JSONArray available_markets) throws JSONException {
        for (int i = 0 ; i < available_markets.length() ; i++) {
            if (available_markets.getString(i).equalsIgnoreCase(countryCode)) {
                return true;
            }
        }
        return false;
    }

    private static Track createTrackFromJSON(JSONObject trackJson, String countryCodeToFilter) {
        try {
            if (countryCodeToFilter != null) {
                // Check if we should parse this track in the first place...
                JSONArray jsonArray = trackJson.getJSONArray("available_markets");
                if (!isCountryInMarketsAvailable(countryCodeToFilter, jsonArray)) {
                    return null;
                }
            }

            String uri = trackJson.getString("uri");
            String id = trackJson.getString("id");
            String name = trackJson.getString("name");
            JSONArray artists = trackJson.getJSONArray("artists");
            String artist = "";
            if (artists.length() > 0) {
                artist = artists.getJSONObject(0).getString("name");
            }

            byte[] img = null;
            if (trackJson.has("album") && trackJson.getJSONObject("album").has("images")) {
                JSONArray images = trackJson.getJSONObject("album").getJSONArray("images");
                img = getImageFromArray(images);
            }

            return new Track(id, name, artist, uri, img);
        } catch (JSONException e) {
            Log.e(Settings.APP_TAG, "Failed to parse track", e);
            Crashlytics.setString("data", trackJson.toString());
            Crashlytics.logException(e);
            return null;
        }
    }

    public static Track[] getTopTracksForArtist(String artistId, String countryCode) throws IOException, JSONException {
        String url = BASE_URL + ARTISTS_BASE + artistId + TOP_TRACKS_ENDPOINT + "?country=" + countryCode;
        String response = get(url, null);
        JSONObject responseJson = new JSONObject(response);

        if (responseJson.has("tracks")) {

            JSONArray tracksJson = responseJson.getJSONArray("tracks");
            Track[] tracks = new Track[tracksJson.length()];

            for (int i = 0 ; i < tracksJson.length() ; i++) {
                tracks[i] = createTrackFromJSON(tracksJson.getJSONObject(i), null);
            }

            return tracks;
        } else {
            Crashlytics.log("Could not request top tracks: " + response);
            return null;
        }
    }

    public static void getAndSaveUserPlaylistAsync(final Profile userProfile) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    int now = (int) (System.currentTimeMillis() / 1000);
                    if (now - userProfile.lastPlaylistRefresh >= Settings.MINIMUM_TIME_BETWEEN_PLAYLIST_REFRESH) {
                        Playlist[] playlists = getUserPlaylist(userProfile);
                        userProfile.lastPlaylistRefresh = now;
                        userProfile.save();
                    }
                } catch (Exception e) {
                    Log.e(Settings.APP_TAG, "Exception", e);
                    Crashlytics.logException(e);
                }
                return null;
            }
        }.execute();
    }

    public static Track[] getPlaylistTracks(String playlistId, Profile profile) throws IOException, JSONException {

        Playlist playlist = Select.from(Playlist.class).where(Condition.prop("SPOTIFY_ID").eq(playlistId)).first();
        int now = (int) (System.currentTimeMillis() / 1000);
        if (playlist != null && now - playlist.lastPlaylistFetch <= Settings.MINIMUM_TIME_BETWEEN_PLAYLIST_TRACKS_REFRESH) {
            // If it is cached and valid get from db.
            List<Track> tracks = Select.from(Track.class).where(Condition.prop("PLAYLIST").eq(playlist.getId())).list();
            Track[] tracksArray = new Track[tracks.size()];
            return tracks.toArray(tracksArray);
        } else if (playlist != null) {
            Track.deleteAll(Track.class, "playlist = ?", String.valueOf(playlist.getId()));
        }

        String url = BASE_URL + USERS_BASE + profile.spotifyId + PLAYLISTS_ENDPOINT + "/" + playlistId + "/tracks";
        String response = get(url, profile.oauth.access_token);
        JSONObject jsonResponse = new JSONObject(response);
        int total = jsonResponse.getInt("total");
        int parsedTracks = 0;
        ArrayList<Track> tracks = new ArrayList<>();

        while (parsedTracks < total) {
            if (jsonResponse.has("items") && total > 0) {
                JSONArray items = jsonResponse.getJSONArray("items");
                if (items.length() == 0) {
                    break;
                }

                for (int i = 0 ; i < items.length() ; i++) {
                    Track t = createTrackFromJSON(items.getJSONObject(i).getJSONObject("track"), profile.countryCode);
                    if (t != null) {
                        tracks.add(t);
                    }
                    parsedTracks++;
                }
            } else if (!jsonResponse.has("items")) {
                break;
            }

            if (parsedTracks < total) {
                url = BASE_URL + USERS_BASE + profile.spotifyId + PLAYLISTS_ENDPOINT + "/" + playlistId + "/tracks?offset=" + parsedTracks;
                response = get(url, profile.oauth.access_token);
                jsonResponse = new JSONObject(response);
            }
        }
        if (playlist != null) {
            for (Track t : tracks) {
                t.playlist = playlist;
                t.save();
            }
            playlist.lastPlaylistFetch = now;
            playlist.save();
        }

        Track[] trackArray = new Track[tracks.size()];
        tracks.toArray(trackArray);
        return trackArray;
    }

    public static Playlist[] getUserPlaylist(Profile userProfile) throws IOException, JSONException {
        OAuthRecord record = userProfile.oauth;
        String userId = userProfile.spotifyId;

        String url = BASE_URL + USERS_BASE + userId + PLAYLISTS_ENDPOINT + "?limit=50";
        String response = get(url, record.access_token);
        JSONObject jsonResponse = new JSONObject(response);

        int total = jsonResponse.getInt("total");
        int playlistsParsed = 0;
        Playlist[] playlists = new Playlist[total];

        while (playlistsParsed < total) {

            if (jsonResponse.has("items") && total > 0) {
                JSONArray items = jsonResponse.getJSONArray("items");
                if (items.length() == 0) {
                    break;
                }

                for (int i = 0 ; i < items.length() ; i++) {
                    Playlist p = parsePlaylist(items.getJSONObject(i), userProfile);
                    playlists[playlistsParsed++] = p;
                }
            } else if (!jsonResponse.has("items")) {
                break;
            }

            if (playlistsParsed < total) {
                url = BASE_URL + USERS_BASE + userId + PLAYLISTS_ENDPOINT + "?offset=" + playlistsParsed + "&limit=50";
                response = get(url, record.access_token);
                jsonResponse = new JSONObject(response);
            }
        }
        return playlists;
    }

    private static Playlist parsePlaylist(JSONObject json, Profile userProfile) throws JSONException {

        String name = json.getString("name");
        String spotifyId = json.getString("id");
        String spotifyUri = json.getString("uri");

        int tracksCount = 0;
        if (json.has("tracks")) {
            tracksCount = json.getJSONObject("tracks").getInt("total");
        }


        JSONArray imagesArray = json.getJSONArray("images");
        byte[] img = null;
        if (imagesArray.length() > 0) {
            img = downloadImage(imagesArray.getJSONObject(0).getString("url"));
        }

        Playlist playlist = Select.from(Playlist.class).where(Condition.prop("SPOTIFY_ID").eq(spotifyId)).first();
        if (playlist == null) {
            playlist = new Playlist();
        }

        playlist.profile = userProfile;
        playlist.name = name;
        playlist.spotifyId = spotifyId;
        playlist.spotifyUri = spotifyUri;
        playlist.tracksCount = tracksCount;
        playlist.setImage(img);

        playlist.save();

        return playlist;
    }

    private static byte[] getImageFromArray(JSONArray images) throws JSONException {
        if (images.length() > 0) {
            // We expect that the array is sorted.
            String imageUrl = "";
            for (int i = 0 ; i < images.length() ; i++) {
                if (images.getJSONObject(i).getInt("width") > MINIMUM_WIDTH) {
                    imageUrl = images.getJSONObject(i).getString("url");
                }
            }
            if (imageUrl != null && !imageUrl.isEmpty()) {
                return downloadImage(imageUrl);
            }
        }
        return null;
    }

    public static byte[] downloadImage(String imageUrl) {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            URL url = new URL(imageUrl);
            in = new BufferedInputStream(url.openStream());
            out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n = 0;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            Log.e("SpotifyWebAPI", "Failed to download image: " + imageUrl);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) { } // Nothing to do here...
        }

        if (out != null) {
            return out.toByteArray();
        } else {
            return null;
        }
    }

    private static String get(String urlStr, String auth) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

        if (auth != null && !auth.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + auth);
        }

        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }

        // Buffer the result into a string
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();

        conn.disconnect();
        return sb.toString();
    }

    private static String post(String urlStr, String auth, String encodedParams) throws IOException {
        String type = "application/x-www-form-urlencoded";
        URL u = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", type);
        conn.setRequestProperty("Content-Length", String.valueOf(encodedParams.length()));
        conn.setRequestProperty("Authorization", auth);
        OutputStream os = conn.getOutputStream();
        os.write(encodedParams.getBytes());

        if (conn.getResponseCode() != 200) {
            throw new IOException(conn.getResponseMessage());
        }

        // Buffer the result into a string
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();

        conn.disconnect();
        return sb.toString();
    }
}
