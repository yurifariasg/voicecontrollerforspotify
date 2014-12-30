package com.voicecontroller.models;


import android.content.Context;

import com.google.android.gms.wearable.DataMap;
import com.voicecontroller.utils.GeneralUtils;

public class VoiceQueryResult {

    private String id;
    private String name;
    private String subtitle; /* Optional */
    private byte[] image;

    public VoiceQueryResult(String id, String name, byte[] image) {
        this(id, name, null, image);
    }

    public VoiceQueryResult(String id, String name, String subtitle, byte[] image) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public byte[] getImageBytes() {
        return image;
    }

    public DataMap toDataMap(Context context) {
        DataMap data = new DataMap();
        data.putString("subtitle", getSubtitle());
        data.putString("title", getName());
        data.putString("id", getId());
        byte[] blurredImg = GeneralUtils.blurImage(getImageBytes(), context);
        if (blurredImg != null) {
            data.putByteArray("image", blurredImg);
        } else {
            data.putByteArray("image", getImageBytes());
        }
        return data;
    }
}
