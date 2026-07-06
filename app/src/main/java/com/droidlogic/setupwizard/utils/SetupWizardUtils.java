package com.droidlogic.setupwizard.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

public final class SetupWizardUtils {

    private static final String TAG = "SetupWizardUtils";

    /** When true, the network step is skipped (opt-in via product config or adb setprop). */
    public static final String PROP_SKIP_WIFI = "ro.ax86.setupwizard.skip_wifi";

    private SetupWizardUtils() {
    }

    public static boolean isSkipWifiSetup() {
        return getBooleanProperty(PROP_SKIP_WIFI, false);
    }

    public static boolean hasWifi(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    public static boolean hasTelephony(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    public static boolean isNetworkConnectedToInternetViaEthernet(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (cm == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public static boolean shouldAutoSkipNetworkStep(Context context) {
        return isSkipWifiSetup()
                || ((!hasWifi(context) && !hasTelephony(context))
                || isNetworkConnectedToInternetViaEthernet(context));
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            String value = (String) systemProperties
                    .getMethod("get", String.class, String.class)
                    .invoke(null, key, defaultValue ? "1" : "0");
            return "1".equals(value) || "true".equalsIgnoreCase(value);
        } catch (Exception e) {
            Log.w(TAG, "getBooleanProperty failed for " + key, e);
            return defaultValue;
        }
    }
}
