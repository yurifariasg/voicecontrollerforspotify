package com.voicecontroller;

import android.app.Application;
import android.content.Intent;


public class WearableApp extends Application {

    // Android Wear's default UncaughtExceptionHandler
    private Thread.UncaughtExceptionHandler mDefaultUEH;

    private Thread.UncaughtExceptionHandler mWearUEH = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(final Thread thread, final Throwable ex) {

            // Pass the exception to a Service which will send the data upstream to your Smartphone/Tablet
            Intent errorIntent = new Intent(WearableApp.this, ErrorService.class);
            errorIntent.putExtra("exception", ex);
            startService(errorIntent);

            // Let the default UncaughtExceptionHandler take it from here
            mDefaultUEH.uncaughtException(thread, ex);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mWearUEH);
    }
}
