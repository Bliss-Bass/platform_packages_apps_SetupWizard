package com.droidlogic.setupwizard.services;

import static com.droidlogic.setupwizard.fragment.NavigationFragment.setGestureNavigation;
import static com.droidlogic.setupwizard.fragment.NavigationFragment.setThreeButtonNavigation;
import static com.droidlogic.setupwizard.fragment.NavigationFragment.setTwoButtonNavigation;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.app.LocalePicker;
import com.droidlogic.setupwizard.MainActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MessengerService extends Service {
    // Navigation mode constants
    public static final int NAVIGATION_MODE_GESTURE = 0;
    public static final int NAVIGATION_MODE_TWO_BUTTON = 1;
    public static final int NAVIGATION_MODE_THREE_BUTTON = 2;

    private static final String TAG = "MessengerService";

    /**
     * Commands to the service.
     */
    static final int MSG_CHANGE_NAVIGATION_MODE = 1;
    static final int MSG_CHANGE_DATE_AND_TIME = 2;
    static final int MSG_CHANGE_LANGUAGE = 3;
    static final int MSG_CHANGE_TIME_ZONE = 4;
    static final int MSG_OVERRIDE_SETUP_COMPLETE = 5;


    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
        private final WeakReference<Context> context;


        IncomingHandler(Context context) {
            super(Looper.getMainLooper());
            this.context = new WeakReference<>(context);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHANGE_NAVIGATION_MODE:
                    int navigationMode = msg.arg1;
                    setNavigationMode(navigationMode);
                    break;

                case MSG_CHANGE_DATE_AND_TIME:
                    if (msg.obj instanceof Long) {
                        long dateInMillis = (Long) msg.obj;

                        if (dateInMillis / 1000 < Integer.MAX_VALUE && context.get() != null)
                            ((AlarmManager) context.get().getSystemService(Context.ALARM_SERVICE)).setTime(dateInMillis);
                    }
                    break;

                case MSG_CHANGE_TIME_ZONE:
                    if (msg.obj instanceof String && context.get() != null) {
                        AlarmManager alarm = (AlarmManager) context.get().getSystemService(Context.ALARM_SERVICE);
                        alarm.setTimeZone((String) msg.obj);
                    }
                    break;

                case MSG_CHANGE_LANGUAGE:
                    if (msg.obj instanceof LocaleList) {
                        LocalePicker.updateLocales((LocaleList) msg.obj);
                    }
                    break;

                case MSG_OVERRIDE_SETUP_COMPLETE:
                    if (context.get() != null)
                        if (msg.arg1 == 1) MainActivity.enableComponent(context.get());
                        else if (msg.arg1 == 0) MainActivity.disableComponent(context.get());
                    break;

                default:
                    super.handleMessage(msg);
                    Log.w(TAG, "Unknown message received: " + msg.what);
            }
        }


    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    Messenger mMessenger;
    private static void setNavigationMode(int mode) {
        try {
            switch (mode) {
                case NAVIGATION_MODE_GESTURE:
                    setGestureNavigation();
                    break;

                case NAVIGATION_MODE_TWO_BUTTON:
                    setTwoButtonNavigation();
                    break;

                case NAVIGATION_MODE_THREE_BUTTON:
                    setThreeButtonNavigation();
                    break;

                default:
                    Log.e(TAG, "Invalid navigation mode");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        mMessenger = new Messenger(new IncomingHandler(this));

        return mMessenger.getBinder();
    }
}
