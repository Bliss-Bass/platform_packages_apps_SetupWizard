package android.net.wifi;

public class WifiConfiguration {
    public String SSID;
    public String preSharedKey;
    public int networkId;
    public int macRandomizationSetting;

    public static class NetworkSelectionStatus {
        public static final int NETWORK_SELECTION_ENABLED = 0;
        public static final int DISABLED_AUTHENTICATION_FAILURE = 1;
        public static final int DISABLED_BY_WRONG_PASSWORD = 2;

        public int getNetworkSelectionStatus() {
            return 0;
        }
    }

    public NetworkSelectionStatus getNetworkSelectionStatus() {
        return null;
    }
}
