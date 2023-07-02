package org.tensorflow.lite.examples.classification;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.tensorflow.lite.examples.classification.env.Logger;

public class PreferencesActivity extends AppCompatActivity {

    private static final Logger LOGGER = new Logger();
    private String TAG = "PreferencesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MySettingsFragment())
                .commit();

        //Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.topAppBar);

        toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
            finish();
        });
    }

    public static class MySettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.activity_preferences, rootKey);
        }
    }

}





