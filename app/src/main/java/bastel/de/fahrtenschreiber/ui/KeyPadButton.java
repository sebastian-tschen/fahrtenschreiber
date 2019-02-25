package bastel.de.fahrtenschreiber.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.widget.AppCompatButton;

public class KeyPadButton extends MaterialButton {


    public KeyPadButton(Context context) {
        super(context);
    }

    public KeyPadButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KeyPadButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public int getValue() {

        return Integer.parseInt(getText().toString());

    }
}
