package android.net.wifi;

public class WifiInfo {
    public String getSSID() {
        return null;
    }

    public static String sanitizeSsid(String ssid) {
        if (ssid == null) {
            return null;
        }
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }
}
