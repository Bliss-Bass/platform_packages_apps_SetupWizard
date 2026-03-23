package com.droidlogic.setupwizard.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.droidlogic.setupwizard.R;

import java.lang.reflect.Method;
import java.util.List;

public class AgeVerificationFragment extends BaseGuideStepFragment {

    private static final String TAG = "AgeVerificationFragment";
    private static final int ACTION_ADULT = 1;
    private static final int ACTION_MINOR = 2;

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
        String title = getString(R.string.age_verification_title);
        String description = getString(R.string.age_verification_description);
        Drawable icon = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_info_details);
        return new GuidanceStylist.Guidance(title, description, "", icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_ADULT)
                .title(getString(R.string.age_verification_adult))
                .build());
        actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_MINOR)
                .title(getString(R.string.age_verification_minor))
                .build());
    }

    @Override
    String getNextActionLabel() {
        return null; // Actions lead directly to next page
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == ACTION_ADULT) {
            setAgeProperty(false);
            onNextAction();
        } else if (action.getId() == ACTION_MINOR) {
            setAgeProperty(true);
            onNextAction();
        }
    }

    @Override
    public void onNextAction() {
        GuidedStepSupportFragment.add(getParentFragmentManager(), new DisplaySettingsFragment());
    }

    private void setAgeProperty(boolean isMinor) {
        String value = isMinor ? "true" : "false";
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method setMethod = systemProperties.getMethod("set", String.class, String.class);
            setMethod.invoke(null, "persist.os.mandate.age_verification.is_minor", value);
            Log.d(TAG, "Set persist.os.mandate.age_verification.is_minor to " + value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set age verification property", e);
            try {
                Runtime.getRuntime().exec("setprop persist.os.mandate.age_verification.is_minor " + value);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to setprop via exec", ex);
            }
        }
    }
}
