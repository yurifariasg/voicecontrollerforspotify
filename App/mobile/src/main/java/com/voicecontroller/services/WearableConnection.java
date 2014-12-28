package com.voicecontroller.services;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;
import com.voicecontroller.models.Track;
import com.voicecontroller.models.WearableMessage;
import com.voicecontroller.settings.Settings;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

public class WearableConnection implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private String mNode;
    private Queue<WearableMessage> messages;
    private static GoogleApiClient mGoogleApiClient;

    public WearableConnection(String nodeId, Context context) {
        this.mNode = nodeId;
        this.messages = new LinkedList<>();

        //Connect the GoogleApiClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    public void requestConfirmation(Track track) throws UnsupportedEncodingException {
        WearableMessage message = new WearableMessage();
        message.path = "confirmation";

        DataMap trackMap = track.toDataMap();
        trackMap.putInt("confirmation_time", Settings.getConfirmationTime());

        message.data = trackMap.toByteArray();
        messages.add(message);

        flushMessages();
    }

    public void flushMessages() {
        if (mNode != null && mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
            while (!messages.isEmpty()) {
                WearableMessage message = messages.poll();
                Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, mNode, message.path, message.data).setResultCallback(

                        new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    Log.w("WearableConnection", "Failed to send message with status code: "
                                            + sendMessageResult.getStatus().getStatusCode());
                                }
                            }
                        }

                );
            }
        }
    }

    public void errorOccurred() {

    }

    @Override
    public void onConnected(Bundle bundle) {
        flushMessages();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mGoogleApiClient = null;
        Log.w("WearableConnection", "onConnectionFailed: " + connectionResult.getErrorCode());
    }
}
