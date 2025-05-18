package com.droidlogic.setupwizard.fragment;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.droidlogic.setupwizard.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NavigationFragment extends BaseGuideStepFragment {

    private static final int NAVIGATION = 10;

    // Navigation bar interaction modes
    private static final int NAV_BAR_MODE_3BUTTON = 0;
    private static final int NAV_BAR_MODE_2BUTTON = 1;
    private static final int NAV_BAR_MODE_GESTURAL = 2;

    /**
     * Navigation bar mode.
     *  0 = 3 button
     *  1 = 2 button
     *  2 = fully gestural
     */
    public static final String NAVIGATION_MODE =
            "navigation_mode";

    private GuidedAction currentGuidedAction;

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
        String title = getString(R.string.navigation_setup);
        String breadcrumb = "";
        String description = getString(R.string.navigation_setup_description);
        Drawable icon = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_revert);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        if (getContext() == null) return;

        List<GuidedAction> subActions = new ArrayList<>();
        Context context = getContext();

        addCheckedAction(getActivity(),
                subActions,
                NAV_BAR_MODE_GESTURAL,
                context.getString(R.string.edge_to_edge_navigation_title),
                context.getString(R.string.edge_to_edge_navigation_summary),
                Settings.Secure.getInt(context.getContentResolver(),
                        NAVIGATION_MODE, -1)
                        == NAV_BAR_MODE_GESTURAL);
        addCheckedAction(getActivity(),
                subActions,
                NAV_BAR_MODE_2BUTTON,
                context.getString(R.string.swipe_up_to_switch_apps_title),
                context.getString(R.string.swipe_up_to_switch_apps_summary),
                Settings.Secure.getInt(context.getContentResolver(),
                        NAVIGATION_MODE, -1)
                        == NAV_BAR_MODE_2BUTTON);
        addCheckedAction(getActivity(),
                subActions,
                NAV_BAR_MODE_3BUTTON,
                context.getString(R.string.legacy_navigation_title),
                context.getString(R.string.legacy_navigation_summary),
                Settings.Secure.getInt(context.getContentResolver(),
                        NAVIGATION_MODE, -1)
                        == NAV_BAR_MODE_3BUTTON);

        for (GuidedAction subAction : subActions) {
            if (subAction.isChecked())
                currentGuidedAction = subAction;
        }

        actions.add(new GuidedAction.Builder(getActivity())
                .id(NAVIGATION)
                .title("Choose navigation mode")
                .description("Select your preferred system navigation mode")
                .subActions(subActions)
                .build());
    }

    @Override
    String getNextActionLabel() {
        return getString(R.string.action_next);
    }

    @Override
    public void onNextAction() {
        GuidedStepSupportFragment.add(getParentFragmentManager(), new NetworkFragment());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            onNextAction();
        } else if (action.getId() == NAVIGATION) {
            actionNextInvisible();
        }
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {

        try {
            switch ((int) action.getId()) {
                case NAV_BAR_MODE_3BUTTON:
                    setThreeButtonNavigation();
                    break;
                case NAV_BAR_MODE_2BUTTON:
                    setTwoButtonNavigation();
                    break;
                case NAV_BAR_MODE_GESTURAL:
                    setGestureNavigation();
                    break;
            }
            if (currentGuidedAction != null) {
                currentGuidedAction.setChecked(false);
            }
            action.setChecked(true);
            currentGuidedAction = action;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return super.onSubGuidedActionClicked(action);
    }

    public static void setGestureNavigation() throws IOException {
        Runtime.getRuntime().exec("cmd overlay enable com.android.internal.systemui.navbar.gestural");
    }

    public static void setTwoButtonNavigation() throws IOException {
        Runtime.getRuntime().exec("cmd overlay enable com.android.internal.systemui.navbar.twobutton");
    }

    public static void setThreeButtonNavigation() throws IOException {
        Runtime.getRuntime().exec("cmd overlay enable com.android.internal.systemui.navbar.threebutton");
    }

}