package bastel.de.fahrtenschreiber;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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
    private void writeToApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        }
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
        if (!isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeWriteRequestTask(mCredential).execute();
        }
    }
}
