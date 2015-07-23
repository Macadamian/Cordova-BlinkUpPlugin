package com.macadamian.blinkup.util;

import android.app.Activity;
import android.content.SharedPreferences;

public class PreferencesHelper {
    private static final String PREFERENCES_NAME = "BlinkUpPlugin";
    private static final String PLAN_ID_KEY = "PlanId";

    static private String getStringPreference(Activity activity, String key, String defVal) {
        SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
        return preferences.getString(key, defVal);
    }

    static private void setStringPreference(Activity activity, String key, String val) {
        SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, val);
        editor.apply();
    }

    static public String getPlanId(Activity activity) {
        return getStringPreference(activity, PLAN_ID_KEY, null);
    }

    static public void setPlanId(Activity activity, String planIdKey) {
        setStringPreference(activity, PLAN_ID_KEY, planIdKey);
    }
}
