package org.tensorflow.lite.examples.classification.tflite;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DatabaseAccess {

    private static final String TAG = "DatabaseAccess";
    private static DatabaseAccess instance;
    private static ArrayList<Element> listRetrieval = new ArrayList<>();
    private static ArrayList<Element> listMonuments = new ArrayList<>();

    private static ArrayList<String> listCategories = new ArrayList<>();

    private static ArrayList<String> listAttributes = new ArrayList<>();

    static HashMap<String, Integer> monumentInteractions = new HashMap<>();


    private final SQLiteOpenHelper openHelperRetrieval;
    private final SQLiteOpenHelper openHelperMonuments;
    private final SQLiteOpenHelper openHelperCategoriesAndAttributes;
    private final SQLiteOpenHelper openHelperLogs;

    private SQLiteDatabase databaseRetrieval;
    private SQLiteDatabase databaseMonuments;
    private SQLiteDatabase databaseCategoriesAndAttributes;
    private SQLiteDatabase databaseLogs;


    public static Thread thread;

    private String dbName;
    private final String dbName2 = "monuments_db.sqlite";
    private final String dbName3 = "list_of_attributes_categories_db.sqlite";
    private final String dbName4 = "logging_db.sqlite";

    private AsyncTask<Void, Integer, Void> databaseLoadingTask;

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    private static SharedPreferences sharedPreferences;

    private DatabaseUpdateListener updateListener;

    private DatabaseAccess(Activity activity, String dbName) {
        this.openHelperRetrieval = new DatabaseOpenHelper(activity, dbName);
        this.openHelperMonuments = new DatabaseOpenHelper(activity, dbName2);
        this.openHelperCategoriesAndAttributes = new DatabaseOpenHelper(activity, dbName3);
        this.openHelperLogs = new DatabaseOpenHelper(activity, dbName4);
    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param activity the Activity
     * @return the instance of DabaseAccess
     */
    public static DatabaseAccess getInstance(Activity activity, String dbName) {
        if (dbName.equals("")){
            dbName = "MobileNetV3_Large_100_db.sqlite";
        }

        if (instance == null){
            instance = new DatabaseAccess(activity, dbName);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        }

        return instance;
    }

    public static List<String> getListCategoriesOrdered() {
        List<String> listCategoriesSelected = new ArrayList<>();

        for (String category : listCategories){
            boolean b = sharedPreferences.getBoolean("category_checkbox_" + category.toLowerCase(), false);
            Log.d(TAG, "getListCategoriesOrdered: category_checkbox: " + category + " b: " + b);
            if (b){
                listCategoriesSelected.add(category);
            }
        }


        HashMap<String, Integer> mapCategories = new HashMap<>();
        for (String category : listCategoriesSelected){
            mapCategories.put(category, 0);
            //caculate the sum of interactions for each category
            for (Map.Entry<String, Integer> entry : monumentInteractions.entrySet()){
                //if the monument has the category
                for (Element e : listMonuments){
                    if (e.getMonument().equals(entry.getKey())){
                        if (e.getCategories().contains(category)){
                            mapCategories.put(category, mapCategories.get(category) + entry.getValue());
                        }
                    }
                }
            }
        }



        //sort the map by value
        List<Map.Entry<String, Integer>> list = new ArrayList<>(mapCategories.entrySet());
        Collections.sort(list, (o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        List<String> listCategoriesOrdered = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : list){
            listCategoriesOrdered.add(entry.getKey());
        }

        //add the categories that are not selected
        for (String category : listCategories){
            if (!listCategoriesOrdered.contains(category)){
                listCategoriesOrdered.add(category);
            }
        }

        return listCategoriesOrdered;
    }

    public static List<String> getMonumentsByCategoryOrdered(String category) {
        ArrayList<String> selectedAttributes = new ArrayList<>();
        ArrayList<String> monumentList = new ArrayList<>();

        for (String attribute : listAttributes){
            boolean b = sharedPreferences.getBoolean("attribute_checkbox_" + attribute.toLowerCase(), false);
            if (b){
                selectedAttributes.add(attribute);
            }
            Log.d(TAG, "getMonumentsByCategoryOrdered: attribute_checkbox: " + attribute + " b: " + b);
        }

        if (!selectedAttributes.isEmpty()){

            //create a list of monuments that have all the selected attributes and belong to the selected category
            for (Element e : listMonuments) {
                if (e.getCategories().contains(category)) {
                    //add only the monuments that have one of the the selected attributes
                    for (String attribute : selectedAttributes) {
                        if (e.getAttributes().contains(attribute)) {
                            monumentList.add(e.getMonument());
                            break;
                        }
                    }
                }
            }
        }

        for (Element e : listMonuments) {
            if (e.getCategories().contains(category)) {
                if (!monumentList.contains(e.getMonument()))
                    monumentList.add(e.getMonument());
            }
        }

        Log.d(TAG, "getMonumentsByCategoryOrdered: list: " + monumentList);

        return monumentList;
    }

    public void setDatabaseUpdateListener(DatabaseUpdateListener listener) {
        this.updateListener = listener;
    }

    public static DatabaseAccess getInstance() {
        if (instance == null){
            Log.d(TAG, "[ERROR] DatabaseAccess instance is null");
        }
        return instance;
    }

    public static ArrayList<String> getListCategories() {
        return listCategories;
    }

    public static ArrayList<String> getListAttributes() {
        return listAttributes;
    }

    public static ArrayList<Element> getListRetrieval() {
        return listRetrieval;
    }

    public static ArrayList<Element> getListMonuments() {
        return listMonuments;
    }

    public static float[][] getMatrixDB() {
        int n = listRetrieval.size();
        float[][] a = new float[n][];
        for (int i = 0; i < n; i++) {
            a[i] = listRetrieval.get(i).getMatrix();
        }

        return a;
    }


    /**
     * Open the database connection.
     */
    public void open() {
        this.databaseRetrieval = openHelperRetrieval.getWritableDatabase();
        this.databaseMonuments = openHelperMonuments.getWritableDatabase();
    }


    /**
     * Close the database connection.
     */

    public void updateDatabaseColdStart(String lang){

        this.databaseCategoriesAndAttributes = openHelperCategoriesAndAttributes.getWritableDatabase();

        if (listCategories.isEmpty()) {
            Log.d(TAG, "[INFO] updateDatabaseColdStart: updating categories...");

            //Update database categories
            Cursor cursorCategories = databaseCategoriesAndAttributes.rawQuery("SELECT name FROM categories_"+ lang, null);
            cursorCategories.moveToFirst();
            while (!cursorCategories.isAfterLast()) {
                String category = cursorCategories.getString(0);
                listCategories.add(category);
                cursorCategories.moveToNext();
            }
        }

        if (listAttributes.isEmpty()) {
            Log.d(TAG, "[INFO] updateDatabaseColdStart: updating attributes...");

            //Update database attributes
            Cursor cursorAttributes = databaseCategoriesAndAttributes.rawQuery("SELECT name FROM attributes_"+ lang, null);
            cursorAttributes.moveToFirst();
            while (!cursorAttributes.isAfterLast()) {
                String attribute = cursorAttributes.getString(0);
                listAttributes.add(attribute);
                cursorAttributes.moveToNext();
            }
        }

        if (databaseCategoriesAndAttributes != null){
            databaseCategoriesAndAttributes.close();
        }
    }

    public void updateDatabase(int k, String lang) {

        this.databaseRetrieval = openHelperRetrieval.getWritableDatabase();

        Log.d(TAG, "[INFO] updateDatabase: updating features...");

        //run the following code in a separate thread
        thread = new Thread(() -> {
            //open the database
            open();
            //check if the database is open
            databaseRetrieval.isOpen();
            listRetrieval = new ArrayList<>();
            //i<k
            for (int i = 0; i < k; i++) {
                Log.v("DatabaseAccess", "id from " + i + "/" + k + " to " + (i + 1) + "/" + k);
                Cursor cursor = databaseRetrieval.rawQuery("SELECT * FROM monuments " +
                        "WHERE rowid > " + i + " * (SELECT COUNT(*) FROM monuments)/" + k + " AND rowid <= (" + i + "+1) * (SELECT COUNT(*) FROM monuments)/" + k, null);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    String monument = cursor.getString(0);
                    String matrix = cursor.getString(1);

                    //Convert matrix string to Float
                    String[] splitted = matrix.substring(1, matrix.length() - 1).split("\\s+");
                    float[] listMatrix = new float[splitted.length];

                    int z = 0;
                    for (String s : splitted
                    ) {
                        if (!Objects.equals(s, "")) {
                            listMatrix[z] = Float.parseFloat(s);
                            z++;
                        }

                    }

                    //element with converted matrix
                    Element e = new Element(monument, listMatrix, 0);
                    listRetrieval.add(e);
                    cursor.moveToNext();
                }
                cursor.close();

                if (updateListener != null) {
                    updateListener.onDatabaseUpdateProgress((i + 1) * 100 / k);
                }
            }

        });

        thread.start();

        //wait for the thread to finish
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (this.databaseRetrieval != null) {
            this.databaseRetrieval.close();
        }
    }

    public void uploadDatabaseMonuments(String lang) {

        //Update database monuments

        this.databaseMonuments = openHelperMonuments.getWritableDatabase();

        Log.d(TAG, "[INFO] updateDatabase: updating monuments...");

        Cursor cursorMonuments = databaseMonuments.rawQuery("SELECT monument,vec,coordX,coordY,categories,attributes FROM monuments_"+ lang, null);
        cursorMonuments.moveToFirst();
        while (!cursorMonuments.isAfterLast()) {
            String monument = cursorMonuments.getString(0);
            String vec = cursorMonuments.getString(1);
            double coordX = cursorMonuments.getFloat(2);
            double coordY = cursorMonuments.getFloat(3);
            String categories = cursorMonuments.getString(4);
            String attributes = cursorMonuments.getString(5);

            //Create list of categories
            String[] splittedCategories = categories.split(",");
            ArrayList<String> listCategories = new ArrayList<>(Arrays.asList(splittedCategories));

            //Create list of attributes
            String[] splittedAttributes = attributes.split(",");
            ArrayList<String> listAttributes = new ArrayList<>(Arrays.asList(splittedAttributes));


            //Convert vec string to Float
            String[] splittedVec = vec.substring(1, vec.length() - 1).split("\\s+");
            float[] listVec = new float[splittedVec.length];

            int z = 0;
            for (String s : splittedVec
            ) {
                if (!Objects.equals(s, "")) {
                    listVec[z] = Float.parseFloat(s);
                    z++;
                }

            }

            //element with converted matrix
            Element e = new Element(monument, listVec, -1);
            e.setCoordinates(coordX,coordY);
            e.setCategories(listCategories);
            e.setAttributes(listAttributes);

            listMonuments.add(e);

            cursorMonuments.moveToNext();
        }
        cursorMonuments.close();

        if (databaseMonuments != null) {
            this.databaseMonuments.close();
        }

    }

    public void uploadMonumentInteractions(){

        Log.d(TAG, "[INFO] updateMonumentInteractions: updating monument interactions...");

        //count number of interactions for each monument
        databaseLogs.isOpen();
        Cursor cursorMonumentInteractions = databaseLogs.rawQuery("SELECT monument, COUNT(*) FROM logs GROUP BY monument", null);
        cursorMonumentInteractions.moveToFirst();
        while (!cursorMonumentInteractions.isAfterLast()) {
            String monument = cursorMonumentInteractions.getString(0);
            int interactions = cursorMonumentInteractions.getInt(1);

            monumentInteractions.put(monument,interactions);

            cursorMonumentInteractions.moveToNext();
        }
        cursorMonumentInteractions.close();

        //print monument interactions
        for (Map.Entry<String, Integer> entry : monumentInteractions.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            Log.d(TAG, "[INFO] updateMonumentInteractions: monument " + key + " has " + value + " interactions");
        }

    }

    public void log(String id) {

        this.databaseLogs = openHelperLogs.getWritableDatabase();

        // Get the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(System.currentTimeMillis());

        // Create the "logs" table if it doesn't exist
        databaseLogs.execSQL("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, monument TEXT)");

        // Insert a record
        ContentValues values = new ContentValues();
        values.put("date", dateTime);
        values.put("monument", id);
        long newRowId = databaseLogs.insert("logs", null, values);

        Log.d(TAG, "[LOG] log: monument " + id + " logged with id " + newRowId);

        //count number of interactions for each monument
        if (monumentInteractions.containsKey(id)){
            monumentInteractions.put(id,monumentInteractions.get(id)+1);
        }else{
            monumentInteractions.put(id,1);
        }

        if (this.databaseLogs != null){
            this.databaseLogs.close();
        }
    }


    public static double[] getCoordinates(String monument) {

        for (Element e : listMonuments
        ) {
            if (Objects.equals(e.getMonument(), monument)) {
                Log.d(TAG, "Coordinates: " + e.getCoordX() + " " + e.getCoordY());
                return e.getCoordinates();
            }
        }

        return null;
    }


    public static String getNearestMonument(double latitude, double longitude, double maxDistance) {

        Location currentLocation = new Location("");
        currentLocation.setLatitude(latitude);
        currentLocation.setLongitude(longitude);


        Element nearestElement = listMonuments.get(0);
        double minDistance = maxDistance;

        for (Element element : listMonuments) {
            // Calculate the distance between current and target locations
            Location targetLocation = new Location("");
            targetLocation.setLatitude(element.getCoordX());
            targetLocation.setLongitude(element.getCoordY());
            float distance = currentLocation.distanceTo(targetLocation);
            if (distance < minDistance) {
                minDistance = distance;
                nearestElement = element;
            }
        }

        if(minDistance >= maxDistance){
            return null;
        }

        Log.d(TAG, "Latitude: " + latitude + " Longitude: " + longitude);
        Log.d(TAG, "Nearest monument: " + nearestElement.getMonument()
                + " Latitude: " + nearestElement.getCoordX()
                + " Longitude: " + nearestElement.getCoordY()
                + " Distance: " + minDistance);
        return nearestElement.getMonument();
    }

    private static double calculateDistance(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

}