package android.net.wifi;

import android.net.Network;
import java.util.List;

public class WifiManager {
    public static final String NETWORK_STATE_CHANGED_ACTION = "android.net.wifi.STATE_CHANGE";
    public static final String EXTRA_NETWORK_INFO = "networkInfo";

    public Network getCurrentNetwork() {
        return null;
    }

    public String[] getFactoryMacAddresses() {
        return null;
    }

    public boolean isConnectedMacRandomizationSupported() {
        return false;
    }

    public void connect(WifiConfiguration config, Object listener) {
    }

    public void forget(int networkId, Object listener) {
    }

    public WifiInfo getConnectionInfo() {
        return null;
    }

    public boolean isWifiEnabled() {
        return false;
    }

    public boolean setWifiEnabled(boolean enabled) {
        return false;
    }

    public List<WifiConfiguration> getConfiguredNetworks() {
        return null;
    }

    public int addNetwork(WifiConfiguration config) {
        return -1;
    }

    public boolean enableNetwork(int netId, boolean disableOthers) {
        return false;
    }

    public boolean saveConfiguration() {
        return false;
    }
}
