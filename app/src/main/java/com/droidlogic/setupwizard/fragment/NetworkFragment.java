package com.droidlogic.setupwizard.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.droidlogic.setupwizard.MainActivity;
import com.droidlogic.setupwizard.NetworkSetupExtras;
import com.droidlogic.setupwizard.R;
import com.droidlogic.setupwizard.utils.SetupWizardUtils;

import java.util.List;

/**
 * Network setup step: delegates Wi-Fi UI to Settings {@code NETWORK_PROVIDER_SETUP}
 * (same approach as LineageOS SetupWizard). Leanback Wi-Fi list/password UI is not used on A16.
 */
public class NetworkFragment extends BaseGuideStepFragment {

    private static final String TAG = "NetworkFragment";
    private static final String STATE_SETTINGS_LAUNCHED = "settings_launched";
    private static final int ID_ETHERNET = 21;

    private ActivityResultLauncher<Intent> networkSetupLauncher;
    private GuidedAction ethernetGuidedAction;
    private boolean settingsLaunched;

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            runOnUiThread(() -> updateEthernetStatus());
        }

        @Override
        public void onLost(@NonNull Network network) {
            runOnUiThread(() -> updateEthernetStatus());
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            runOnUiThread(() -> updateEthernetStatus());
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            settingsLaunched = savedInstanceState.getBoolean(STATE_SETTINGS_LAUNCHED, false);
        }
        networkSetupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onNetworkSetupResult);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SETTINGS_LAUNCHED, settingsLaunched);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!settingsLaunched) {
            settingsLaunched = true;
            startNetworkSetup();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ConnectivityManager cm = requireContext().getSystemService(ConnectivityManager.class);
        if (cm != null) {
            cm.registerNetworkCallback(
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                            .build(),
                    networkCallback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        ConnectivityManager cm = requireContext().getSystemService(ConnectivityManager.class);
        if (cm != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.network_setup);
        String description = getString(R.string.network_description_settings);
        Drawable icon = ContextCompat.getDrawable(requireActivity(), R.drawable.network);
        return new GuidanceStylist.Guidance(title, description, "", icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        ethernetGuidedAction = new GuidedAction.Builder(requireContext())
                .id(ID_ETHERNET)
                .title(getString(R.string.network_type_ethernet))
                .description(getString(R.string.not_connected))
                .enabled(false)
                .build();
        actions.add(ethernetGuidedAction);

        actions.add(new GuidedAction.Builder(requireContext())
                .id(CONTINUE)
                .title(getString(R.string.network_open_settings))
                .build());
    }

    @Override
    String getNextActionLabel() {
        return getString(R.string.network_skip_wifi);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateEthernetStatus();
        actionNextVisible();
    }

    private void updateEthernetStatus() {
        if (ethernetGuidedAction != null && isAdded()) {
            boolean connected = SetupWizardUtils.isEthernetConnected(requireContext());
            ethernetGuidedAction.setDescription(connected ? getString(R.string.connected) : getString(R.string.not_connected));
            ethernetGuidedAction.setEnabled(connected);
            notifyActionChanged(0);
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            startNetworkSetup();
        }
    }

    @Override
    public void onNextAction() {
        FragmentManager fm = getParentFragmentManager();
        GuidedStepSupportFragment.add(fm, DateTimeFragment.newInstance(0));
    }

    private void startNetworkSetup() {
        if (SetupWizardUtils.shouldAutoSkipNetworkStep(requireContext())) {
            Log.i(TAG, "auto-skipping network step");
            settingsLaunched = true;
            onNextAction();
            return;
        }

        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            mainActivity.enableWifi();
        }

        Intent intent = new Intent(NetworkSetupExtras.ACTION_SETUP_NETWORK);
        intent.putExtra(NetworkSetupExtras.EXTRA_IS_FIRST_RUN, true);
        intent.putExtra(NetworkSetupExtras.EXTRA_IS_SETUP_FLOW, true);
        intent.putExtra(NetworkSetupExtras.EXTRA_PREFS_SHOW_BUTTON_BAR, true);
        intent.putExtra(NetworkSetupExtras.EXTRA_PREFS_SHOW_SKIP, true);
        intent.putExtra(NetworkSetupExtras.EXTRA_PREFS_SHOW_SKIP_TV, true);
        intent.putExtra(NetworkSetupExtras.EXTRA_PREFS_SET_BACK_TEXT, (String) null);
        intent.putExtra(NetworkSetupExtras.EXTRA_ENABLE_NEXT_ON_CONNECT, true);

        settingsLaunched = true;
        try {
            networkSetupLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NETWORK_PROVIDER_SETUP not available", e);
            settingsLaunched = false;
            toast(R.string.network_settings_missing);
        }
    }

    private void onNetworkSetupResult(ActivityResult result) {
        int resultCode = result.getResultCode();
        Intent data = result.getData();
        Log.i(TAG, "network setup result code=" + resultCode);

        if (resultCode == Activity.RESULT_OK) {
            onNextAction();
            return;
        }

        if (data != null && data.getBooleanExtra("onBackPressed", false)) {
            startNetworkSetup();
            return;
        }

        actionNextVisible();
    }
}
