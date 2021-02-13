package com.example.sensorstation;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.google.android.material.snackbar.Snackbar;

public class GraphSettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener
{

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.prefs_graph);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();
        int count = preferenceScreen.getPreferenceCount();
        for (int i =0; i < count; i++){
            Preference p = preferenceScreen.getPreference(i);
            String value = sharedPreferences.getString(p.getKey(), "0");
            p.setSummary(value);
        }
        setOnChangeListeners();
    }

    //Set change listeners so the values can be tested to be numbers
    void setOnChangeListeners(){
        Preference preferenceCO2min = findPreference("PREF_KEY_GR_CO2_Y_MIN");
        Preference preferenceCO2max = findPreference("PREF_KEY_GR_CO2_Y_MAX");
        Preference preferenceCO2labels = findPreference("PREF_KEY_GR_CO2_VERT_LABELS");

        Preference preferenceT1min = findPreference("PREF_KEY_GR_T1_Y_MIN");
        Preference preferenceT1max = findPreference("PREF_KEY_GR_T1_Y_MAX");
        Preference preferenceT1labels = findPreference("PREF_KEY_GR_T1_VERT_LABELS");

        Preference preferenceT2min = findPreference("PREF_KEY_GR_T2_Y_MIN");
        Preference preferenceT2max = findPreference("PREF_KEY_GR_T2_Y_MAX");
        Preference preferenceT2labels = findPreference("PREF_KEY_GR_T2_VERT_LABELS");

        Preference preferenceRHmin = findPreference("PREF_KEY_GR_RH_Y_MIN");
        Preference preferenceRHmax = findPreference("PREF_KEY_GR_RH_Y_MAX");
        Preference preferenceRHlabels = findPreference("PREF_KEY_GR_RH_VERT_LABELS");

        Preference preferenceTVOCmin = findPreference("PREF_KEY_GR_TVOC_Y_MIN");
        Preference preferenceTVOCmax = findPreference("PREF_KEY_GR_TVOC_Y_MAX");
        Preference preferenceTVOClabels = findPreference("PREF_KEY_GR_TVOC_VERT_LABELS");

        Preference preferencePRmin = findPreference("PREF_KEY_GR_PR_Y_MIN");
        Preference preferencePRmax = findPreference("PREF_KEY_GR_PR_Y_MAX");
        Preference preferencePRlabels = findPreference("PREF_KEY_GR_PR_VERT_LABELS");

        preferenceCO2min.setOnPreferenceChangeListener(this);
        preferenceCO2max.setOnPreferenceChangeListener(this);
        preferenceCO2labels.setOnPreferenceChangeListener(this);

        preferenceT1min.setOnPreferenceChangeListener(this);
        preferenceT1max.setOnPreferenceChangeListener(this);
        preferenceT1labels.setOnPreferenceChangeListener(this);

        preferenceT2min.setOnPreferenceChangeListener(this);
        preferenceT2max.setOnPreferenceChangeListener(this);
        preferenceT2labels.setOnPreferenceChangeListener(this);

        preferenceRHmin.setOnPreferenceChangeListener(this);
        preferenceRHmax.setOnPreferenceChangeListener(this);
        preferenceRHlabels.setOnPreferenceChangeListener(this);

        preferenceTVOCmin.setOnPreferenceChangeListener(this);
        preferenceTVOCmax.setOnPreferenceChangeListener(this);
        preferenceTVOClabels.setOnPreferenceChangeListener(this);

        preferencePRmin.setOnPreferenceChangeListener(this);
        preferencePRmax.setOnPreferenceChangeListener(this);
        preferencePRlabels.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference != null){
            String value = sharedPreferences.getString(preference.getKey(), "");
            preference.setSummary(value);
        }
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

    //This is called before the preference is saved in SharedPreferences
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        View view = getActivity().findViewById(R.id.settings_activity);
        Snackbar errorSnackbar = Snackbar.make(view, "Invalid value", Snackbar.LENGTH_SHORT);

        String prefKey = preference.getKey();
        String settingValue = (String) newValue;
        int settingIntValue = -9999;
        //Check that the numeric values can be cast to ints
        if (prefKey.equals("PREF_KEY_GR_CO2_Y_MIN") ||
                prefKey.equals("PREF_KEY_GR_CO2_Y_MAX")||
                prefKey.equals("PREF_KEY_GR_CO2_VERT_LABELS")||
                prefKey.equals("PREF_KEY_GR_T1_Y_MIN")||
                prefKey.equals("PREF_KEY_GR_T1_Y_MAX")||
                prefKey.equals("PREF_KEY_GR_T1_VERT_LABELS")||
                prefKey.equals("PREF_KEY_GR_T2_Y_MIN")||
                prefKey.equals("PREF_KEY_GR_T2_Y_MAX")||
                prefKey.equals("PREF_KEY_GR_T2_VERT_LABELS")||
                prefKey.equals("PREF_KEY_GR_RH_Y_MIN")||
                prefKey.equals("PREF_KEY_GR_RH_Y_MAX")||
                prefKey.equals("PREF_KEY_GR_RH_VERT_LABELS")||
                prefKey.equals("PREF_KEY_GR_TVOC_Y_MIN")||
                prefKey.equals("PREF_KEY_GR_TVOC_Y_MAX")||
                prefKey.equals("PREF_KEY_GR_TVOC_VERT_LABELS")||
                prefKey.equals("PREF_KEY_GR_PR_Y_MIN")||
                prefKey.equals("PREF_KEY_GR_PR_Y_MAX")||
                prefKey.equals("PREF_KEY_GR_PR_VERT_LABELS")){
            try {
                settingIntValue = Integer.parseInt(settingValue);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                errorSnackbar.show();
                //Do not ssave the settings
                return false;
            }
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //Check that the MIN value is not set higher than the max value
        if (prefKey.equals("PREF_KEY_GR_CO2_Y_MIN")){
            if (isSettingIncorrect(settingIntValue,
                    Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_CO2_Y_MAX", "0")))){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_T1_Y_MIN")){
            if (isSettingIncorrect(settingIntValue,
                    Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T1_Y_MAX", "0")))){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_T2_Y_MIN")){
            if (isSettingIncorrect(settingIntValue,
                    Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T2_Y_MAX", "0")))){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_RH_Y_MIN")){
            if (isSettingIncorrect(settingIntValue,
                    Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_RH_Y_MAX", "0")))){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_TVOC_Y_MIN")){
            if (isSettingIncorrect(settingIntValue,
                    Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_TVOC_Y_MAX", "0")))){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_PR_Y_MIN")){
            if (isSettingIncorrect(settingIntValue,
                    Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_PR_Y_MAX", "0")))){
                errorSnackbar.show();
                return false;
            }
        }



        if (prefKey.equals("PREF_KEY_GR_CO2_Y_MAX")){
            if (isSettingIncorrect(Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_CO2_Y_MIN",  "0")),
                    settingIntValue)){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_T1_Y_MAX")){
            if (isSettingIncorrect(Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T1_Y_MIN", "0")),
                    settingIntValue)){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_T2_Y_MAX")){
            if (isSettingIncorrect(Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T2_Y_MIN", "0")),
                    settingIntValue)){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_RH_Y_MAX")){
            if (isSettingIncorrect(Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_RH_Y_MIN", "0")),
                    settingIntValue)){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_TVOC_Y_MAX")){
            if (isSettingIncorrect(Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_TVOC_Y_MIN", "0")),
                    settingIntValue)){
                errorSnackbar.show();
                return false;
            }
        }
        if (prefKey.equals("PREF_KEY_GR_PR_Y_MAX")){
            if (isSettingIncorrect(Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_PR_Y_MIN", "0")),
                    settingIntValue)){
                errorSnackbar.show();
                return false;
            }
        }
        //True - the value will be saved in shared preferences
        return true;
    }

    private boolean isSettingIncorrect (int min, int max){
        if (min >= max){
            return true;
        }
        return false;
    }
}
