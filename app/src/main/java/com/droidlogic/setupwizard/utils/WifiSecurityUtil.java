package com.droidlogic.setupwizard.utils;

import android.net.wifi.WifiConfiguration;
import com.android.settingslib.wifi.AccessPoint;

public class WifiSecurityUtil {
    public static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return AccessPoint.SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) || config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            return AccessPoint.SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? AccessPoint.SECURITY_WEP : AccessPoint.SECURITY_NONE;
    }
}
