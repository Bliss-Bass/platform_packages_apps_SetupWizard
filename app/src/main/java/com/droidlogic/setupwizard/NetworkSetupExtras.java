package com.droidlogic.setupwizard;

/**
 * Intent extras for {@link android.provider.Settings#ACTION_NETWORK_PROVIDER_SETUP}
 * (handled by Settings {@code NetworkSetupActivity}). Matches Lineage SetupWizard.
 */
public final class NetworkSetupExtras {

    public static final String ACTION_SETUP_NETWORK = "android.settings.NETWORK_PROVIDER_SETUP";

    public static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    public static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";
    public static final String EXTRA_PREFS_SHOW_SKIP_TV = "extra_show_skip_network";
    public static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";
    public static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";

    private NetworkSetupExtras() {
    }
}
