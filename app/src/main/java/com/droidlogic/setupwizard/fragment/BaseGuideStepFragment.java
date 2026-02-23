package com.droidlogic.setupwizard.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.droidlogic.setupwizard.MainActivity;
import com.droidlogic.setupwizard.R;

import java.lang.reflect.Method;
import java.util.List;

public abstract class BaseGuideStepFragment extends GuidedStepSupportFragment {

    protected static final int CONTINUE = 1;
    protected static final int BACK = 2;
    protected static final int ACTION_NEXT = 1000;

    private static final int OPTION_CHECK_SET_ID = 10;

    public static GuidedAction addCheckedAction(
            Context context,
            List<GuidedAction> actions,
            int id,
            String title,
            String desc,
            boolean checked) {
        GuidedAction guidedAction = new GuidedAction.Builder(context)
                .id(id)
                .title(title)
                .description(desc)
                .checkSetId(OPTION_CHECK_SET_ID)
                .build();
        guidedAction.setChecked(checked);
        actions.add(guidedAction);
        return guidedAction;
    }

    protected void toast(int textResId) {
        toast(getString(textResId));
    }

    protected void toast(String text) {
        if (getContext() == null) return;
        runOnUiThread(() -> Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show());
    }

    protected void runOnUiThread(Runnable runnable) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(runnable);
        }
    }

    protected MainActivity getMainActivity() {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            return (MainActivity) activity;
        }
        return null;
    }

    abstract String getNextActionLabel();

    @Override
    public void onCreateButtonActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        actions.add(new GuidedAction.Builder(getContext())
                .id(ACTION_NEXT)
                .title(getNextActionLabel())
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == ACTION_NEXT) {
            onNextAction();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Ensure buttons are visible within the panel
        try {
            View actionsContainer = getGuidedActionsStylist().getActionsGridView().getParent() instanceof View 
                ? (View) getGuidedActionsStylist().getActionsGridView().getParent() : null;
            if (actionsContainer != null) {
                actionsContainer.setElevation(0);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public abstract void onNextAction();

    public void onFocusChange(View oldFocus, View newFocus) {
    }

    protected void actionNextInvisible() {
        // No-op, managed by Button Actions
    }

    protected void actionNextVisible() {
        // No-op, managed by Button Actions
    }

    protected void setNextActionText(String text) {
        // No-op, managed by Button Actions
    }
}
