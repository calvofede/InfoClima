package com.seminario2.fede.calvo.infoclima;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.seminario2.fede.calvo.infoclima.sync.InfoClimaSyncUtils;

import static com.seminario2.fede.calvo.infoclima.MainActivity.CONTENT_URI;

/**
 * Se utiliza conjuntamente a la activity Settings.
 * Para mostrar una jerarquia de preferencias en forma de lista
 */
public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Chequeamos si es una listPreference o no para setear el value
    //actual que tiene esa setting
    private void buildPrefSummary(Object obj, Preference pref) {
        if (pref instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) pref;
            int prefIndex = listPreference.findIndexOfValue(obj.toString());

            if (prefIndex >= 0) {
                pref.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            pref.setSummary(obj.toString());
        }
    }

    //Recorremos las preferencias del fragment para setearles el summary value
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();
        int count = prefScreen.getPreferenceCount();

        for (int i = 0; i < count; i++) {
            Preference pref = prefScreen.getPreference(i);

            if (!(pref instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(pref.getKey(), "");
                buildPrefSummary(value, pref);
            }
        }
    }

    //Implementado para detectar cuando cambia una setting actualizar su value en
    //el summary
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);

        if (key.equals("location")){
            InfoClimaSyncUtils.startImmediateSync(getActivity());
        }

        else if (key.equals("units")) {
            getActivity().getContentResolver().notifyChange(CONTENT_URI, null);
        }

        if (null != preference) {
            if (!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(), "");
                buildPrefSummary(value, preference);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    //registro esta clase como listener de cambios de shared preferences
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    //desregistro el listener
    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
