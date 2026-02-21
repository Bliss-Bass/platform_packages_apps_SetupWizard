package com.droidlogic.setupwizard.fragment;

import android.app.AlarmManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.GuidedActionsStylist;
import androidx.leanback.widget.GuidedDatePickerAction;

import com.android.settingslib.datetime.ZoneGetter;
import com.droidlogic.setupwizard.R;
import com.droidlogic.setupwizard.leanback.timepicker.GuidedActionsStylistExtended;
import com.droidlogic.setupwizard.leanback.timepicker.GuidedTimePickerAction;
import com.droidlogic.setupwizard.utils.I18NUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class DateTimeFragment extends BaseGuideStepFragment {

    private static final int TIME_ZONE = 30;
    private static final int DATE_PICKER = 31;
    private static final int TIME_PICKER = 32;

    private static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_GMT = "gmt";
    private static final String KEY_OFFSET = "offset";

    @Override
    public GuidedActionsStylist onCreateActionsStylist() {
        return new GuidedActionsStylistExtended();
    }

    public static DateTimeFragment newInstance(final int option) {
        final DateTimeFragment f = new DateTimeFragment();
        final Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }

    @Override
    String getNextActionLabel() {
        return getString(R.string.action_next);
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.data_time_setup);
        String breadcrumb = "";
        String description = getString(R.string.data_time_description);
        Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.calendar);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    GuidedAction currentGuidedAction;
    List<Map<String, Object>> zonesList;
    GuidedAction timeZoneGuidedAction;
    GuidedDatePickerAction datePickerGuidedAction;
    GuidedTimePickerAction timePickerGuidedAction;

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        if (getContext() == null) return;

        currentGuidedAction = null;
        currentTimeZoneId = TimeZone.getDefault().getID();

        List<GuidedAction> timeZone = new ArrayList<>();
        zonesList = ZoneGetter.getZonesList(getContext());
        final TimeZoneComparator comparator = new TimeZoneComparator(KEY_OFFSET);
        zonesList.sort(comparator);

        for (int i = 0; i < zonesList.size(); i++) {
            Map<String, Object> zoneMap = zonesList.get(i);
            boolean check = String.valueOf(zoneMap.get(KEY_ID)).equals(currentTimeZoneId);
            GuidedAction guidedAction = addCheckedAction(getActivity(), timeZone, i, String.valueOf(zoneMap.get(KEY_NAME)), String.valueOf(zoneMap.get(KEY_GMT)), check);
            if (check) {
                currentGuidedAction = guidedAction;
            }
        }

        timeZoneGuidedAction = new GuidedAction.Builder(getActivity())
                .id(TIME_ZONE)
                .title(I18NUtils.getTimeZoneName())
                .description(I18NUtils.getTimeZoneOffset())
                .subActions(timeZone)
                .build();

        actions.add(timeZoneGuidedAction);

        datePickerGuidedAction = new GuidedDatePickerAction.Builder(getActivity())
                .id(DATE_PICKER)
                .title(getString(R.string.current_date))
                .build();
        actions.add(datePickerGuidedAction);

        timePickerGuidedAction = new GuidedTimePickerAction.Builder(getActivity())
                .id(TIME_PICKER)
                .title(getString(R.string.current_time))
                .build();
        actions.add(timePickerGuidedAction);
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        if (action.getId() == DATE_PICKER || action.getId() == TIME_PICKER) {
            updateDateTime();
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

    private void updateDateTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(datePickerGuidedAction.getDate()));
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        Log.i("updateDateTime", "year:" + year + "\tmonth:" + month + "\tday:" + day);

        calendar.setTime(new Date(timePickerGuidedAction.getTime()));
        calendar.set(year, month, day);

        long when = calendar.getTimeInMillis();
        if (getContext() == null) return;
        if (when / 1000 < Integer.MAX_VALUE) {
            if (datePickerGuidedAction != null) {
                datePickerGuidedAction.setDate(when);
            }
            ((AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    @Override
    public void onNextAction() {
        GuidedStepSupportFragment.add(getParentFragmentManager(), new DisplaySettingsFragment());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            onNextAction();
        } else if (action.getId() == BACK) {
            getParentFragmentManager().popBackStack();
        } else if (action.getId() == TIME_ZONE || action.getId() == DATE_PICKER || action.getId() == TIME_PICKER) {
            actionNextInvisible();
        }
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        try {
            Map<String, Object> zoneMap = zonesList.get((int) action.getId());
            final String tzId = (String) zoneMap.get(KEY_ID);
            if (setTimeZone(tzId)) {
                postDelayed(() -> {
                    String title = I18NUtils.getTimeZoneName();
                    String desc = I18NUtils.getTimeZoneOffset();
                    timeZoneGuidedAction.setTitle(title);
                    timeZoneGuidedAction.setDescription(desc);
                    updateDateTime();
                    notifyActionChanged(0);
                    notifyActionChanged(1);
                    notifyActionChanged(2);
                }, 350);
            }
            if (currentGuidedAction != null) {
                currentGuidedAction.setChecked(false);
            }
            action.setChecked(true);
            currentGuidedAction = action;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onSubGuidedActionClicked(action);
    }

    private String currentTimeZoneId;

    private boolean setTimeZone(String tzId) {
        boolean update = false;
        if (getContext() != null && currentTimeZoneId != null && !currentTimeZoneId.equals(tzId)) {
            AlarmManager alarm = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            alarm.setTimeZone(tzId);
            currentTimeZoneId = tzId;
            update = true;
        }
        return update;
    }

    private static class TimeZoneComparator implements Comparator<Map<String, ?>> {

        private final String sortKey;

        public TimeZoneComparator(String sortKey) {
            this.sortKey = sortKey;
        }

        public int compare(Map<String, ?> map1, Map<String, ?> map2) {
            Object value1 = map1.get(sortKey);
            Object value2 = map2.get(sortKey);
            if (!isComparable(value1)) {
                return isComparable(value2) ? 1 : 0;
            } else if (!isComparable(value2)) {
                return -1;
            }
            return ((Comparable) value1).compareTo(value2);
        }

        private boolean isComparable(Object value) {
            return (value instanceof Comparable);
        }

    }

}
