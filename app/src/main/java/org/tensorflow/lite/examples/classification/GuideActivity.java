package org.tensorflow.lite.examples.classification;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;
import org.tensorflow.lite.examples.classification.tflite.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mukesh.MarkdownView;

public class GuideActivity extends AppCompatActivity {

    private final String TAG = "GuideActivity";

    private TextView textView;
    private String user_id;
    private String monumentId;
    private String language;
    private String user_id_db;
    private String monument_id_db;
    private String monumentDescription;

    private boolean recommendationsReceived = false;
    private ArrayList<Element> hints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_guide_md);

        monumentId = getIntent().getStringExtra("monument_id");
        language = getIntent().getStringExtra("language");
        user_id = getIntent().getStringExtra("user_id");



        Toolbar toolbar = (Toolbar) findViewById(R.id.topAppBar);
        toolbar.setTitle(monumentId);

        toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        //Share button
        ActionMenuItemView btnShare = findViewById(R.id.shareButton);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Instantiate an intent
                Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("image/*");

                String link = DatabaseAccess.getImageLink(monumentId);

                // Get bitmap from assets
/*
                InputStream is = null;
                try {
                    is = getAssets().open("categories/Arte.jpg");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                Uri uri = getmageToShare(bitmap);
*/
                //Save image from url
                Glide.with(GuideActivity.this)
                        .load(link)
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {

                                Bitmap bitmap = ((BitmapDrawable)resource).getBitmap();
                                bitmap = addWatermark(bitmap);

                                //Toast.makeText(GuideActivity.this, "Saving Image...", Toast.LENGTH_SHORT).show();
                                Uri uri = getmageToShare(bitmap);

                                // Add the URI to the Intent.
                                intent.putExtra(Intent.EXTRA_STREAM, uri);

                                // Add extra text to the Intent (optional)
                                intent.putExtra(Intent.EXTRA_TEXT, "I'm visiting " + monumentId + " with SmartTourism app!");
                                intent.putExtra(Intent.EXTRA_TITLE, "SmartTourism");
                                intent.putExtra(Intent.EXTRA_SUBJECT, "SmartTourism");

                                // Broadcast the Intent.
                                startActivity(Intent.createChooser(intent, "Share to"));

                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                super.onLoadFailed(errorDrawable);

                                Toast.makeText(GuideActivity.this, "Failed to Download Image! Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        });





            }

            // Retrieving the url to share
            private Uri getmageToShare(Bitmap bitmap) {
                File imagefolder = new File(getCacheDir(), "images");
                Uri uri = null;
                try {
                    imagefolder.mkdirs();
                    File file = new File(imagefolder, "shared_image.jpeg");
                    FileOutputStream outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    uri = FileProvider.getUriForFile(GuideActivity.this, "org.tensorflow.lite.examples.classification.fileprovider", file);
                } catch (Exception e) {
                    Toast.makeText(GuideActivity.this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                return uri;
            }

            // Add watermark to image
            private Bitmap addWatermark(Bitmap src) {
                int w = src.getWidth();
                int h = src.getHeight();
                Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
                Canvas canvas = new Canvas(result);
                canvas.drawBitmap(src, 0, 0, null);

                //Draw Text
                /*
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setTextSize(15);
                paint.setAntiAlias(true);
                paint.setUnderlineText(false);
                canvas.drawText(watermark, 10, h-10, paint);
                 */

                //Draw drawable
                Drawable drawable = getResources().getDrawable(R.drawable.logo_name);
                Bitmap bitmapLogo = ((BitmapDrawable)drawable).getBitmap();
                //Resize bitmap
                bitmapLogo = BITMAP_RESIZER(bitmapLogo, 50, 50);
                canvas.drawBitmap(bitmapLogo, w-60, h-60, new Paint(Paint.FILTER_BITMAP_FLAG));

                return result;
            }

            private Bitmap BITMAP_RESIZER(Bitmap bitmap,int newWidth,int newHeight) {
                Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

                float ratioX = newWidth / (float) bitmap.getWidth();
                float ratioY = newHeight / (float) bitmap.getHeight();
                float middleX = newWidth / 2.0f;
                float middleY = newHeight / 2.0f;

                Matrix scaleMatrix = new Matrix();
                scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

                Canvas canvas = new Canvas(scaledBitmap);
                canvas.setMatrix(scaleMatrix);
                canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

                return scaledBitmap;

            }
        });

        MarkdownView markdownView = (MarkdownView) findViewById(R.id.markdown_view);
        markdownView.loadMarkdownFromAssets("guides/" + monumentId + "/" + language + "/guide.md"); //Loads the markdown file from the assets folder


        double[] coordinates = DatabaseAccess.getCoordinates(monumentId);

        //Log monument interaction
        DatabaseAccess.getInstance().log(monumentId);


        if (coordinates != null){
            Log.d(TAG, "Map coordinates: " + coordinates[0] + ", "+coordinates[1]);

            //Show button to open intent
            Uri gmmIntentUri = Uri.parse("geo:+ "+ coordinates[0] + ","+ coordinates[1] + "?q=" + Uri.encode(monumentId));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {

                Button mapsButton = findViewById(R.id.mapButton);
                mapsButton.setOnClickListener(view -> {
                    startActivity(mapIntent);
                    //finish();
                });
                mapsButton.setVisibility(View.VISIBLE);

            } else {
                Log.w(TAG, "No maps app found.");
            }

        }else{
            Log.d(TAG, "No coordinates found.");
        }


        //Send request to API server
        textView = findViewById(R.id.textView);
        if (internetIsConnected()){
            //volley_request();
            //For the moment we use the local DocToVec
        }

        //Show hints
        //Wait few seconds to let the md file open
        final Runnable r = this::showHInts;
        Handler handler = new Handler();
        handler.postDelayed(r, 2000);

    }

    private void showHInts(){
        View hintsView = findViewById(R.id.hintsView);

        if(!recommendationsReceived){
            hints = getHints(monumentId); //local DocToVec
        }

        Button p1_button = findViewById(R.id.hintButton1);
        String monument1 = hints.get(0).getMonument();
        p1_button.setText(monument1);
        p1_button.setOnClickListener(view -> {
            Intent intent = new Intent(GuideActivity.this, GuideActivity.class);
            intent.putExtra("monument_id", monument1);
            intent.putExtra("language", language);
            intent.putExtra("user_id", user_id);

            startActivity(intent);
        });

        Button p2_button = findViewById(R.id.hintButton2);
        String monument2 = hints.get(1).getMonument();
        p2_button.setText(monument2);
        p2_button.setOnClickListener(view -> {
            Intent intent = new Intent(GuideActivity.this, GuideActivity.class);
            intent.putExtra("monument_id", monument2);
            intent.putExtra("language", language);
            intent.putExtra("user_id", user_id);
            startActivity(intent);
        });

        Button p3_button = findViewById(R.id.hintButton3);
        String monument3 = hints.get(2).getMonument();
        p3_button.setText(monument3);
        p3_button.setOnClickListener(view -> {
            Intent intent = new Intent(GuideActivity.this, GuideActivity.class);
            intent.putExtra("monument_id", monument3);
            intent.putExtra("language", language);
            intent.putExtra("user_id", user_id);
            startActivity(intent);
        });

        hintsView.setVisibility(View.VISIBLE);

    }


    /*
    private void volley_request(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://smart-tourism.onrender.com/user";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //textView.setText("Response is: " + response);

                        //Log in + get user_id_db
                        List<String> persIds = jsonGetUsers(response);
                        Log.d(TAG, "persIds: " + persIds.toString());
                        if(persIds == null || !persIds.contains(user_id)){
                            //Register id to the server
                            registerNewUser(user_id);
                        }else{
                            requestUserId();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("volley_request: JSON GET Request didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    private void checkMonument(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://smart-tourism.onrender.com/monument";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        List<String> monumentsIds = jsonGetMonuments(response);
                        Log.d(TAG, "monumentsIds: " + monumentsIds.toString());
                        if(monumentsIds == null || !monumentsIds.contains(monumentId)){
                            //Register id to the server
                            registerNewMonument();
                        }else{
                            requestMonumentId();
                        }



                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("checkMonument: JSON GET Request didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    private void requestUserId(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://smart-tourism.onrender.com/user";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        user_id_db = jsonGetUserId(response);
                        Log.d(TAG, "user_id_db: " + user_id_db);

                        checkMonument(); //check if the monument already exist + get monument_id_db
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("requestUserId: JSON GET Request didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    private void requestMonumentId(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://smart-tourism.onrender.com/monument";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        monument_id_db = jsonGetMonumentId(response);
                        Log.d(TAG, "monument_id_db: " + monument_id_db);


                        if (!Objects.equals(user_id_db, "") && !Objects.equals(monument_id_db, "")
                                && !Objects.equals(user_id_db, "ID_DB NOT FOUND") && !Objects.equals(monument_id_db, "ID_DB NOT FOUND")
                                && user_id_db != null && monument_id_db != null){
                            Log.d(TAG,"Interacting ...");
                            makeInteraction();
                        }else{
                            Log.d(TAG,"NOT interacting ...");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("requestUserId: JSON GET Request didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    private String jsonGetUserId(String jsonString){

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            //List<String> IDs = new ArrayList<>();
            String id = "ID_DB NOT FOUND";

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                id =  jsonObject.getString("id");
                //IDs.add(id);
            }

            //Log.d(TAG, "user_id_db: " + id);

            return id;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    private String jsonGetMonumentId(String jsonString){

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            //List<String> IDs = new ArrayList<>();
            String id = "ID_DB NOT FOUND";

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                id =  jsonObject.getString("id");
                //IDs.add(id);
            }

            //Log.d(TAG, "monument_id_db: " + id);

            return id;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    private List<String> jsonGetUsers(String jsonString){

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            List<String> persIds = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String persId = jsonObject.getString("pers_id");
                persIds.add(persId);
            }

            //Log.d(TAG, "persIds: " + persIds);

            return persIds;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    private List<String> jsonGetMonuments(String jsonString){

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            List<String> monumentIds = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String monumentId = jsonObject.getString("name");
                monumentIds.add(monumentId);
            }

            //Log.d(TAG, "monumentIds: " + monumentIds);

            return monumentIds;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    private boolean jsonGetRecommendation(String jsonString){

        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            hints.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String r1 = jsonObject.getString("r1");
                String r2 = jsonObject.getString("r2");
                String r3 = jsonObject.getString("r3");
                Element e1 = new Element(r1,null,0);
                Element e2 = new Element(r1,null,0);
                Element e3 = new Element(r1,null,0);

                hints.add(e1);
                hints.add(e2);
                hints.add(e3);
            }

            //Log.d(TAG, "monumentIds: " + monumentIds);

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void registerNewUser(String user_id){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://smart-tourism.onrender.com/user";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //textView.setText("Response is: " + response);
                        requestUserId();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        textView.setText("registerNewUser: JSON POST Request didn't work!");
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();

                params.put("personal_id",user_id);
                params.put("firstname","FirstName");
                params.put("lastname","LastName");

                return params;
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

    }

    private void registerNewMonument(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://smart-tourism.onrender.com/monument";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //textView.setText("Response is: " + response);
                        requestMonumentId();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        textView.setText("registerNewMonument: JSON POST Request didn't work!");
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();

                params.put("name",monumentId);
                params.put("description",monumentDescription);
                params.put("category","category");
                params.put("image","image");

                return params;
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

    }

    private boolean makeInteraction(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://smart-tourism.onrender.com/interaction";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //textView.setText("Response is: " + response);
                        getHintsFromServer();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        textView.setText("makeInteraction: JSON POST Request didn't work!");
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();

                params.put("user_id",user_id_db);
                params.put("monument_id",monument_id_db);

                return params;
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

        return false;
    }

    private void getHintsFromServer(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://smart-tourism.onrender.com/getRecommendation/"+user_id_db;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        recommendationsReceived = jsonGetRecommendation(response);
                        if (recommendationsReceived)
                            textView.setText("Using recommendations from the server API:" + hints);
                        else
                            textView.setText("Hints list is empty. Using local recommendations.");

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("getHintsFromServer: JSON GET Request didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    */
    /*
    private Boolean readCoordinates(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String lineCoordinates;
        StringBuilder text = new StringBuilder();
        String line;
        try {
            lineCoordinates = reader.readLine();

            while((line = reader.readLine()) != null){
                text.append(line);
                text.append('\n');
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        monumentDescription = text.toString();
        if (monumentDescription.length() != 0)
            Log.d(TAG, "Monument description found.");
        else
            Log.d(TAG, "Monument description NOT found.");


        String patternString = "<!--\\s*([-+]?\\d*\\.\\d+)\\s+([-+]?\\d*\\.\\d+)\\s*-->";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(lineCoordinates);

        if (matcher.find()) {
            String number1 = matcher.group(1);
            String number2 = matcher.group(2);
            //Log.d(TAG, "Number 1: " + number1);
            //Log.d(TAG, "Number 2: " + number2);
            assert number1 != null;
            xCord = Double.valueOf(number1);
            assert number2 != null;
            yCord = Double.valueOf(number2);
            return true;
        }

        return false;

    }
    */
    private ArrayList<Element> getHints(String monumendId) { //hints just calculating the distances
        float[] recognizedVec = new float[0];
        ArrayList<Element> listDocToVec = DatabaseAccess.getListMonuments();

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

    private boolean internetIsConnected() {
        try {
            String command = "ping -c 1 google.com";
            return (Runtime.getRuntime().exec(command).waitFor() == 0);
        } catch (Exception e) {
            return false;
        }
    }
}