package org.tensorflow.lite.examples.classification;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ColdStartActivitySkippable extends AppCompatActivity {

    private ListView listView;
    private String TAG = "ColdStartActivitySkippable";

    private String language;
    private ArrayList<String> listSelectedAttributes = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cold_start_skippable);

        Log.d(TAG, "[INFO] onCreate ");


        language = getIntent().getStringExtra("language");

        Toolbar toolbar = (Toolbar) findViewById(R.id.topAppBar);
        toolbar.setTitle("Attributes");
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));

        listView = findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, DatabaseAccess.getListAttributes());
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        //Once the button is presset, get the selected categories and add them in a list

        Button b = findViewById(R.id.button);

        b.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: " + listView.getCheckedItemPositions());
            for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                if (listView.isItemChecked(i)) {
                    String s = (String) listView.getItemAtPosition(i);
                    Log.d(TAG, "Item Selected: " + s);
                    listSelectedAttributes.add(s);
                }
            }

            SharedPreferences sharedPreferences;
            if (listSelectedAttributes.size() > 0) {
                sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE);

                // Get the editor to make changes
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // Convert the list of strings to a single string
                Set<String> stringSet = new HashSet<>(listSelectedAttributes);

                // Save the string set to SharedPreferences
                editor.putStringSet("selectedAttributes", stringSet);

                // Commit the changes
                editor.apply();

            }
            finish();
        });






    }


}
