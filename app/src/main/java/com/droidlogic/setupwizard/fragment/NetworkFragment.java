package com.droidlogic.setupwizard.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.GuidedActionAdapter;
import androidx.leanback.widget.GuidedActionEditText;
import androidx.leanback.widget.VerticalGridView;
import androidx.lifecycle.Lifecycle;

import com.android.settingslib.wifi.AccessPoint;
import com.droidlogic.setupwizard.ConnectivityListener;
import com.droidlogic.setupwizard.MainActivity;
import com.droidlogic.setupwizard.R;
import com.droidlogic.setupwizard.leanback.timepicker.GuidedWifiSignalAction;
import com.droidlogic.setupwizard.utils.WifiConfigHelper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static android.net.NetworkInfo.DetailedState.CONNECTING;

public class NetworkFragment extends BaseGuideStepFragment {

    private static final String TAG = "NetworkFragment";

    private final int ID_WIFI = 20;
    private final int ID_ETHERNET = 21;
    private final int EDITABLE_LABEL = 2000;

    private static final int MSG_WHAT_START = 1;
    private static final int MSG_WHAT_STOP = 2;

    private WifiManager wifiManager;
    private ConnectivityListener connectivityListener;
    private final List<GuidedAction> wifiGuideActionList = new ArrayList<>();
    private final ArrayList<AccessPoint> wifiList = new ArrayList<>();
    private WifiConfiguration currentConfiguration;

    private GuidedAction wifiGuidedAction;
    private GuidedAction ethernetGuidedAction;

    private TaskHandler taskHandler;
    private HandlerThread handlerThread;

