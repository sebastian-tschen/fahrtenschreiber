package bastel.de.fahrtenschreiber;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import static bastel.de.fahrtenschreiber.FahrtenschreiberActivity.REQUEST_AUTHORIZATION;
import static bastel.de.fahrtenschreiber.FahrtenschreiberActivity.REQUEST_GOOGLE_PLAY_SERVICES;

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
    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                getActivity(),
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }
    private boolean checkSheetAvailable(Object newValue) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.show();

        SheetsHelper.getInstance().checkSheetsId((valid, reason) -> {
            progressDialog.hide();
            if (valid) {
                sheetIDPreference.setText((String) newValue);
            } else {
                Exception mLastError = reason;
                if (mLastError != null) {
                    if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                        showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) mLastError)
                                        .getConnectionStatusCode());
                    } else if (mLastError instanceof UserRecoverableAuthIOException) {
                        startActivityForResult(
                                ((UserRecoverableAuthIOException) mLastError).getIntent(),
                                REQUEST_AUTHORIZATION);
                    } else {
                        Toast.makeText(getContext().getApplicationContext(), "The following error occurred:\n"
                                + mLastError.getMessage(), Toast.LENGTH_SHORT);

                    }
                } else {
                    Toast.makeText(getContext().getApplicationContext(), "not a valid sheets id: "+reason.getMessage(), Toast.LENGTH_SHORT);
                }

            }
        }, (String) newValue);
        return false;

    }
}