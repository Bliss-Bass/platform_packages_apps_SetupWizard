package com.droidlogic.setupwizard.fragment;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.droidlogic.setupwizard.R;

import java.util.ArrayList;
import java.util.List;

public class DisplaySettingsFragment extends BaseGuideStepFragment {

    private static final String TAG = "DisplaySettingsFragment";

    private static final int ACTION_THEME = 100;
    private static final int ACTION_FONT_SIZE = 101;

    private static final int THEME_LIGHT = 1000;
    private static final int THEME_DARK = 1001;

    private static final int FONT_SMALL = 2000;
    private static final int FONT_DEFAULT = 2001;
    private static final int FONT_LARGE = 2002;
    private static final int FONT_LARGEST = 2003;

    private GuidedAction themeAction;
    private GuidedAction fontAction;

    @Override
    String getNextActionLabel() {
        return getString(R.string.complete_setup);
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.display_setup);
        String breadcrumb = "";
        String description = getString(R.string.display_description);
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_display_theme);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        if (getContext() == null) return;

        // Theme sub-actions
        List<GuidedAction> themeSubActions = new ArrayList<>();
        int currentNightMode = getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        addCheckedAction(getActivity(), themeSubActions, THEME_LIGHT, getString(R.string.theme_light), null, currentNightMode == Configuration.UI_MODE_NIGHT_NO);
        addCheckedAction(getActivity(), themeSubActions, THEME_DARK, getString(R.string.theme_dark), null, currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        themeAction = new GuidedAction.Builder(getActivity())
                .id(ACTION_THEME)
                .title(getString(R.string.ui_theme))
                .description(currentNightMode == Configuration.UI_MODE_NIGHT_YES ? getString(R.string.theme_dark) : getString(R.string.theme_light))
                .subActions(themeSubActions)
                .build();
        actions.add(themeAction);

        // Font size sub-actions
        List<GuidedAction> fontSubActions = new ArrayList<>();
        float currentFontScale = getContext().getResources().getConfiguration().fontScale;
        addCheckedAction(getActivity(), fontSubActions, FONT_SMALL, getString(R.string.font_size_small), null, Math.abs(currentFontScale - 0.85f) < 0.01f);
        addCheckedAction(getActivity(), fontSubActions, FONT_DEFAULT, getString(R.string.font_size_default), null, Math.abs(currentFontScale - 1.0f) < 0.01f);
        addCheckedAction(getActivity(), fontSubActions, FONT_LARGE, getString(R.string.font_size_large), null, Math.abs(currentFontScale - 1.15f) < 0.01f);
        addCheckedAction(getActivity(), fontSubActions, FONT_LARGEST, getString(R.string.font_size_largest), null, Math.abs(currentFontScale - 1.30f) < 0.01f);

        fontAction = new GuidedAction.Builder(getActivity())
                .id(ACTION_FONT_SIZE)
                .title(getString(R.string.font_size))
                .description(getFontSizeLabel(currentFontScale))
                .subActions(fontSubActions)
                .build();
        actions.add(fontAction);
    }

    private String getFontSizeLabel(float scale) {
        if (Math.abs(scale - 0.85f) < 0.01f) return getString(R.string.font_size_small);
        if (Math.abs(scale - 1.15f) < 0.01f) return getString(R.string.font_size_large);
        if (Math.abs(scale - 1.30f) < 0.01f) return getString(R.string.font_size_largest);
        return getString(R.string.font_size_default);
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        if (getContext() == null) return true;

        if (action.getId() == THEME_LIGHT || action.getId() == THEME_DARK) {
            int mode = action.getId() == THEME_DARK ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO;
            updateTheme(mode);
            themeAction.setDescription(action.getTitle());
            notifyActionChanged(findActionPositionById(ACTION_THEME));
        } else if (action.getId() >= FONT_SMALL && action.getId() <= FONT_LARGEST) {
            float scale = 1.0f;
            if (action.getId() == FONT_SMALL) scale = 0.85f;
            else if (action.getId() == FONT_LARGE) scale = 1.15f;
            else if (action.getId() == FONT_LARGEST) scale = 1.30f;
            
            updateFontSize(scale);
            fontAction.setDescription(action.getTitle());
            notifyActionChanged(findActionPositionById(ACTION_FONT_SIZE));
        }
        return super.onSubGuidedActionClicked(action);
    }

    private void updateTheme(int mode) {
        UiModeManager uiModeManager = (UiModeManager) getContext().getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager != null) {
            uiModeManager.setNightMode(mode);
        }
    }

    private void updateFontSize(float scale) {
        try {
            Settings.System.putFloat(getContext().getContentResolver(), Settings.System.FONT_SCALE, scale);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update font scale", e);
        }
    }

    @Override
    public int findActionPositionById(long id) {
        List<GuidedAction> actions = getActions();
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).getId() == id) return i;
        }
        return -1;
    }

    @Override
    public void onNextAction() {
        getMainActivity().finishSetup();
    }
}
