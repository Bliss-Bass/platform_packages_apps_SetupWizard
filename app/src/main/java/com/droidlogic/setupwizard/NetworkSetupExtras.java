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

    /**
     * SetupWizard markers read by {@code WizardManagerHelper.isAnySetupWizard(Intent)} in
     * setupcompat. Settings {@code NetworkProviderSettings} selects its Glif/SUW layout from the
     * activity theme but decides {@code mIsInSetupWizard} from these intent extras. If they are
     * absent the layout is Glif while {@code mIsInSetupWizard} is false, and the mismatch throws an
     * NPE on the (null) footer button and progress views. Passing these keeps the two in sync.
     */
    public static final String EXTRA_IS_FIRST_RUN = "firstRun";
    public static final String EXTRA_IS_SETUP_FLOW = "isSetupFlow";

    private NetworkSetupExtras() {
    }
}
