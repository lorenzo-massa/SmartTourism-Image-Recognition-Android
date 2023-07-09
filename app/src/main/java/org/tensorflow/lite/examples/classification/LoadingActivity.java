package org.tensorflow.lite.examples.classification;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
        import android.widget.Toast;

        import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;
import org.tensorflow.lite.examples.classification.tflite.DatabaseUpdateListener;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

public class LoadingActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private String TAG = "LoadingActivity";

    private String language;
    private String uniqueID;

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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(System.currentTimeMillis());
            uniqueID = date + "-" + UUID.randomUUID().toString() ;
            sharedPreferences.edit().putString("pref_key_user_id", uniqueID).apply();
        }

        language = sharedPreferences.getString("pref_key_language", "English");


        shared_listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if(key.equals("pref_key_language")){
                    language = prefs.getString("pref_key_language", "English");
                }
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(shared_listener);


        // Check permissions

        if (hasPermission() && hasPermissionGPS()){
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

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

            requestPermissions(new String[]{PERMISSION_CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (hasPermission() && hasPermissionGPS()) {
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

            DatabaseAccess databaseAccess = DatabaseAccess.getInstance(LoadingActivity.this, "");
            databaseAccess.setDatabaseUpdateListener(this);
            databaseAccess.open();
            databaseAccess.updateDatabase(5, language);
            databaseAccess.updateDatabaseColdStart(language);
            databaseAccess.updateDatabaseDocToVec(language);
            databaseAccess.setOpenHelperLoggers();
            databaseAccess.updateMonumentInteractions();
            databaseAccess.closeOpenHelperLoggers();
            databaseAccess.close();

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

            Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
            intent.putExtra("language", language);
            startActivity(intent);
            overridePendingTransition(R.anim.slow_fade_in, R.anim.slow_fade_out);
            finish();
        }

        @Override
        public void onDatabaseUpdateProgress(int progress) {
            publishProgress(progress);
        }
    }
}

