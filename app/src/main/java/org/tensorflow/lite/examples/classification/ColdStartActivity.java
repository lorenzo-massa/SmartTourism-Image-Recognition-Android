package org.tensorflow.lite.examples.classification;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

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

public class ColdStartActivity extends AppCompatActivity {

    private ListView listView;
    private String TAG = "ColdStartActivity";

    private String language;
    private ArrayList<String> listSelectedCategories = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cold_start);

        Log.d(TAG, "[INFO] onCreate ");

        language = getIntent().getStringExtra("language");

        Toolbar toolbar = (Toolbar) findViewById(R.id.topAppBar);
        toolbar.setTitle("Categories");
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));

        listView = findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, DatabaseAccess.getListCategories());
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
                    listSelectedCategories.add(s);
                }
            }
            //Close the activity only if at least one category is selected
            SharedPreferences sharedPreferences;
            if (listSelectedCategories.size() > 0) {
                sharedPreferences = getDefaultSharedPreferences(getApplicationContext());

                // Get the editor to make changes
                SharedPreferences.Editor editor = sharedPreferences.edit();

                for ( String category : DatabaseAccess.getListCategories() ) {
                    if(listSelectedCategories.contains(category))
                        editor.putBoolean("category_checkbox_" + category.toLowerCase(), true);
                    else
                        editor.putBoolean("category_checkbox_" + category.toLowerCase(), false);
                }

                // Commit the changes
                editor.apply();

                // Close the activity and start another one
                Intent intent = new Intent(ColdStartActivity.this, ColdStartActivitySkippable.class);
                intent.putExtra("language", language);
                startActivity(intent);
                overridePendingTransition(R.anim.slow_fade_in, R.anim.slow_fade_out);

                finish();
            } else
                Toast.makeText(this, "Choose at least one category", Toast.LENGTH_SHORT).show();
        });






    }

    @Override
    public void onBackPressed() {
        // Remove the super call to disable the back button functionality
        // super.onBackPressed();
        // Make a toast
        Toast.makeText(this, "Choose at least one category", Toast.LENGTH_SHORT).show();
    }
}
