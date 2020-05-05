package com.logicalsapien.movem.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    Context context;
    SharedPreferences preference;
    public static final String PREFS_NAME = "movem_shared";

    public SharedPrefManager(Context c) {
        context = c;
        preference = c.getSharedPreferences(PREFS_NAME, c.MODE_PRIVATE);
    }

    public SharedPreferences.Editor getEditor() {
        return preference.edit();
    }

    public SharedPreferences getPref() {
        return preference;
    }

    public boolean apply() {
        return preference.edit().commit();
    }

    public void putLong(String key, long value) {
        SharedPreferences sharedPrefs = getPref();
        SharedPreferences.Editor ed = sharedPrefs.edit();
        ed.putLong(key, value);
        ed.commit();
    }

    public void putFloat(String key, float value) {
        SharedPreferences sharedPrefs = getPref();
        SharedPreferences.Editor ed = sharedPrefs.edit();
        ed.putFloat(key, value);
        ed.commit();
    }

    public void putString(String key, String value) {
        SharedPreferences sharedPrefs = getPref();
        SharedPreferences.Editor ed = sharedPrefs.edit();
        ed.putString(key, value);
        ed.commit();
    }

    public long getLong(String key) {
        SharedPreferences sharedPrefs = getPref();
        return sharedPrefs.getLong(key,0);
    }

    public float getFloat(String key) {
        SharedPreferences sharedPrefs = getPref();
        return sharedPrefs.getFloat(key,0);
    }

    public String getString(String key) {
        SharedPreferences sharedPrefs = getPref();
        return sharedPrefs.getString(key,"");
    }

    public boolean checkIfPresent(String key) {
        SharedPreferences sharedPrefs = getPref();
        return sharedPrefs.contains(key);
    }
}