    // Local constants for hidden values
    private static final int NETWORK_SELECTION_ENABLED = 0;
    private static final int DISABLED_AUTHENTICATION_FAILURE = 1;
    private static final int DISABLED_BY_WRONG_PASSWORD = 2;

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info != null && info.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    updateConnectState(true);
                } else if (info != null) {
                    NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == CONNECTING ||
                            state == NetworkInfo.DetailedState.AUTHENTICATING ||
                            state == NetworkInfo.DetailedState.OBTAINING_IPADDR ||
                            state == NetworkInfo.DetailedState.DISCONNECTED ||
                            state == NetworkInfo.DetailedState.FAILED) {
                        updateWifiState(state);
                    }
                }
            }
        }
    };

    private NetworkInfo getNetworkInfo(int networkType) {
        if (getContext() == null || wifiManager == null) return null;
        ConnectivityManager connManager = (ConnectivityManager) getContext().getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getNetworkInfo(networkType);
    }

    private void updateWifiList() {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            runOnUiThread(() -> {
                if (connectivityListener != null) {
                    final List<AccessPoint> accessPoints = connectivityListener.getAvailableNetworks();
                    updateWifiList(accessPoints);
                    updateConnectState(false);
                }
            });
        }
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.network_setup);
        String breadcrumb = "";
        String description = getString(R.string.network_description);
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.network);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onFocusChange(View oldFocus, View newFocus) {
        super.onFocusChange(oldFocus, newFocus);
        MainActivity mainActivity = getMainActivity();
        if (mainActivity == null) return;
        if (newFocus instanceof GuidedActionEditText) {
            mainActivity.showWifiView(newFocus);
        } else {
            mainActivity.hideWifiView();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        if (context == null) return;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private void setWifiListener() {
        if (connectivityListener != null) {
            connectivityListener.setWifiListener(() -> {
                List<AccessPoint> accessPointList = connectivityListener.getAvailableNetworks();
                if (accessPointList != null) {
                    for (AccessPoint accessPoint : accessPointList) {
                        try {
                            WifiConfiguration configuration = accessPoint.getConfig();
                            if (currentConfiguration != null && configuration != null && TextUtils.equals(configuration.SSID, currentConfiguration.SSID)) {
                                int state = getNetworkSelectionStatus(configuration);
                                if (state == DISABLED_AUTHENTICATION_FAILURE || state == DISABLED_BY_WRONG_PASSWORD) {
                                    if (!TextUtils.isEmpty(currentConfiguration.preSharedKey)) {
                                        toast(currentConfiguration.SSID + getString(R.string.password_error));
                                    }
                                    currentConfiguration = null;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                updateWifiList();
            });
        }
    }

    private int getNetworkSelectionStatus(WifiConfiguration config) {
        try {
            Method getStatusMethod = WifiConfiguration.class.getMethod("getNetworkSelectionStatus");
            Object status = getStatusMethod.invoke(config);
            if (status != null) {
                Method getNetworkSelectionStatusMethod = status.getClass().getMethod("getNetworkSelectionStatus");
                return (int) getNetworkSelectionStatusMethod.invoke(status);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get network selection status", e);
        }
        return NETWORK_SELECTION_ENABLED;
    }

    private void initTask() {
        if (taskHandler == null) {
            if (handlerThread != null) {
                handlerThread.quitSafely();
            }
            handlerThread = new HandlerThread(getClass().getName());
            handlerThread.start();
            taskHandler = new TaskHandler(handlerThread.getLooper(), this);
        }
    }

    private static class TaskHandler extends Handler {

        private final WeakReference<NetworkFragment> weakReference;

        public TaskHandler(Looper looper, NetworkFragment networkFragment) {
            super(looper);
            weakReference = new WeakReference<>(networkFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                NetworkFragment networkFragment = weakReference.get();
                if (networkFragment == null) return;
                switch (msg.what) {
                    case MSG_WHAT_START:
                        if (networkFragment.connectivityListener != null) {
                            networkFragment.connectivityListener.start();
                            networkFragment.setWifiListener();
                            networkFragment.updateWifiList();
                        }
                        break;
                    case MSG_WHAT_STOP:
                        if (networkFragment.connectivityListener != null) {
                            networkFragment.connectivityListener.stop();
                            networkFragment.connectivityListener.destroy();
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Context context = getContext();
        if (context != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            context.registerReceiver(wifiScanReceiver, intentFilter);
        }
        initTask();
        if (taskHandler != null) {
            taskHandler.sendEmptyMessage(MSG_WHAT_START);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(wifiScanReceiver);
        }
        if (taskHandler != null) {
            taskHandler.sendEmptyMessage(MSG_WHAT_STOP);
            taskHandler = null;
        }
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }
    }

    @Override
    String getNextActionLabel() {
        return getString(R.string.action_next);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        connectivityListener = new ConnectivityListener(getContext(), this::updateWifiList);
    }

    private static void addWifiAction(
            Context context,
            List<GuidedAction> actions,
            long id,
            String title,
            String desc,
            int signalLevel) {
        actions.add(new GuidedWifiSignalAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .signalLevel(signalLevel)
                .build());
    }

    private static void addEditablePasswordAction(
            Context context,
            List<GuidedAction> actions,
            long id,
            String title,
            String desc,
            int signalLevel) {
        actions.add(new GuidedWifiSignalAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .editable(true)
                .editTitle("")
                .signalLevel(signalLevel)
                .descriptionInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .descriptionEditInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .build());
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        wifiGuidedAction = new GuidedAction.Builder(getActivity())
                .id(ID_WIFI)
                .title(getString(R.string.network_type_wifi))
                .description(getString(R.string.not_connected))
                .build();
        actions.add(wifiGuidedAction);

        ethernetGuidedAction = new GuidedAction.Builder(getActivity())
                .id(ID_ETHERNET)
                .title(getString(R.string.network_type_ethernet))
                .description(getString(R.string.not_connected))
                .build();
        actions.add(ethernetGuidedAction);
    }

    @SuppressLint("RestrictedApi")
    private void updateWifiList(List<AccessPoint> accessPoints) {
        if (accessPoints == null || wifiManager == null) return;
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            wifiGuideActionList.clear();
            wifiList.clear();
            wifiList.addAll(accessPoints);
            int index = 0;
            for (final AccessPoint accessPoint : accessPoints) {
                WifiConfiguration config = accessPoint.getConfig();
                if (config != null) {
                    if (getNetworkSelectionStatus(config) != NETWORK_SELECTION_ENABLED) {
                        addEditablePasswordAction(getActivity(), wifiGuideActionList, EDITABLE_LABEL + index, accessPoint.getTitle(), accessPoint.getSummary(), accessPoint.getLevel());
                    } else {
                        addWifiAction(getActivity(), wifiGuideActionList, index, accessPoint.getTitle(), accessPoint.getSummary(), accessPoint.getLevel());
                    }
                } else {
                    if (accessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
                        addWifiAction(getActivity(), wifiGuideActionList, index, accessPoint.getTitle(), accessPoint.getSummary(), accessPoint.getLevel());
                    } else {
                        addEditablePasswordAction(getActivity(), wifiGuideActionList, EDITABLE_LABEL + index, accessPoint.getTitle(), accessPoint.getSummary(), accessPoint.getLevel());
                    }
                }
                index++;
            }
            VerticalGridView verticalGridView = getGuidedActionsStylist().getSubActionsGridView();
            if (verticalGridView != null) {
                GuidedActionAdapter guidedActionAdapter = (GuidedActionAdapter) verticalGridView.getAdapter();
                if (guidedActionAdapter != null) {
                    guidedActionAdapter.setActions(wifiGuideActionList);
                }
            }
        }
    }

    private void updateConnectState(boolean toastMsg) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            //wifi state
            if (wifiManager != null) {
                NetworkInfo networkInfo = getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnected()) {
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo != null) {
                        String ssid = wifiInfo.getSSID();
                        if (!TextUtils.isEmpty(ssid) && ssid.length() > 2) {
                            ssid = WifiConfigHelper.sanitizeSsid(ssid);
                        }
                        if (wifiGuidedAction != null) {
                            wifiGuidedAction.setDescription(ssid);
                            notifyActionChanged(0);
                        }
                        if (toastMsg) {
                            toast(ssid + getString(R.string.connected));
                        }
                    }
                }
            }
            //ethernet
            if (connectivityListener != null && connectivityListener.isEthernetConnected()) {
                if (ethernetGuidedAction != null) {
                    ethernetGuidedAction.setDescription(getString(R.string.connected));
                    notifyActionChanged(1);
                }
            }
        }
    }

    private void updateWifiState(NetworkInfo.DetailedState state) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED) && state != null && wifiGuidedAction != null) {
            int strId = -1;
            switch (state) {
                case CONNECTING:
                    strId = R.string.wifi_state_connecting;
                    break;
                case AUTHENTICATING:
                    strId = R.string.wifi_state_authenticating;
                    break;
                case OBTAINING_IPADDR:
                    strId = R.string.wifi_state_obtaining_ipaddr;
                    break;
                case DISCONNECTED:
                    strId = R.string.wifi_state_disconnected;
                    break;
                case FAILED:
                    strId = R.string.wifi_state_failed;
                    break;
            }
            if (strId != -1) {
                wifiGuidedAction.setDescription(getString(strId));
                notifyActionChanged(0);
            }
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        super.onGuidedActionClicked(action);
        if (action.getId() == ID_WIFI) {
            setActions(wifiGuideActionList);
        } else if (action.getId() == ID_ETHERNET) {
            // do nothing
        } else if (action.getId() < EDITABLE_LABEL) {
            AccessPoint accessPoint = wifiList.get((int) action.getId());
            if (accessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
                WifiConfiguration configuration = WifiConfigHelper.getConfiguration(getContext(), accessPoint.getSsidStr(), accessPoint.getSecurity(), "");
                connectToNetwork(configuration);
                currentConfiguration = configuration;
            }
        }
    }

    private void connectToNetwork(WifiConfiguration config) {
        try {
            Method connectMethod = WifiManager.class.getMethod("connect", WifiConfiguration.class, Class.forName("android.net.wifi.WifiManager$ActionListener"));
            connectMethod.invoke(wifiManager, config, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to network", e);
            // Fallback to standard enableNetwork
            int networkId = wifiManager.addNetwork(config);
            if (networkId != -1) {
                wifiManager.enableNetwork(networkId, true);
            }
        }
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        if (action.getId() >= EDITABLE_LABEL) {
            AccessPoint accessPoint = wifiList.get((int) (action.getId() - EDITABLE_LABEL));
            String password = action.getEditTitle().toString();
            if (!TextUtils.isEmpty(password)) {
                WifiConfiguration configuration = WifiConfigHelper.getConfiguration(getContext(), accessPoint.getSsidStr(), accessPoint.getSecurity(), password);
                connectToNetwork(configuration);
                currentConfiguration = configuration;
            }
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

    @Override
    public void onNextAction() {
        // Correct the flow loop: Network -> DateTime
        GuidedStepSupportFragment.add(getParentFragmentManager(), new DateTimeFragment());
    }
}
