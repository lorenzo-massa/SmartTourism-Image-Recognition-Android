package org.tensorflow.lite.examples.classification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

public class PreferencesActivity extends AppCompatActivity {

    private static final String TAG = "PreferencesActivity";
    public static String language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MySettingsFragment())
                .commit();

        //Toolbar
        Toolbar toolbar = findViewById(R.id.topAppBar);

        toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
            finish();
        });

    }

    public static class MySettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.activity_preferences, rootKey);

            // Find the preference and set the summary
            Log.d(TAG, "onCreatePreferences: " + rootKey);
            Preference preference = findPreference("pref_key_user_id");
            if (preference != null)
                preference.setSummary(DatabaseAccess.getSharedPreferences().getString("pref_key_user_id", "Not found"));

            // Register the listener to detect preference changes
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .registerOnSharedPreferenceChangeListener(this);

            //Set language entries programmatically
            ListPreference languagePreference = findPreference("pref_key_language");
            if (languagePreference != null) {
                languagePreference.setEntries(DatabaseAccess.getListLanguages().toArray(new String[0]));
                languagePreference.setEntryValues(DatabaseAccess.getListLanguages().toArray(new String[0]));
            }


        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            // Unregister the listener to avoid memory leaks
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        // Listener method to handle preference changes
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("pref_key_model")) {
                // Show the popup dialog when the specific preference is changed
                showRestartConfirmationDialog();
            }

            if (key.equals("pref_key_language")) {
                DatabaseAccess.setLanguage(sharedPreferences.getString(key, "English"));
                language = sharedPreferences.getString(key, "English");
            }

            if (key.equals("pref_key_num_threads")) {
                int minThreads = 1;
                int maxThreads = 8;

                int numThreads = Integer.parseInt(sharedPreferences.getString(key, "1"));
                if (numThreads < minThreads){
                    sharedPreferences.edit().putString(key, String.valueOf(minThreads)).apply();
                    Toast.makeText(requireContext(), "Minimum number of threads is 1", Toast.LENGTH_SHORT).show();
                } else if (numThreads > maxThreads) {
                    sharedPreferences.edit().putString(key, String.valueOf(maxThreads)).apply();
                    Toast.makeText(requireContext(), "Maximum number of threads is 8", Toast.LENGTH_SHORT).show();
                }

            }

        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
            if ("key_categories_settings".equals(key)) {
                startActivity(new Intent(getActivity(), CategoriesPreferencesActivity.class));
                return true;
            }

            if ("key_attributes_settings".equals(key)) {
                startActivity(new Intent(getActivity(), AttributesPreferencesActivity.class));
                return true;
            }

            return super.onPreferenceTreeClick(preference);
        }

        // Method to show the restart confirmation dialog
        private void showRestartConfirmationDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Restart Application");
            builder.setMessage("Changing this preference requires restarting the application. Do you want to restart now?");
            builder.setPositiveButton("Restart", (dialog, which) -> {
                // Restart the application
                restartApplication();
            });
            builder.setNegativeButton("Cancel", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        // Method to restart the application
        private void restartApplication() {
            Intent intent = new Intent(requireContext(), LoadingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ActivityCompat.finishAffinity(requireActivity());
            startActivity(intent);
        }


    }


}





