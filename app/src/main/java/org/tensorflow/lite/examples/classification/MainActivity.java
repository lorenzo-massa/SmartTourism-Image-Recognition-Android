package org.tensorflow.lite.examples.classification;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

public class MainActivity extends AppCompatActivity implements MonumentAdapter.OnButtonClickListener {

    private String language;
    private String uniqueID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        language = getIntent().getStringExtra("language");
        uniqueID = getIntent().getStringExtra("user_id");

        DatabaseAccess databaseAccess = DatabaseAccess.getInstance();
        databaseAccess.updateDatabaseColdStart(language);


        //Preferences button
        ActionMenuItemView btnPreferences = findViewById(R.id.settings);
        btnPreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
                startActivity(intent);
            }
        });

        //FAB button
        FloatingActionButton btnFAB = findViewById(R.id.fab);
        btnFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ClassifierActivity.class);
                intent.putExtra("language", language);
                startActivity(intent);
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        //Update attributes and categories if you edit preferences
        RecyclerView recyclerView = findViewById(R.id.listCardView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        MonumentAdapter monumentAdapter = new MonumentAdapter(DatabaseAccess.getListCategoriesOrdered(), this);
        recyclerView.setAdapter(monumentAdapter);
    }

    @Override
    public void onButtonClick(String monument) {
        Intent intent = new Intent(MainActivity.this, GuideActivity.class);
        intent.putExtra("monument_id", monument);
        intent.putExtra("language", language);
        intent.putExtra("user_id", uniqueID);
        startActivity(intent);
    }

}

