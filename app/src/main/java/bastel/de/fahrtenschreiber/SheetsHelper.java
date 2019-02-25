package bastel.de.fahrtenschreiber;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import androidx.preference.PreferenceManager;
import bastel.de.fahrtenschreiber.pojo.TripEntry;
import listeners.CheckIdCallback;
import listeners.EventuallyGetLatestTripCallback;
import listeners.OnCancelledListener;
import listeners.TripEntryUpdatedListener;

public class SheetsHelper {

    public static final String TAG = "nobbi";
    private static SheetsHelper instance = null;

    static final String SHEET_ID = "1x3AFBQ93jx5PXLcbiw3Dbr4jvFrCRRD0nWcFUALkUvo";
    private TripEntry latestTripEntry = null;
    private Instant latestTripEntryTimestamp = null;
    private static final Duration TRIP_ENTRY_TIMEOUT = Duration.ofMinutes(4);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");


    private GoogleAccountCredential mCredential;
    private Context appContext;
    private Sheets mService;


    public String getSheetId() {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
                .getString("sheet_id", SHEET_ID);
    }


    private SheetsHelper() {
    }

    public synchronized static SheetsHelper getInstance() {
        if (instance == null) {
            instance = new SheetsHelper();
        }
        return instance;
    }

    /**
     * initialize this singelton if it is not already initialized.
     *
     * @param appContext
     * @param credential
     */
    public synchronized void init(Context appContext, GoogleAccountCredential credential) {
        if (!isInitialized()) {
            mCredential = credential;
            this.appContext = appContext;

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();

        }
    }

    public synchronized boolean isInitialized() {
        return mCredential != null;
    }

    ArrayList<EventuallyGetLatestTripCallback> tripEntryCallbacks = new ArrayList<>();
    private boolean lastEntryRetrievalRunning = false;

    private void toast(String s) {

        Toast.makeText(appContext, s, Toast.LENGTH_SHORT).show();
        Log.d(TAG, s);
    }

    private boolean isLastEntryAvailable() {
        return (latestTripEntry != null) &&
                Duration.between(latestTripEntryTimestamp, Instant.now()).getSeconds() < TRIP_ENTRY_TIMEOUT.getSeconds();
    }

    public void checkSheetsId(CheckIdCallback callback, String sheetsId) {
        new GetLastEntryTask(latestTripEntry1 -> callback.sheetsIdChecked(true, null),
                error -> callback.sheetsIdChecked(false, error)).execute(sheetsId);

    }

    class MakeWriteRequestTask extends AsyncTask<Object, Void, TripEntry> {


        private final OnCancelledListener onCancelledCallback;
        private Exception mLastError = null;

        TripEntryUpdatedListener callback;

        MakeWriteRequestTask(TripEntryUpdatedListener callback, OnCancelledListener onCancelledCallback) {

            this.callback = callback;
            this.onCancelledCallback = onCancelledCallback;
        }


        /**
         * Background task to call Google Sheets API.
         *
         * @param data no parameters needed for this task.
         */
        @Override
        protected TripEntry doInBackground(Object... data) {
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
        private TripEntry appendTripEntryRow(String spreadsheet, TripEntry data) throws IOException {

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
            AppendValuesResponse response = mService.spreadsheets().values()
                    .append(spreadsheet, range, input)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("OVERWRITE")
                    .execute();
            return data;
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
        protected void onPostExecute(TripEntry output) {
            if (callback != null) {
                callback.tripEntryUpdated(output);
            }
        }

        @Override
        protected void onCancelled() {
            onCancelledCallback.onCancel(mLastError);
        }

    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    class GetLastEntryTask extends AsyncTask<String, Void, TripEntry> {
        private final OnCancelledListener onCancelledListener;
        private Exception mLastError = null;
        private List<TripEntryUpdatedListener> listeners = new ArrayList<>();

        GetLastEntryTask(TripEntryUpdatedListener listener, OnCancelledListener onCancelledListener) {

            this.onCancelledListener = onCancelledListener;
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
            ValueRange response = mService.spreadsheets().values()
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
            Log.d("nobbi", "retrieve last entry");
        }

        @Override
        protected void onPostExecute(TripEntry lastTrip) {
            latestTripEntry = lastTrip;
            latestTripEntryTimestamp = Instant.now();
            for (TripEntryUpdatedListener listener : listeners) {
                listener.tripEntryUpdated(latestTripEntry);
            }
            toast("found last entry: " + lastTrip);

        }

        @Override
        protected void onCancelled() {
            onCancelledListener.onCancel(mLastError);
        }

    }


    public void invalidateLatestTripEntry() {
        latestTripEntry = null;
    }


    public synchronized void eventuallyGetLatestTrip(EventuallyGetLatestTripCallback callback) {
        if (isLastEntryAvailable()) {
            callback.lastTripRecieved(latestTripEntry);
            return;
        }
        tripEntryCallbacks.add(callback);
        if (!lastEntryRetrievalRunning && mCredential.getSelectedAccount() != null) {
            lastEntryRetrievalRunning = true;
            new GetLastEntryTask(this::writeNewLatestTripValue, error -> {
                if (error != null) {
                    toast("the following error occurd: " + error.getMessage());
                } else {
                    toast("an unknown error occured");
                }
                lastEntryRetrievalRunning = false;
            }).execute(getSheetId());
        }
    }


    public synchronized void writeNewLatestTripValue(TripEntry latestTrip) {
        latestTripEntry = latestTrip;
        latestTripEntryTimestamp = Instant.now();
        for (EventuallyGetLatestTripCallback callback : tripEntryCallbacks) {
            callback.lastTripRecieved(latestTripEntry);
        }
        tripEntryCallbacks.clear();
        lastEntryRetrievalRunning = false;
    }


    public void writeNewEntry(TripEntry data, OnCancelledListener onCancelledListener) {
        Log.d(TAG, "write entry: " + data);
        invalidateLatestTripEntry();
        new MakeWriteRequestTask(this::entryWritten, onCancelledListener).execute(getSheetId(), data);
    }

    private void entryWritten(TripEntry tripEntry) {
        toast("written " + tripEntry.getOdo() + "km");
        eventuallyGetLatestTrip(tripEntry1 -> {
        });
    }

}
