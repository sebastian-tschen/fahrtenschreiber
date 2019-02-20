package bastel.de.fahrtenschreiber;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bastel.de.fahrtenschreiber.pojo.TripEntry;
import listeners.TripEntryUpdatedListener;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public abstract class FahrtenschreiberActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , EasyPermissions.PermissionCallbacks {

    private static final String F_TAG = "ftag";
    private static final Duration TRIP_ENTRY_TIMEOUT = Duration.ofMinutes(4);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    GoogleAccountCredential mCredential;
    private Button mCallApiButton;
    private Button mCallWriteApiButton;

    //    ProgressDialog mProgress;
    private boolean verbose = true;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};

    static final String SHEET_ID = "1x3AFBQ93jx5PXLcbiw3Dbr4jvFrCRRD0nWcFUALkUvo";
    private TripEntry latestTripEntry = null;
    private Instant latestTripEntryTimestamp = null;


    public void toast(String s) {

        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        Log.d(F_TAG, s);
    }

    public void debug(String s) {
        Log.d(F_TAG, s);
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
//                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    toast(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
//                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
//                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
//                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
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
                FahrtenschreiberActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public void getLatestTripEntryAsync(TripEntryUpdatedListener callback) {

        if ((latestTripEntry != null) &&
                Duration.between(latestTripEntryTimestamp, Instant.now()).getSeconds() < TRIP_ENTRY_TIMEOUT.getSeconds()) {
            callback.tripEntryUpdated(latestTripEntry);
        } else {
            new GetLastEntryTask(mCredential, callback).execute(SHEET_ID);
        }
    }

    class MakeWriteRequestTask extends AsyncTask<Object, Void, Integer> {


        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;

        TripEntryUpdatedListener callback;

        MakeWriteRequestTask(GoogleAccountCredential credential, TripEntryUpdatedListener callback) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
            this.callback = callback;
        }

        /**
         * Background task to call Google Sheets API.
         *
         * @param data no parameters needed for this task.
         */
        @Override
        protected Integer doInBackground(Object... data) {
            String spreadsheet = (String) data[0];
            TripEntry tripEntry = (TripEntry) data[1];
            try {
                return appendTripEntryRow(spreadsheet, tripEntry);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }


        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         *
         * @return List of names and majors
         * @throws IOException
         */
        private int appendTripEntryRow(String spreadsheet, TripEntry data) throws IOException {

//            String range = "Fahrten!A" + data.getRow() + ":D" + data.getRow();
            String range = "Fahrten!A2:D2";
            ValueRange input = new ValueRange();
            input.setRange(range);
            List<Object> rowData = new ArrayList<>();
            rowData.add(data.getDriver());
            rowData.add(data.getDate().format(DATE_TIME_FORMATTER));
            rowData.add(data.getOdo().toString());
            rowData.add("added via Fahrtenschreiber (TM)");
            List<List<Object>> matrixData = new ArrayList<>();
            matrixData.add(rowData);
            input.setValues(matrixData);
            AppendValuesResponse response = this.mService.spreadsheets().values()
                    .append(spreadsheet, range, input)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("OVERWRITE")
                    .execute();
            return 1;
        }

        private List<List<Object>> getListOf(String content, int outer, int inner) {
            List<List<Object>> outerList = new ArrayList<>();
            for (int o = 0; o < outer; o++) {
                List<Object> innerList = new ArrayList<>();
                outerList.add(innerList);
                for (int i = 0; i < inner; i++) {
                    innerList.add(content);
                }
            }
            return outerList;

        }


        @Override
        protected void onPreExecute() {
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(Integer output) {
            if (callback != null) {
                callback.tripEntryUpdated(null);
            }
        }

        @Override
        protected void onCancelled() {
//            mProgress.hide();
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
                    toast("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                toast("Request cancelled.");
            }
        }
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    class GetLastEntryTask extends AsyncTask<String, Void, TripEntry> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;
        private List<TripEntryUpdatedListener> listeners = new ArrayList<>();

        GetLastEntryTask(GoogleAccountCredential credential, TripEntryUpdatedListener listener) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
            if (listener != null) {
                listeners.add(listener);
            }
        }


        public void addListener(TripEntryUpdatedListener listener) {
            listeners.add(listener);
        }

        public boolean removeListener(TripEntryUpdatedListener listener) {
            return listeners.remove(listener);
        }

        /**
         * Background task to call Google Sheets API.
         *
         * @param spreadsheet the spreadsheet id to work on
         */
        @Override
        protected TripEntry doInBackground(String... spreadsheet) {
            try {
                return getLatestTripEntry(spreadsheet[0]);
            } catch (Exception e) {
                mLastError = e;
                Log.d("ftag", "error in requesting latest trip entry", e);
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         *
         * @param spreadsheet - the spreadsheet id to catch the data from
         * @return List of names and majors
         * @throws IOException
         */
        private TripEntry getLatestTripEntry(String spreadsheet) throws IOException {

            String range = "Fahrten!A2:D";
            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheet, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            if (values != null) {
                for (int i = 1; i < values.size(); i++) {
                    List<Object> row = values.get(values.size() - i);
                    if (row.size() >= 3) {
                        String driver = (String) row.get(0);
                        int odo = Integer.parseInt((String) row.get(2));
                        LocalDate date = null;
                        try {
                            date = LocalDate.parse((String) row.get(1), DATE_TIME_FORMATTER);
                        } catch (DateTimeParseException e) {
                            //date could not be parsed. just leave blank
                        }
                        return new TripEntry(driver, odo, date, values.size() + 2 - i);
                    }

                }
            }
            return new TripEntry(null, null, null, null);
        }


        @Override
        protected void onPreExecute() {
            if (verbose) {
                toast("finding last entry");
            }
        }

        @Override
        protected void onPostExecute(TripEntry lastTrip) {
            if (verbose) {
                latestTripEntry = lastTrip;
                latestTripEntryTimestamp = Instant.now();
                for (TripEntryUpdatedListener listener : listeners) {
                    listener.tripEntryUpdated(latestTripEntry);
                }
                toast("found last entry: " + lastTrip);
            }

        }

        @Override
        protected void onCancelled() {
//            mProgress.hide();
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
                    toast("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                toast("Request cancelled.");
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent newAct = new Intent(this, SettingsActivity.class);
            startActivity(newAct);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_record_trip) {
            Intent newAct = new Intent(this, TripRecordActivity.class);
            startActivity(newAct);
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        } else if (id == R.id.nav_record_refuel) {

            Intent newAct = new Intent(this, RefuelRecordActivity.class);
            startActivity(newAct);
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        } else if (id == R.id.nav_settings) {
            Intent newAct = new Intent(this, SettingsActivity.class);
            startActivity(newAct);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void invalidateLatestTripEntry() {
        latestTripEntry = null;
    }


}
