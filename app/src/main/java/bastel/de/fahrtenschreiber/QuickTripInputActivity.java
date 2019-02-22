package bastel.de.fahrtenschreiber;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import bastel.de.fahrtenschreiber.pojo.TripEntry;
import bastel.de.fahrtenschreiber.ui.KeyPadButton;

public class QuickTripInputActivity extends FahrtenschreiberActivity {


    private List<Integer> buttonsIds;
    private TextView newOdoReadingTextView;
    private TextView projectedDistanceTextView;

    private String enteredText = "";
    private TripEntry lastTripEntry = null;
    private String frontText = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_quick_trip);
        super.onCreate(savedInstanceState);

        newOdoReadingTextView = findViewById(R.id.new_odo_reading);
        projectedDistanceTextView = findViewById(R.id.projected_trip_distance);

        View.OnClickListener keyPadOnClickListener = v -> {
            KeyPadButton button = (KeyPadButton) v;
            try {

                String newText = enteredText + button.getText().toString();
                Integer.parseInt(newText);
                enteredText = newText;
            } catch (NumberFormatException e) {
                //the new text is not parsable as int. ignore input
            }
            updateText();
        };

        buttonsIds = Arrays.asList(
                R.id.kpb_0,
                R.id.kpb_1,
                R.id.kpb_2,
                R.id.kpb_3,
                R.id.kpb_4,
                R.id.kpb_5,
                R.id.kpb_6,
                R.id.kpb_7,
                R.id.kpb_8,
                R.id.kpb_9);

        for (int b_id : buttonsIds) {
            findViewById(b_id).setOnClickListener(keyPadOnClickListener);
        }
        updateText();


        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        }
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
        eventuallyGetLatestTrip(tripEntry -> {
            lastTripEntry = tripEntry;
            updateText();
        });


    }

    private void updateText() {
        frontText = "";
        String projectedText = "0";
        if (lastTripEntry != null && lastTripEntry.getOdo() != null) {
            if (enteredText.isEmpty()) {
                frontText = lastTripEntry.getOdo().toString();
            } else if (enteredText.length() < lastTripEntry.getOdo().toString().length()) {
                String odo = lastTripEntry.getOdo().toString();
                String back = odo.substring(odo.length() - enteredText.length());
                frontText = odo.substring(0, odo.length() - enteredText.length());
                if (Integer.parseInt(back) > Integer.parseInt(enteredText)) {
                    frontText = Integer.toString(Integer.parseInt(frontText) + 1);
                }

            }
            Integer newOdo = Integer.parseInt(frontText + enteredText);
            projectedText = Integer.toString(newOdo - lastTripEntry.getOdo());

        }
        newOdoReadingTextView.setText(Html.fromHtml("<font color=\"#c0c0c0\">" + frontText + "</>" + "<b>" + enteredText + "</b>"));
        projectedDistanceTextView.setText(projectedText);

    }

    public void backspace(View view) {
        if (!enteredText.isEmpty()) {
            enteredText = enteredText.substring(0, enteredText.length() - 1);
            updateText();
        }
    }

    public void delete(View view) {
        enteredText = "";
        updateText();
    }

    public void writeEntry(View view) {

        if (lastTripEntry != null) {
            Integer newOdo = Integer.parseInt(frontText + enteredText);
            String driver = getDefaultDriver();
            LocalDate date = LocalDate.now();
            Integer row = null;
            if (lastTripEntry.getRow() != null) {
                row = lastTripEntry.getRow() + 1;
            }
            TripEntry entry = new TripEntry(driver, newOdo, date, row);
            tripEntryCallbacks.add(tripEntry -> {
                lastTripEntry = tripEntry;
                enteredText="";
                updateText();
            });
            writeNewEntry(entry);


        } else {
            toast("letztter eintrag konnte nicht gefunden werden.");
        }


    }


}
