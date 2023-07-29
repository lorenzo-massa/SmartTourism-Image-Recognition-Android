package org.tensorflow.lite.examples.classification;


import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ProgressBar;
        import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;
import org.tensorflow.lite.examples.classification.tflite.DatabaseUpdateListener;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class LoadingActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private final String TAG = "LoadingActivity";

    private String language;
    private String uniqueID;

    private Classifier.Model model;

    private boolean firstStart;

    //Shared Preferences
    public SharedPreferences sharedPreferences;

    public SharedPreferences.OnSharedPreferenceChangeListener shared_listener;

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

        progressBar = findViewById(R.id.progressBar);

        //SharedPreferences

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        uniqueID = sharedPreferences.getString("pref_key_user_id", "");

        if(uniqueID.equals("")){
            firstStart = true;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(System.currentTimeMillis());
            uniqueID = date + "-" + UUID.randomUUID().toString() ;
            sharedPreferences.edit().putString("pref_key_user_id", uniqueID).apply();
        }else{
            firstStart = false;
        }

        language = sharedPreferences.getString("pref_key_language", "English");
        model = Classifier.Model.valueOf(sharedPreferences.getString("pref_key_model", "Precise"));


        shared_listener = (prefs, key) -> {
            if(key.equals("pref_key_language")){
                language = prefs.getString("pref_key_language", "English");
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(shared_listener);


        // Check permissions

        if (hasPermission() && hasPermissionGPS() && hasPermissionNotification()) {
            //setFragment(); //first creation of classifier (maybe never called)
            new LoadingActivityTask().execute();         // Start the database upload process
        } else {
            requestPermission();
        }





    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    private boolean hasPermissionGPS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    private boolean hasPermissionNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
            Toast.makeText(
                            LoadingActivity.this,
                            "Camera permission is required for this demo",
                            Toast.LENGTH_LONG)
                    .show();
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(
                    LoadingActivity.this,
                    "Location permission is required for this demo",
                    Toast.LENGTH_LONG)
                    .show();
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            Toast.makeText(
                            LoadingActivity.this,
                            "Notification permission is required for this demo",
                            Toast.LENGTH_LONG)
                    .show();
        }


        requestPermissions(new String[]{PERMISSION_CAMERA,Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST);

    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (hasPermission() && hasPermissionGPS() && hasPermissionNotification()) {
                new LoadingActivityTask().execute();
            } else {
                requestPermission();
            }
        }
    }



    private class LoadingActivityTask extends AsyncTask<Void, Integer, Void> implements DatabaseUpdateListener {

        @Override
        protected Void doInBackground(Void... voids) {
            // Perform the database upload process here
            // Update the progress using publishProgress() method

            String dbName = "";

            switch (model) {
                case Precise:
                    dbName = "MobileNetV3_Large_100_db.sqlite";
                    break;
                case Medium:
                    dbName = "MobileNetV3_Large_075_db.sqlite";
                    break;
                case Fast:
                    dbName = "MobileNetV3_Small_100_db.sqlite";
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            DatabaseAccess databaseAccess = DatabaseAccess.getInstance(LoadingActivity.this, dbName);
            databaseAccess.setDatabaseUpdateListener(this);

            databaseAccess.updateDatabase(5);
            databaseAccess.updateDatabaseColdStart();
            databaseAccess.uploadDatabaseMonuments();
            databaseAccess.uploadLanguages();

            //TODO check if this is needed, android should create the database on its own
            //databaseAccess.setOpenHelperLoggers();
            //databaseAccess.updateMonumentInteractions();
            //databaseAccess.closeOpenHelperLoggers();

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // Update the progress indicator
            int progress = values[0];
            progressBar.setProgress(progress);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Database upload is complete
            //Toast.makeText(getApplicationContext(), "Database upload completed", Toast.LENGTH_SHORT).show();
            // You can start the next activity or finish the current activity here

            if (firstStart){
                Intent intent = new Intent(LoadingActivity.this, ColdStartActivity.class);
                intent.putExtra("language", language.toString());
                startActivity(intent);
                overridePendingTransition(R.anim.slow_fade_in, R.anim.slow_fade_out);
            }else {
                Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
                intent.putExtra("language", language.toString());
                startActivity(intent);
                overridePendingTransition(R.anim.slow_fade_in, R.anim.slow_fade_out);
            }

            finish();
        }

        @Override
        public void onDatabaseUpdateProgress(int progress) {
            publishProgress(progress);
        }
    }
}

