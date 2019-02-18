package bastel.de.fahrtenschreiber;

import android.os.Bundle;
import android.view.View;

public class TripRecordActivity extends FahrtenschreiberActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_record_trip);
        super.onCreate(savedInstanceState);
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */

    public void writeToApi(View view) {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        }
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
        if (!isDeviceOnline()) {
            toast("No network connection available.");

        } else {
            new GetLastEntryTask(mCredential).execute(SHEET_ID);
        }
    }

}
