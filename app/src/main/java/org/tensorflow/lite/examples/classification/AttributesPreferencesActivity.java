package org.tensorflow.lite.examples.classification;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

import java.util.List;

public class AttributesPreferencesActivity extends AppCompatActivity {

    private static final Logger LOGGER = new Logger();
    private final String TAG = "PreferencesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences_attributes);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MySettingsAttributesFragment())
                .commit();

        //Toolbar
        Toolbar toolbar = findViewById(R.id.topAppBar);

        toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
            finish();
        });

    }

    public static class MySettingsAttributesFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.activity_preferences_attributes, rootKey);

            // Get the preference category
            PreferenceCategory checkboxAttribute = findPreference("attribute_checkbox");

            // Get the list of strings
            List<String> stringList = DatabaseAccess.getListAttributes();

            // Add checkbox preferences dynamically
            for (String string : stringList) {
                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(requireContext());
                checkBoxPreference.setTitle(string);
                checkBoxPreference.setKey("attribute_checkbox_" + string.toLowerCase());
                checkBoxPreference.setChecked(false); // Set initial checked state

                checkBoxPreference.setIconSpaceReserved(false);

                checkboxAttribute.addPreference(checkBoxPreference);
            }
        }
    }


}
