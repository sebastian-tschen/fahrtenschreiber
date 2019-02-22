package bastel.de.fahrtenschreiber;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String DRIVER = "driver";
    public static final String SHEET_ID_KEY = "sheet_id";
    private EditTextPreference driverPreference;
    private EditTextPreference sheetIDPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);


        // Get the username Preference
        driverPreference = (EditTextPreference) getPreferenceManager()
                .findPreference(DRIVER);

        // Get the username Preference
        sheetIDPreference = (EditTextPreference) getPreferenceManager()
                .findPreference(SHEET_ID_KEY);

        findPreference("sheet_id").setOnPreferenceChangeListener((preference, newValue) -> checkSheetAvailable(newValue));

        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        driverPreference.setSummary(sharedPreferences.getString(DRIVER, ""));
        sheetIDPreference.setSummary(sharedPreferences.getString(SHEET_ID_KEY, ""));

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(DRIVER)) {
            driverPreference.setSummary(sharedPreferences.getString(DRIVER, ""));
        }

        if (key.equals(SHEET_ID_KEY)) {
            sheetIDPreference.setSummary(sharedPreferences.getString(SHEET_ID_KEY, ""));
        }


    }

    private boolean checkSheetAvailable(Object newValue) {

        return true;

    }
}