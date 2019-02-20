package bastel.de.fahrtenschreiber;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import bastel.de.fahrtenschreiber.pojo.TripEntry;

public class TripRecordActivity extends FahrtenschreiberActivity {

    private LocalDate selectedDate = LocalDate.now();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_record_trip);
        super.onCreate(savedInstanceState);
        ((CalendarView) findViewById(R.id.tripCalendarView)).setOnDateChangeListener((view, year, month, dayOfMonth) -> selectedDate = LocalDate.of(year, month + 1, dayOfMonth));


        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        }
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
        if (isDeviceOnline()) {
            getLatestTripEntryAsync(latestTripEntry -> {
                ((EditText) findViewById(R.id.odometer_reading)).setText(latestTripEntry.getOdo().toString());
                ((EditText) findViewById(R.id.driver)).setText(latestTripEntry.getDriver());
            });
        }
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

            TripEntry data = getData();
            writeNewEntry(data);

        }
    }

    private TripEntry getData() {
        int odo = Integer.parseInt(((EditText) findViewById(R.id.odometer_reading)).getText().toString());
        String driver = ((EditText) findViewById(R.id.driver)).getText().toString();
        return new TripEntry(driver, odo, selectedDate, null);

    }

    public void writeNewEntry(TripEntry data) {
        debug("write entry: " + data);
        invalidateLatestTripEntry();
        new MakeWriteRequestTask(mCredential, latestTripEntry -> toast("written " + data.getOdo() + "km")).execute(SHEET_ID, data);
    }


}
