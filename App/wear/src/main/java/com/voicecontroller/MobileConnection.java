package com.voicecontroller;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

public class MobileConnection implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Node mNode; // the connected device to send the message to
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError=false;
    private Queue<WearableMessage> messages;

    private static MobileConnection instance = null;

    public static MobileConnection getInstance() {
        return instance;
    }

    public static void createInstance(Context context) {
        instance = new MobileConnection(context);
    }

    public static void destroyInstance() {
        instance = null;
    }

    private MobileConnection(Context context) {
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        Log.e("MobileConnection", "Creating new google api client...");

        messages = new LinkedList<>();

        if (!mResolvingError) {
            Log.e("MobileConnection", "Connecting....");
            mGoogleApiClient.connect();
        }
    }

    private void createMessage(String path, String data, MessageCallback callback) {
        try {
            WearableMessage message = new WearableMessage();
            message.path = path;
            message.callback = callback;
            message.data = data.getBytes("UTF-8");
            messages.add(message);
            flushMessages();
        } catch (UnsupportedEncodingException e) {
            Log.e("MobileConnection", "Could not send query: " + e.getLocalizedMessage());
        }
    }

    public void sendQuery(String query, MessageCallback callback) {
        createMessage("query", query, callback);
    }

    public void confirmTrack(String uri) {
        createMessage("confirm_track", uri, null);
    }

    private void flushMessages() {
        Log.i("MobileConnection", "Flushing Messages...");
        if (mNode != null && mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
            while (!messages.isEmpty()) {
                final WearableMessage message = messages.poll();
                Log.i("MobileConnection", "Sending message with topic " + message.path);
                Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, mNode.getId(), message.path, message.data).setResultCallback(

                        new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {

                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    Log.e("TAG", "Failed to send message with status code: "
                                            + sendMessageResult.getStatus().getStatusCode());
                                    if (message.callback != null) {
                                        message.callback.onMessageFailed();
                                    }
                                } else {
                                    Log.i("sendMessage", "Callback Success!!");
                                    if (message.callback != null) {
                                        message.callback.onMessageSent();
                                    }
                                }
                            }
                        }

                );
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("MobileConnection", "Connected!");
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                for (Node node : nodes.getNodes()) {
                    mNode = node;
                }

                // Send queue contents
                flushMessages();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("MobileConnection", "Conneciton Suspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("MobileConnection", "Connection Failed: " + connectionResult.getErrorCode());
    }
}
