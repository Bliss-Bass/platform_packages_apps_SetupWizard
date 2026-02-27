package com.droidlogic.setupwizard.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.GuidedActionAdapter;
import androidx.leanback.widget.GuidedActionEditText;
import androidx.leanback.widget.GuidedActionsStylist;
import androidx.leanback.widget.VerticalGridView;
import androidx.lifecycle.Lifecycle;

import com.android.settingslib.wifi.AccessPoint;
import com.droidlogic.setupwizard.ConnectivityListener;
import com.droidlogic.setupwizard.MainActivity;
import com.droidlogic.setupwizard.R;
import com.droidlogic.setupwizard.leanback.timepicker.GuidedActionsStylistExtended;
import com.droidlogic.setupwizard.leanback.timepicker.GuidedWifiSignalAction;
import com.droidlogic.setupwizard.utils.WifiConfigHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.net.NetworkInfo.DetailedState.CONNECTING;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.DISABLED_AUTHENTICATION_FAILURE;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.DISABLED_BY_WRONG_PASSWORD;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.NETWORK_SELECTION_ENABLED;

public class NetworkFragment extends BaseGuideStepFragment {

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

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    updateConnectState(true);
                } else {
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
                final List<AccessPoint> accessPoints = connectivityListener.getAvailableNetworks();
                updateWifiList(accessPoints);
                updateConnectState(false);
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
        if (context instanceof MainActivity) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
    }

    private void setWifiListener() {
        connectivityListener.setWifiListener(() -> {
            List<AccessPoint> accessPointList = connectivityListener.getAvailableNetworks();
            if (accessPointList != null) {
                for (AccessPoint accessPoint : accessPointList) {
                    try {
                        WifiConfiguration configuration = accessPoint.getConfig();
                        if (currentConfiguration != null && configuration != null && TextUtils.equals(configuration.SSID, currentConfiguration.SSID)) {
                            int state = (configuration.getNetworkSelectionStatus().getNetworkSelectionStatus());
                            if (state == DISABLED_AUTHENTICATION_FAILURE || state == DISABLED_BY_WRONG_PASSWORD) {
                                if (currentConfiguration != null && !TextUtils.isEmpty(currentConfiguration.preSharedKey)) {
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
                        networkFragment.connectivityListener.start();
                        networkFragment.setWifiListener();
                        networkFragment.updateWifiList();
                        break;
                    case MSG_WHAT_STOP:
                        networkFragment.connectivityListener.stop();
                        networkFragment.connectivityListener.destroy();
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

    @SuppressLint("RestrictedApi")
    private void updateWifiList(List<AccessPoint> accessPoints) {
        if (accessPoints == null || wifiManager == null) return;
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED) && isHideShowSoftKeyboard()) {
            wifiGuideActionList.clear();
            wifiList.clear();
            wifiList.addAll(accessPoints);
            int index = 0;
            for (final AccessPoint accessPoint : accessPoints) {
                WifiConfiguration config = accessPoint.getConfig();
                if (config != null) {
                    if (config.getNetworkSelectionStatus().getNetworkSelectionStatus() != NETWORK_SELECTION_ENABLED) {
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
        if (wifiGuidedAction != null && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            //wifi state
            if (wifiManager != null) {
                NetworkInfo networkInfo = getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnected()) {
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo == null) {
                        wifiGuidedAction.setDescription(getString(R.string.not_connected));
                    } else {
                        String ssid = wifiInfo.getSSID();
                        if (!TextUtils.isEmpty(ssid) && ssid.length() > 2) {
                            if (ssid.startsWith("\"")) {
                                ssid = ssid.substring(1);
                            }
                            if (ssid.endsWith("\"")) {
                                ssid = ssid.substring(0, ssid.length() - 1);
                            }
                        }
                        wifiGuidedAction.setDescription(ssid);
                        notifyActionChanged(0);
                        if (toastMsg) {
                            toast(ssid + getString(R.string.connected));
                        }
                    }
                }
            }
            //ethernet
            if (connectivityListener.isEthernetConnected()) {
                ethernetGuidedAction.setDescription(getString(R.string.connected));
            } else {
                ethernetGuidedAction.setDescription(getString(R.string.not_connected));
            }
            notifyActionChanged(1);
        }
    }

    private void updateWifiState(NetworkInfo.DetailedState state) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED) && state != null) {
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
                    strId = R.string.not_connected;
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

    private int getWifiIcon(int level) {
        if (level <= 0) {
            return R.drawable.wifi_strength_0;
        } else if (level == 1) {
            return R.drawable.wifi_strength_1;
        } else if (level == 2) {
            return R.drawable.wifi_strength_2;
        } else if (level == 3) {
            return R.drawable.wifi_strength_3;
        }
        return R.drawable.wifi_strength_4;
    }

    @Override
    public GuidedActionsStylist onCreateActionsStylist() {
        GuidedActionsStylistExtended guidedActionsStylistExtended = new GuidedActionsStylistExtended() {
            @Override
            public void onBindViewHolder(ViewHolder vh, GuidedAction action) {
                super.onBindViewHolder(vh, action);
                if (action instanceof GuidedWifiSignalAction) {
                    try {
                        GuidedWifiSignalAction guidedWifiSignalAction = (GuidedWifiSignalAction) action;
                        Drawable drawable = AppCompatResources.getDrawable(vh.itemView.getContext(), getWifiIcon(guidedWifiSignalAction.getSignalLevel()));
                        if (drawable != null) {
                            drawable.setBounds(0, 0, 48, 48);
                            TextView titleView = vh.getTitleView();
                            titleView.setCompoundDrawables(null, null, drawable, null);
                        }
                        vh.itemView.setOnLongClickListener(view -> {
                            try {
                                if (!action.isEditable()) {
                                    AccessPoint accessPoint = wifiList.get((int) action.getId());
                                    WifiConfiguration config = accessPoint.getConfig();
                                    if (config != null) {
                                        showForgetDialog(accessPoint);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return true;
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        guidedActionsStylistExtended.setEditingModeChangeListener(new GuidedActionsStylistExtended.EditingModeChangeListener() {
            @Override
            public void onEditingModeChange(GuidedActionsStylist.ViewHolder vh, boolean editing, boolean withTransition) {
                if (editing) {
                    GuidedAction action = vh.getAction();
                    CharSequence editTitle = action.getTitle();
                    MainActivity mainActivity = getMainActivity();
                    if (mainActivity == null) return;
                    mainActivity.setWifiName(String.valueOf(editTitle));
                }
            }
        });
        return guidedActionsStylistExtended;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        wifiGuidedAction = new GuidedAction.Builder(getActivity())
                .title(getString(R.string.network_type_wifi))
                .subActions(wifiGuideActionList)
                .id(ID_WIFI)
                .description(getString(R.string.not_connected))
                .build();

        ethernetGuidedAction = new GuidedAction.Builder(getActivity())
                .title(getString(R.string.network_type_ethernet))
                .id(ID_ETHERNET)
                .description(getString(R.string.not_connected))
                .build();

        actions.add(wifiGuidedAction);
        actions.add(ethernetGuidedAction);

        //addAction(getContext(), actions, CONTINUE, getString(R.string.action_next), null);
    }

    @Override
    public void onNextAction() {
        FragmentManager fm = getParentFragmentManager();
        DateTimeFragment next = DateTimeFragment.newInstance(getSelectedActionPosition() - 1);
        GuidedStepSupportFragment.add(fm, next);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            onNextAction();
        } else if (action.getId() == ID_WIFI) {
            actionNextInvisible();
        } else if (action.getId() == ID_ETHERNET) {
            if (ethernetGuidedAction != null) {
                toast(getString(R.string.network_type_ethernet) + ":" + ethernetGuidedAction.getDescription());
            }
        }
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        if (action.getId() >= EDITABLE_LABEL) return super.onSubGuidedActionClicked(action);
        AccessPoint accessPoint = wifiList.get((int) action.getId());
        WifiConfiguration config = accessPoint.getConfig();
        if (config != null) {
            if (!accessPoint.isActive()) {
                connect(config);
            }
        } else if (accessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
            WifiConfiguration wifiConfiguration = WifiConfigHelper.getConfiguration(getContext(), accessPoint.getSsidStr(), accessPoint.getSecurity(), null);
            connect(wifiConfiguration);
        }
        return super.onSubGuidedActionClicked(action);
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        int position = (int) (action.getId() - EDITABLE_LABEL);
        CharSequence wifiPassword = action.getEditTitle();
        AccessPoint accessPoint = wifiList.get(position);
        WifiConfiguration wifiConfiguration = WifiConfigHelper.getConfiguration(getContext(), accessPoint.getSsidStr(), accessPoint.getSecurity(), String.valueOf(wifiPassword));
        connect(wifiConfiguration);
        return super.onGuidedActionEditedAndProceed(action);
    }

    private void connect(WifiConfiguration configuration) {
        if (wifiManager == null || configuration == null) return;
        currentConfiguration = configuration;
        wifiManager.disconnect();
        wifiManager.connect(configuration, null);
    }

    private void forget(WifiConfiguration configuration) {
        if (wifiManager == null || configuration == null) return;
        wifiManager.forget(configuration.networkId, null);
        wifiManager.reconnect();
    }

    private void showForgetDialog(AccessPoint accessPoint) {
        Context context = getContext();
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle(R.string.forget_network_title)
                .setMessage(R.string.forget_network_message)
                .setNegativeButton(R.string.dialog_negative, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.dialog_positive, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    if (accessPoint.isActive()) {
                        currentConfiguration = null;
                        if (wifiManager != null) {
                            wifiManager.disconnect();
                        }
                    }
                    forget(accessPoint.getConfig());
                }).create().show();
    }

    private boolean isHideShowSoftKeyboard() {
        if (getContext() == null) return true;
        InputMethodManager im = getContext().getSystemService(InputMethodManager.class);
        return im.getInputMethodWindowVisibleHeight() == 0;
    }

}