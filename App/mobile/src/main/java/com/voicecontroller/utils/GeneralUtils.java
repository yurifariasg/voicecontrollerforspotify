package com.voicecontroller.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.voicecontroller.settings.Settings;

import java.io.ByteArrayOutputStream;

public class GeneralUtils {

    public static String startWithAny(String name, String[] prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    public static String endWithAny(String name, String[] suffixes) {
        for (String suffix : suffixes) {
            if (name.endsWith(suffix)) {
                return suffix;
            }
        }
        return null;
    }

    public static byte[] blurImage(byte[] img, Context context) {
        // Blur image
        Bitmap b = BitmapFactory.decodeByteArray(img, 0, img.length);
        if (b.getWidth() != 300 || b.getHeight() != 300) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, 300, 300, true);
            b.recycle();
            b = scaledBitmap;
        }

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

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}
