package com.droidlogic.setupwizard.fragment;

import android.app.ActivityManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.android.internal.app.LocalePicker;
import com.droidlogic.setupwizard.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalFragment extends BaseGuideStepFragment {

    private static final int LANGUAGE = 10;
    private static final int LANGUAGE_ITEM = 11;

    private final List<GuidedAction> languageGuideActionList = new ArrayList<>();
    private List<LocalePicker.LocaleInfo> localeInfoList;
    private GuidedAction currentGuidedAction;

    private Locale getCurrentLocale() {
        Locale currentLocal = null;
        try {
            currentLocal = ActivityManager.getService().getConfiguration()
                    .getLocales().get(0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return currentLocal;
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
        localeInfoList = LocalePicker.getAllAssetLocales(getContext(), isInDeveloperMode);
        for (LocalePicker.LocaleInfo localeInfo : localeInfoList) {
            boolean checked = false;
            if (currentLocal != null && currentLocal.equals(localeInfo.getLocale())) {
                checked = true;
            }
            GuidedAction guidedAction = addCheckedAction(getActivity(), languageGuideActionList, LANGUAGE_ITEM, localeInfo.getLabel(), null, checked);
            if (checked) {
                currentGuidedAction = guidedAction;
            }
        }

        actions.add(new GuidedAction.Builder(getActivity())
                .id(LANGUAGE)
                .title(currentLocal != null ? currentLocal.getDisplayName() : "")
                .subActions(languageGuideActionList)
                .build());

        //addAction(getContext(), actions, CONTINUE, getString(R.string.action_next), null);
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
            for (LocalePicker.LocaleInfo localeInfo : localeInfoList) {
                if (TextUtils.equals(localeInfo.getLabel(), action.getTitle())) {
                    action.setChecked(true);
                    if (currentGuidedAction != null) {
                        currentGuidedAction.setChecked(false);
                    }
                    currentGuidedAction = action;
                    LocalePicker.updateLocale(localeInfo.getLocale());

                    onNextAction();

                    break;
                }
            }
        }
        return super.onSubGuidedActionClicked(action);
    }

}