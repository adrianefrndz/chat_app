package com.example.chatapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;

    // Constructor to initialize SharedPreferences
    public PreferenceManager(Context context) {
        preferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    // Save a boolean value
    public void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value).apply();
    }

    // Retrieve a boolean value, default is false
    public boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }

    // Save a string value
    public void putString(String key, String value) {
        editor.putString(key, value).apply();
    }

    // Retrieve a string value, default is null
    public String getString(String key) {
        return preferences.getString(key, null);
    }

    // Remove a specific key-value pair
    public void remove(String key) {
        editor.remove(key).apply();
    }

    // Clear all preferences
    public void clear() {
        editor.clear().apply();
    }
}
