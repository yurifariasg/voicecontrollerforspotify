package com.voicecontroller.services;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;
import com.voicecontroller.models.Track;
import com.voicecontroller.models.WearableMessage;

import org.json.JSONException;

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
        Log.i("WearableConnection", "Creating Confirmation....");

        WearableMessage message = new WearableMessage();
        message.path = "confirmation";
        message.data = track.toDataMap().toByteArray();
        messages.add(message);

        flushMessages();
    }

    public void flushMessages() {
        Log.i("WearableConnection", "Flushing messages...");
        if (mNode != null && mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
            while (!messages.isEmpty()) {
                WearableMessage message = messages.poll();
                Log.i("WearableConnection", "Sending message with topic " + message.path);
                Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, mNode, message.path, message.data).setResultCallback(

                        new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    Log.e("TAG", "Failed to send message with status code: "
                                            + sendMessageResult.getStatus().getStatusCode());
                                } else {
                                    Log.i("sendMessage", "Callback Success!!");
                                }
                            }
                        }

                );
            }
        }
    }

    public void trackNotFound() {

    }

    public void errorOccurred() {

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i("WearableConnection", "onConnected");
        flushMessages();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient = null;
        Log.i("WearableConnection", "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mGoogleApiClient = null;
        Log.i("WearableConnection", "onConnectionFailed: " + connectionResult.getErrorCode());
    }
}
