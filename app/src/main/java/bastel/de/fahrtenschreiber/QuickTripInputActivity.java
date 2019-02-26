package bastel.de.fahrtenschreiber;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import bastel.de.fahrtenschreiber.pojo.TripEntry;
import bastel.de.fahrtenschreiber.ui.KeyPadButton;

public class QuickTripInputActivity extends FahrtenschreiberActivity implements DatePickerDialog.OnDateSetListener {


    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.");
    private List<Integer> buttonsIds;
    private TextView newOdoReadingTextView;
    private TextView projectedDistanceTextView;

    private String enteredText = "";
    private TripEntry lastTripEntry = null;
    private String frontText = "";
    private LocalDate date = LocalDate.now();
    private Button dateButton;

    private Button driverButton;
    private Button commentButton;
    private FloatingActionButton addButton;

    List<View> buttons = new ArrayList<>();
    private String driver = null;
    private String comment = null;


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
            View view = findViewById(b_id);
            view.setOnClickListener(keyPadOnClickListener);
            buttons.add(view);
        }
        updateText();


        sheetsHelper.eventuallyGetLatestTrip(tripEntry -> {
            lastTripEntry = tripEntry;
            updateText();
        });


        dateButton = findViewById(R.id.date_button);
        dateButton.setText(date.format(DATE_FORMATTER));

        driverButton = findViewById(R.id.driver_button);
        driverButton.setText(getDriver());

        commentButton = findViewById(R.id.comment_button);

        addButton = findViewById(R.id.add_entry_button);

        buttons.add(commentButton);
        buttons.add(dateButton);
        buttons.add(driverButton);
        buttons.add(addButton);


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


    private void setAllButtonsEnabled(boolean enabled) {
        for (View button : buttons) {
            button.setEnabled(enabled);
        }
    }

    public void writeEntry(View view) {

        if (enteredText.isEmpty()) {
            return;
        }
        if (lastTripEntry != null) {
            Integer newOdo = Integer.parseInt(frontText + enteredText);
            String driver = getDriver();
            LocalDate date = this.date;
            String comment = getComment();
            Integer row = null;
            if (lastTripEntry.getRow() != null) {
                row = lastTripEntry.getRow() + 1;
            }
            TripEntry entry = new TripEntry(driver, newOdo, date, comment, row);
            sheetsHelper.tripEntryCallbacks.add(tripEntry -> {
                lastTripEntry = tripEntry;
                enteredText = "";
                updateText();
                setAllButtonsEnabled(true);
            });
            setAllButtonsEnabled(false);
            sheetsHelper.writeNewEntry(entry, this);

        } else {
            toast("letzter eintrag konnte nicht gefunden werden.");
        }
    }

    private String getDriver() {
        if (driver == null) {
            return getDefaultDriver();
        }
        return driver;
    }

    public String getComment() {
        if (comment==null){
            return getDefaultComment();
        }
        return comment;
    }

    public void pickDate(View view) {

        DatePickerDialog dialog = new DatePickerDialog(this);
        dialog.setOnDateSetListener(this);
        dialog.show();

    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        date = LocalDate.of(year, month + 1, dayOfMonth);
        dateButton.setText(date.format(DATE_FORMATTER));
    }

    @Override
    public void onCancel(Exception mLastError) {
        super.onCancel(mLastError);
        setAllButtonsEnabled(true);
    }

    public void pickDriver(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Fahrer");

        final EditText input = new EditText(this);
        input.setText(getDriver());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().isEmpty()) {
                    driver = input.getText().toString();
                    driverButton.setText(driver);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void pickComment(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kommentar");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                comment = input.getText().toString();
                commentButton.setText(comment);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
