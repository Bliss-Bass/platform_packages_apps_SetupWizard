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

import java.lang.reflect.Method;
import java.util.List;

public abstract class BaseGuideStepFragment extends GuidedStepSupportFragment {

    protected static final int CONTINUE = 1;
    protected static final int BACK = 2;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            VerticalGridView verticalGridView = getGuidedActionsStylist().getActionsGridView();
            if (verticalGridView != null) {
                // Add bottom padding to lift the actions list above the navbar
                int bottomPadding = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 86, getResources().getDisplayMetrics());
                verticalGridView.setPadding(
                        verticalGridView.getPaddingLeft(),
                        verticalGridView.getPaddingTop(),
                        verticalGridView.getPaddingRight(),
                        bottomPadding
                );
                verticalGridView.setClipToPadding(false);

                // Existing reflection code
                RecyclerView.LayoutManager layoutManager = verticalGridView.getLayoutManager();
                Class cls = Class.forName("androidx.leanback.widget.GridLayoutManager");
                Method method;
                try {
                    method = cls.getMethod("setFocusOutAllowed", boolean.class, boolean.class);
                } catch (NoSuchMethodException e) {
                    method = cls.getDeclaredMethod("setFocusOutAllowed", boolean.class, boolean.class);
                    method.setAccessible(true);
                }
                method.invoke(layoutManager, true, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            mainActivity.nextActionBringToFront();
        }

        setNextActionText(getNextActionLabel());
    }

    protected void actionNextInvisible() {
        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            mainActivity.actionNextInvisible();
        }
    }

    protected void actionNextVisible() {
        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            mainActivity.actionNextVisible();
        }
    }

    protected void setNextActionText(String text) {
        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null) {
            mainActivity.setNextActionText(text);
        }
    }

    @Override
    public void collapseAction(boolean withTransition) {
        super.collapseAction(withTransition);
        actionNextVisible();
    }

    public abstract void onNextAction();

    public void onFocusChange(View oldFocus, View newFocus) {

    }

}