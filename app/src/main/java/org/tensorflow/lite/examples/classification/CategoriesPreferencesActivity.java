package org.tensorflow.lite.examples.classification;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

import java.util.List;

public class CategoriesPreferencesActivity extends AppCompatActivity {

    private static final Logger LOGGER = new Logger();
    private String TAG = "PreferencesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences_categories);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MySettingsCategoriesFragment())
                .commit();

        //Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.topAppBar);

        toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
            finish();
        });

    }

    public static class MySettingsCategoriesFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.activity_preferences_categories, rootKey);

            // Get the preference category
            PreferenceCategory checkboxCategory = findPreference("category_checkbox");

            // Get the list of strings
            List<String> stringList = DatabaseAccess.getListCategories();

            // Add checkbox preferences dynamically
            for (String string : stringList) {
                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(requireContext());
                checkBoxPreference.setTitle(string);
                checkBoxPreference.setKey("category_checkbox_" + string.toLowerCase());
                checkBoxPreference.setChecked(false); // Set initial checked state

                checkBoxPreference.setIconSpaceReserved(false);

                checkboxCategory.addPreference(checkBoxPreference);
            }
        }
    }



}
