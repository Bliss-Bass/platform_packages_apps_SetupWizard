package com.droidlogic.setupwizard.fragment;

import android.app.ActivityManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.droidlogic.setupwizard.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalFragment extends BaseGuideStepFragment {

    private static final String TAG = "LocalFragment";
    private static final int LANGUAGE = 10;
    private static final int LANGUAGE_ITEM = 11;

    private final List<GuidedAction> languageGuideActionList = new ArrayList<>();
    private List<Object> localeInfoList;
    private GuidedAction currentGuidedAction;

    private Locale getCurrentLocale() {
        return getResources().getConfiguration().getLocales().get(0);
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
        String title = getString(R.string.local_setup);
        String breadcrumb = "";
        String description = getString(R.string.local_setup_description);
        Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.language);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        if (getContext() == null) return;

        languageGuideActionList.clear();
        Locale currentLocal = getCurrentLocale();

        boolean isInDeveloperMode = Settings.Global.getInt(getContext().getContentResolver(), "development_settings_enabled", 0) != 0;
        localeInfoList = getAllAssetLocales(isInDeveloperMode);
        
        if (localeInfoList != null) {
            for (Object localeInfo : localeInfoList) {
                try {
                    Method getLocaleMethod = localeInfo.getClass().getMethod("getLocale");
                    Method getLabelMethod = localeInfo.getClass().getMethod("getLabel");
                    Locale locale = (Locale) getLocaleMethod.invoke(localeInfo);
                    String label = (String) getLabelMethod.invoke(localeInfo);
                    
                    boolean checked = false;
                    if (currentLocal != null && currentLocal.equals(locale)) {
                        checked = true;
                    }
                    GuidedAction guidedAction = addCheckedAction(getActivity(), languageGuideActionList, LANGUAGE_ITEM, label, null, checked);
                    if (checked) {
                        currentGuidedAction = guidedAction;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to process locale info", e);
                }
            }
        }

        actions.add(new GuidedAction.Builder(getActivity())
                .id(LANGUAGE)
                .title(currentLocal != null ? currentLocal.getDisplayName() : "")
                .subActions(languageGuideActionList)
                .build());
    }

    @SuppressWarnings("unchecked")
    private List<Object> getAllAssetLocales(boolean isInDeveloperMode) {
        try {
            Class<?> localePickerClass = Class.forName("com.android.internal.app.LocalePicker");
            Method method = localePickerClass.getMethod("getAllAssetLocales", android.content.Context.class, boolean.class);
            return (List<Object>) method.invoke(null, getContext(), isInDeveloperMode);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get all asset locales via reflection", e);
            return new ArrayList<>();
        }
    }

    @Override
    String getNextActionLabel() {
        return getString(R.string.action_next);
    }

    @Override
    public void onNextAction() {
        GuidedStepSupportFragment.add(getParentFragmentManager(), new NavigationFragment());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            onNextAction();
        } else if (action.getId() == LANGUAGE) {
            actionNextInvisible();
        }
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        if (action.getId() == LANGUAGE_ITEM && localeInfoList != null) {
            for (Object localeInfo : localeInfoList) {
                try {
                    Method getLabelMethod = localeInfo.getClass().getMethod("getLabel");
                    String label = (String) getLabelMethod.invoke(localeInfo);
                    if (TextUtils.equals(label, action.getTitle())) {
                        action.setChecked(true);
                        if (currentGuidedAction != null) {
                            currentGuidedAction.setChecked(false);
                        }
                        currentGuidedAction = action;
                        
                        Method getLocaleMethod = localeInfo.getClass().getMethod("getLocale");
                        Locale locale = (Locale) getLocaleMethod.invoke(localeInfo);
                        updateLocale(locale);

                        onNextAction();
                        break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to update locale via reflection", e);
                }
            }
        }
        return super.onSubGuidedActionClicked(action);
    }

    private void updateLocale(Locale locale) {
        try {
            Class<?> localePickerClass = Class.forName("com.android.internal.app.LocalePicker");
            Method method = localePickerClass.getMethod("updateLocale", java.util.Locale.class);
            method.invoke(null, locale);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update locale via reflection", e);
        }
    }

}
