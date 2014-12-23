package com.voicecontroller.utils;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.voicecontroller.settings.Settings;
import com.voicecontroller.callbacks.OnProfileAcquired;
import com.voicecontroller.models.Profile;
import com.voicecontroller.models.Track;
import com.voicecontroller.oauth.OAuthRecord;
import com.voicecontroller.oauth.OAuthService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SpotifyWebAPI {


    public static final String CLIENT_ID = "a4a0cd5140774f89afd25d2e4dfd4e19";
    private static final String REDIRECT_URI = "voice-controller-spotify://callback";
    private static final String BASE_URL = "https://api.spotify.com/v1/";
    private static final String SEARCH_API = "search";
    private static final String USER_PROFILE = "me";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final int MINIMUM_WIDTH = 200;

    private static OAuthRecord oauthRecord = null;

    public static boolean checkOAuth() {
        Log.i("SpotifyWebAPI", "Checking OAuth");
        if (oauthRecord == null || oauthRecord.expiration < System.currentTimeMillis() / 1000) {
            oauthRecord = OAuthService.getOAuthToken();
            if (oauthRecord != null) {
                try {
                    Profile userProfile = getUserProfile(oauthRecord);
                } catch (Exception e) {
                    Log.w("SpotifyWebAPI", "OAuth Check Failed: " + e.getLocalizedMessage());
                    oauthRecord = null;
                }
            }
        }

        return oauthRecord != null;
    }

    public static void callOAuthWindow(Activity activity) {
        Log.i("SpotifyWebAPI", "Calling OAuth Window");
        // Initiate new OAuth Request...
        SpotifyAuthentication.openAuthWindow(CLIENT_ID, "token", REDIRECT_URI,
                new String[]{"user-read-private", "streaming"}, null, activity);
    }

    public static void getProfileAsync(final OAuthRecord oauth, final OnProfileAcquired callback) {
        new AsyncTask<Void, String, Profile>() {

            @Override
            protected Profile doInBackground(Void... params) {
                return getUserProfile(oauth);
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

    public static Profile getUserProfile(OAuthRecord oauth) {

        if (oauth == null) {
            oauth = OAuthService.getOAuthToken();
        }

        if (oauth != null) {

            try {
                String response = get(BASE_URL + USER_PROFILE, oauth.token);
                JSONObject json = new JSONObject(response);

                String name = json.getString("display_name");
                String imgUrl = null;
                JSONArray imgs = json.getJSONArray("images");
                if (imgs.length() > 0) {
                    imgUrl = imgs.getJSONObject(0).getString("url");
                }

                Profile p = new Profile();
                p.name = name;
                if (imgUrl != null) {
                    p.setImage(downloadImage(imgUrl));
                }
                return p;

            } catch (Exception e) {
                Log.w("SpotifyWebAPI", "Could not get user profile", e);
            }
        }
        return null;
    }

    public static Track searchTrack(String trackName, Context context) throws JSONException, IOException {
        String url = BASE_URL + SEARCH_API + "?q=" + URLEncoder.encode(trackName, DEFAULT_ENCODING) +
                "&type=track";
        String result = get(url, null);

        JSONObject json = new JSONObject(result);

        JSONArray items = json.getJSONObject("tracks").getJSONArray("items");

        if (items.length() > 0) {

            json = items.getJSONObject(0);
            String uri = json.getString("uri");
            String id = json.getString("id");
            String name = json.getString("name");
            JSONArray artists = json.getJSONArray("artists");
            String artist = "";
            if (artists.length() > 0) {
                artist = artists.getJSONObject(0).getString("name");
            }

            byte[] img = null;
            if (json.has("album") && json.getJSONObject("album").has("images")) {
                JSONArray images = json.getJSONObject("album").getJSONArray("images");
                if (images.length() > 0) {

                    // We expect that the array is sorted.
                    String imageUrl = "";
                    for (int i = 0 ; i < images.length() ; i++) {
                        if (images.getJSONObject(i).getInt("width") > MINIMUM_WIDTH) {
                            imageUrl = images.getJSONObject(i).getString("url");
                        }
                    }
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        img = downloadImage(imageUrl);
                    }
                }
            }

            if (img != null) {
                // Blur image
                Bitmap b = BitmapFactory.decodeByteArray(img, 0, img.length);
                if (b.getWidth() != 300 || b.getHeight() != 300) {
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, 300, 300, true);
                    b.recycle();
                    b = scaledBitmap;
                }

                try {
                    float blur = Settings.getBlur();
                    if (blur > 0) {
                        RenderScript rs = RenderScript.create(context);
                        final Allocation input = Allocation.createFromBitmap(rs, b);
                        final Allocation output = Allocation.createTyped(rs, input.getType());
                        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                        script.setRadius(Settings.getBlur());
                        script.setInput(input);
                        script.forEach(output);
                        output.copyTo(b);
                    }
                } catch (Exception e) {
                    Log.e("SpotifyWebAPI", "Exception while processing image: " + e.getLocalizedMessage(), e);
                }

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                b.compress(Bitmap.CompressFormat.PNG, 100, stream);
                img = stream.toByteArray();
            }

            return new Track(id, name, artist, uri, img);
        } else {
            return null;
        }
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

        Log.i("SpotifyWebAPI", "Request to: " + urlStr + " - OAuth: " + (auth == null ? "None" : auth));

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
