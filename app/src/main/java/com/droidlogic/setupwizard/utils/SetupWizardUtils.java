package com.droidlogic.setupwizard.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

public final class SetupWizardUtils {

    private static final String TAG = "SetupWizardUtils";

    /** When true, the network step is skipped (opt-in via product config or adb setprop). */
    public static final String PROP_SKIP_WIFI = "ro.ax86.setupwizard.skip_wifi";
    public static final String PROP_SETUPWIZARD_MODE = "ro.setupwizard.mode";

    private SetupWizardUtils() {
    }

    public static boolean isSetupWizardDisabled() {
        return "DISABLED".equalsIgnoreCase(getProperty(PROP_SETUPWIZARD_MODE, ""));
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

    public static boolean isEthernetConnected(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (cm == null) return false;
        for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldAutoSkipNetworkStep(Context context) {
        return isSkipWifiSetup()
                || ((!hasWifi(context) && !hasTelephony(context))
                || isNetworkConnectedToInternetViaEthernet(context));
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, defaultValue ? "1" : "0");
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public static String getProperty(String key, String defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            return (String) systemProperties
                    .getMethod("get", String.class, String.class)
                    .invoke(null, key, defaultValue);
        } catch (Exception e) {
            Log.w(TAG, "getProperty failed for " + key, e);
            return defaultValue;
        }
    }
}
