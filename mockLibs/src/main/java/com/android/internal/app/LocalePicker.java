package com.android.internal.app;

import android.content.Context;
import java.util.List;
import java.util.Locale;

public class LocalePicker {
    public static class LocaleInfo {
        public String getLabel() { return null; }
        public Locale getLocale() { return null; }
    }

    public static List<LocaleInfo> getAllAssetLocales(Context context, boolean isInDeveloperMode) {
        return null;
    }

    public static void updateLocale(Locale locale) {
    }
}
