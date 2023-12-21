package org.tensorflow.lite.examples.classification;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

import java.util.ArrayList;

public class ColdStartActivitySkippable extends AppCompatActivity {

    private ListView listView;
    private final String TAG = "ColdStartActivitySkippable";

    private String language;
    private final ArrayList<String> listSelectedAttributes = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cold_start_skippable);

        Log.d(TAG, "[INFO] onCreate ");


        language = getIntent().getStringExtra("language");

        Toolbar toolbar = findViewById(R.id.topAppBar);
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
                sharedPreferences = getDefaultSharedPreferences(getApplicationContext());

                // Get the editor to make changes
                SharedPreferences.Editor editor = sharedPreferences.edit();

                for (String attribute : DatabaseAccess.getListAttributes()) {
                    editor.putBoolean("attribute_checkbox_" + attribute.toLowerCase(), listSelectedAttributes.contains(attribute));
                }

                // Commit the changes
                editor.apply();

            }

            // Close the activity and start another one
            Intent intent = new Intent(ColdStartActivitySkippable.this, MainActivity.class);
            intent.putExtra("language", language);
            startActivity(intent);
            overridePendingTransition(R.anim.slow_fade_in, R.anim.slow_fade_out);

            finish();
        });

    }


}
