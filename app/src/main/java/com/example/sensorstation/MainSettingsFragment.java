package com.example.sensorstation;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.material.snackbar.Snackbar;

public class MainSettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener{

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_main, rootKey);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();
        int count = preferenceScreen.getPreferenceCount();
        for (int i =0; i < count; i++){
            //Loop through all of the prefs
            Preference p = preferenceScreen.getPreference(i);
            if (p instanceof PreferenceCategory){
                //if its a Category, go through all the subprefs
                PreferenceCategory category = (PreferenceCategory) p;
                int subCount = category.getPreferenceCount();
                for (int j = 0; j < subCount; j++){
                    Preference p2 = category.getPreference(j);
                    if (p2 instanceof EditTextPreference){
                        String value = sharedPreferences.getString(p2.getKey(), "error");
                        p2.setSummary(value);
                    }
                }
            }
        }
        setOnChangeListeners();
    }

    void setOnChangeListeners(){
        Preference preferenceNotificationEnabled = findPreference("PREF_KEY_MAIN_NOTIFICATIONS_ENABLED");
        Preference preferenceNotificationsStop = findPreference("PREF_KEY_MAIN_STOP_NOTIFICATIONS");
        Preference preferenceNotificationsStart = findPreference("PREF_KEY_MAIN_START_NOTIFICATIONS");

        preferenceNotificationEnabled.setOnPreferenceChangeListener(this);
        preferenceNotificationsStop.setOnPreferenceChangeListener(this);
        preferenceNotificationsStart.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference != null){
            if (preference instanceof EditTextPreference){
                String value = sharedPreferences.getString(preference.getKey(), "");
                preference.setSummary(value);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String prefKey = preference.getKey();
        String settingValue = (String) newValue;

        View view = getActivity().findViewById(R.id.settings_main);
        Snackbar errorSnackbar = Snackbar.make(view, "onPreferenceChange" + settingValue, Snackbar.LENGTH_SHORT);

        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
