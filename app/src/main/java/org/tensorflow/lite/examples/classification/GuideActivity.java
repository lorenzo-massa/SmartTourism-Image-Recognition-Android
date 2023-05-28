package org.tensorflow.lite.examples.classification;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;
import org.tensorflow.lite.examples.classification.tflite.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mukesh.MarkdownView;

public class GuideActivity extends AppCompatActivity {

    private final String TAG = "GuideActivity";

    private Double xCord = 0d;
    private Double yCord = 0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_guide_md);

        String monumentId = getIntent().getStringExtra("monument_id");
        String language = getIntent().getStringExtra("language");

        Toolbar toolbar = (Toolbar) findViewById(R.id.topAppBar);
        toolbar.setTitle(monumentId);

        toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        MarkdownView markdownView = (MarkdownView) findViewById(R.id.markdown_view);
        markdownView.loadMarkdownFromAssets("guides/" + monumentId + "/" + language + "/guide.md"); //Loads the markdown file from the assets folder


        //Show hints

        ArrayList<Element> hints = getHints(monumentId);

        Button p1_button = findViewById(R.id.hintButton1);
        String monument1 = hints.get(0).getMonument();
        p1_button.setText(monument1);
        p1_button.setOnClickListener(view -> {
            Intent intent = new Intent(GuideActivity.this, GuideActivity.class);
            intent.putExtra("monument_id", monument1);
            intent.putExtra("language", language);
            startActivity(intent);
        });

        Button p2_button = findViewById(R.id.hintButton2);
        String monument2 = hints.get(1).getMonument();
        p2_button.setText(monument2);
        p2_button.setOnClickListener(view -> {
            Intent intent = new Intent(GuideActivity.this, GuideActivity.class);
            intent.putExtra("monument_id", monument2);
            intent.putExtra("language", language);
            startActivity(intent);
        });

        Button p3_button = findViewById(R.id.hintButton3);
        String monument3 = hints.get(2).getMonument();
        p3_button.setText(monument3);
        p3_button.setOnClickListener(view -> {
            Intent intent = new Intent(GuideActivity.this, GuideActivity.class);
            intent.putExtra("monument_id", monument3);
            intent.putExtra("language", language);
            startActivity(intent);
        });

        View hintsView = findViewById(R.id.hintsView);


        //Wait few seconds to let the md file open

        final Runnable r = () -> hintsView.setVisibility(View.VISIBLE);

        Handler handler = new Handler();
        handler.postDelayed(r, 2000);

        //Read text from markdown

        InputStream inputStream = null;
        try {
            inputStream = getAssets().open("guides/" + monumentId + "/" + language + "/guide.md");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        if (inputStream != null && readTextFile(inputStream)){
            Log.d(TAG, "Map coordinates: " + xCord + ", "+yCord);

            //Show button to open intent
            Uri gmmIntentUri = Uri.parse("geo:+ "+ xCord + ","+ yCord + "?q=" + Uri.encode(monumentId));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {

                Button mapsButton = findViewById(R.id.mapButton);
                mapsButton.setOnClickListener(view -> {
                    startActivity(mapIntent);
                    //finish();
                });
                mapsButton.setVisibility(View.VISIBLE);

            }

        }else{
            Log.d(TAG, "No coordinates found.");
        }





    }

    private Boolean readTextFile(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            line = reader.readLine();
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String patternString = "<!--\\s*([-+]?\\d*\\.\\d+)\\s+([-+]?\\d*\\.\\d+)\\s*-->";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String number1 = matcher.group(1);
            String number2 = matcher.group(2);
            Log.d(TAG, "Number 1: " + number1);
            Log.d(TAG, "Number 2: " + number2);
            assert number1 != null;
            xCord = Double.valueOf(number1);
            assert number2 != null;
            yCord = Double.valueOf(number2);
            return true;
        }

        return false;

    }

    private ArrayList<Element> getHints(String monumendId) { //hints just calculating the distances
        float[] recognizedVec = new float[0];
        ArrayList<Element> listDocToVec = DatabaseAccess.getListDocToVec();

        //find the vec of the recognized monument
        for (Element x:listDocToVec) {
            if(x.getMonument().equals(monumendId)){
                recognizedVec=x.getMatrix();
            }
        }

        //compute all distances
        for (Element x:listDocToVec) {
            x.setDistance(euclideanDistance(x.getMatrix(),recognizedVec));
        }

        //sort by distances
        Collections.sort(listDocToVec, new Comparator<Element>(){
            public int compare(Element obj1, Element obj2) {
                // ## Ascending order
                 return Double.compare(obj1.getDistance(), obj2.getDistance()); // To compare integer values
            }
        });
        Log.i(TAG, "listDocToVec: "+listDocToVec);

        ArrayList<Element> results = new ArrayList<>();
        results.add(listDocToVec.get(1));
        results.add(listDocToVec.get(2));
        results.add(listDocToVec.get(listDocToVec.size()-1));

        return results;
    }


    public static double euclideanDistance(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vector dimensions must be equal");
        }

        double sumOfSquares = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            sumOfSquares += diff * diff;
        }

        return Math.sqrt(sumOfSquares);
    }
}