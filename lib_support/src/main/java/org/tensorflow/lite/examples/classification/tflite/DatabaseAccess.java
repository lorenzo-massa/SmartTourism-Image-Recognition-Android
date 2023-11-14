package org.tensorflow.lite.examples.classification.tflite;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DatabaseAccess {

    private static final String TAG = "DatabaseAccess";
    private static DatabaseAccess instance;
    private static ArrayList<Element> listRetrieval = new ArrayList<>();
    private static ArrayList<Element> listMonuments = new ArrayList<>(); //All possible monuments

    private static ArrayList<String> listCategories = new ArrayList<>(); //All possible categories

    private static ArrayList<String> listAttributes = new ArrayList<>(); //All possible attributes

    private static ArrayList<String> listLanguages = new ArrayList<>(); //All possible languages

    private final SQLiteOpenHelper openHelperRetrieval;
    private static SQLiteOpenHelper openHelperMonuments = null;

    private SQLiteDatabase databaseRetrieval;
    private static SQLiteDatabase databaseMonuments;


    public static Thread thread;

    private String dbName;
    private final String dbName2 = "monuments_db.sqlite";

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    private static SharedPreferences sharedPreferences;

    private DatabaseUpdateListener updateListener;

    private static String language;

    private DatabaseAccess(Activity activity, String dbName) {
        this.openHelperRetrieval = new DatabaseOpenHelper(activity, dbName);
        this.openHelperMonuments = new DatabaseOpenHelper(activity, dbName2);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        language = sharedPreferences.getString("pref_key_language", "English");
    }

    public static void setLanguage(String language) {
        DatabaseAccess.language = language;
    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param activity the Activity
     * @return the instance of DabaseAccess
     */
    public static DatabaseAccess getInstance(Activity activity, String dbName) {
        if (dbName.equals("")) {
            dbName = "MobileNetV3_Large_100_db.sqlite";
        }

        if (instance == null) {
            instance = new DatabaseAccess(activity, dbName);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        }

        return instance;
    }

    public List<String> getListCategoriesOrdered() {
        List<String> selectedCategories = new ArrayList<>();
        List<String> listCategoriesOrdered = new ArrayList<>();

        for (String category : listCategories) {
            boolean b = sharedPreferences.getBoolean("category_checkbox_" + category.toLowerCase(), false);
            Log.d(TAG, "getListCategoriesOrdered: category_checkbox: " + category + " b: " + b);
            if (b) {
                selectedCategories.add(category);
            }
        }

        databaseMonuments = openHelperMonuments.getWritableDatabase();

        // Create the "logs" table if it doesn't exist
        databaseMonuments.execSQL("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, monumentId INTEGER, " +
                "FOREIGN KEY (monumentId) REFERENCES monuments_English(id))");

        //create a list with the selected categories ordered by the number of interactions
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT c.name, COUNT(l.id) AS interaction_count ")
                .append("FROM categories_" + language + " c ")
                .append("JOIN monuments_categories_" + language + " mc ON c.id = mc.categoryID ")
                .append("JOIN monuments_" + language + " m ON mc.monumentID = m.id ")
                .append("LEFT JOIN logs l ON m.id = l.monumentId ")
                .append("WHERE c.name IN (");

        // Add placeholders for category names
        for (int i = 0; i < selectedCategories.size(); i++) {
            queryBuilder.append("?");
            if (i < selectedCategories.size() - 1) {
                queryBuilder.append(",");
            }
        }

        queryBuilder.append(") GROUP BY c.name ORDER BY interaction_count DESC");

        String[] selectionArgs = selectedCategories.toArray(new String[0]);

        Cursor cursor = databaseMonuments.rawQuery(queryBuilder.toString(), selectionArgs);

        while (cursor.moveToNext()) {
            String categoryName = cursor.getString(0);
            listCategoriesOrdered.add(categoryName);
        }

        cursor.close();
        databaseMonuments.close();

        //add the categories that are not selected

        for (String category : listCategories) {
            if (!listCategoriesOrdered.contains(category)) {
                listCategoriesOrdered.add(category);
            }
        }

        return listCategoriesOrdered;
    }

    public static List<String> getMonumentsByCategoryOrdered(String category) {
        ArrayList<String> selectedAttributes = new ArrayList<>();
        ArrayList<String> monumentList = new ArrayList<>();

        for (String attribute : listAttributes) {
            boolean b = sharedPreferences.getBoolean("attribute_checkbox_" + attribute.toLowerCase(), false);
            if (b) {
                selectedAttributes.add(attribute);
            }
            Log.d(TAG, "getMonumentsByCategoryOrdered: attribute_checkbox: " + attribute + " b: " + b);
        }

        if (!selectedAttributes.isEmpty()) {
            databaseMonuments = openHelperMonuments.getWritableDatabase();

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT DISTINCT m.id, m.monument ")
                    .append("FROM monuments_" + language + " m ")
                    .append("JOIN monuments_attributes_" + language + " ma ON m.id = ma.monumentID ")
                    .append("JOIN attributes_" + language + " a ON ma.attributeID = a.id ")
                    .append("JOIN monuments_categories_" + language + " mc ON m.id = mc.monumentID ")
                    .append("JOIN categories_" + language + " c ON mc.categoryID = c.id ")
                    .append("WHERE a.name IN (");

            // Add placeholders for attribute names
            for (int i = 0; i < selectedAttributes.size(); i++) {
                queryBuilder.append("?");
                if (i < selectedAttributes.size() - 1) {
                    queryBuilder.append(",");
                }
            }

            queryBuilder.append(") AND c.name = ?");

            List<String> selectionArgsList = new ArrayList<>(selectedAttributes);
            selectionArgsList.add(category);

            String[] selectionArgs = selectionArgsList.toArray(new String[0]);

            Cursor cursor = databaseMonuments.rawQuery(queryBuilder.toString(), selectionArgs);

            while (cursor.moveToNext()) {
                String monumentName = cursor.getString(1);
                monumentList.add(monumentName);
            }

            cursor.close();
            databaseMonuments.close();
        }

        for (String m : getMonumentsByCategory(category)) {
            if (!monumentList.contains(m)) {
                monumentList.add(m);
            }
        }

        Log.d(TAG, "getMonumentsByCategoryOrdered: list: " + monumentList);

        return monumentList;
    } //TODO optimize with query

    public void setDatabaseUpdateListener(DatabaseUpdateListener listener) {
        this.updateListener = listener;
    }

    public static DatabaseAccess getInstance() {
        if (instance == null) {
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

    public static ArrayList<String> getListLanguages() {
        return listLanguages;
    }

    private static List<String> getMonumentsByCategory(String category) {
        List<String> monumentList = new ArrayList<>();
        databaseMonuments = openHelperMonuments.getWritableDatabase();

        String query = "SELECT m.monument " +
                "FROM monuments_" + language + " m " +
                "JOIN monuments_categories_" + language + " mc ON m.id = mc.monumentID " +
                "JOIN categories_" + language + " c ON mc.categoryID = c.id " +
                "WHERE c.name = ?";

        String[] selectionArgs = {category};

        Cursor cursor = databaseMonuments.rawQuery(query, selectionArgs);

        while (cursor.moveToNext()) {
            String monumentName = cursor.getString(0);
            monumentList.add(monumentName);
        }

        cursor.close();
        databaseMonuments.close();

        return monumentList;
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
        // check SQLLite DB file really exists in app filesystem



        this.databaseRetrieval = openHelperRetrieval.getWritableDatabase();
        this.databaseMonuments = openHelperMonuments.getWritableDatabase();
    }


    /**
     * Close the database connection.
     */

    public void updateDatabaseColdStart() {

        this.databaseMonuments = openHelperMonuments.getWritableDatabase();

        if (listCategories.isEmpty()) {
            Log.d(TAG, "[INFO] updateDatabaseColdStart: updating categories...");

            //Update database categories
            Cursor cursorCategories = databaseMonuments.rawQuery("SELECT name FROM categories_" + language, null);
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
            Cursor cursorAttributes = databaseMonuments.rawQuery("SELECT name FROM attributes_" + language, null);
            cursorAttributes.moveToFirst();
            while (!cursorAttributes.isAfterLast()) {
                String attribute = cursorAttributes.getString(0);
                listAttributes.add(attribute);
                cursorAttributes.moveToNext();
            }
        }

        if (databaseMonuments != null) {
            databaseMonuments.close();
        }
    }

    public void updateDatabase(int k) {

        this.databaseRetrieval = openHelperRetrieval.getWritableDatabase();

        Log.d(TAG, "[INFO] updateDatabase: updating features...");

        //run the following code in a separate thread
        thread = new Thread(() -> {
            //open the database
            open();
            //check if the database is open
            if (!databaseRetrieval.isOpen())
                Log.e(TAG, "[Error] updateDatabase: databaseRetrieval database is not open");
            listRetrieval = new ArrayList<>();
            //i<k
            for (int i = 0; i < k; i++) {
                Log.v("DatabaseAccess", "id from " + i + "/" + k + " to " + (i + 1) + "/" + k);
                String SQLQuery = "SELECT * FROM monuments WHERE rowid > " + i + " * (SELECT COUNT(*) FROM monuments)/" + k + " AND rowid <= (" + i + "+1) * (SELECT COUNT(*) FROM monuments)/" + k;
                Cursor cursor = databaseRetrieval.rawQuery(SQLQuery, null);
                if (cursor.moveToFirst()) {
                    do {
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
                    } while (!cursor.isAfterLast());
                } else {
                    Log.e(TAG, "[Error] updateDatabase: databaseRetrieval cursor is empty");
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

    public void uploadDatabaseMonuments() {

        //Update database monuments

        this.databaseMonuments = openHelperMonuments.getWritableDatabase();

        Log.d(TAG, "[INFO] updateDatabase: updating monuments...");

        Cursor cursorMonuments = databaseMonuments.rawQuery("SELECT monument,vec,coordX,coordY,path FROM monuments_" + language, null);
        cursorMonuments.moveToFirst();
        while (!cursorMonuments.isAfterLast()) {
            String monument = cursorMonuments.getString(0);
            String vec = cursorMonuments.getString(1);
            double coordX = cursorMonuments.getFloat(2);
            double coordY = cursorMonuments.getFloat(3);
            String path = cursorMonuments.getString(4);

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
            e.setCoordinates(coordX, coordY);
            e.setPath(path);

            listMonuments.add(e);

            cursorMonuments.moveToNext();
        }
        cursorMonuments.close();

        if (databaseMonuments != null) {
            this.databaseMonuments.close();
        }
    }

    public void uploadLanguages() {
        databaseMonuments = openHelperMonuments.getWritableDatabase();

        Log.d(TAG, "[INFO] updateDatabase: updating languages...");

        Cursor cursorLanguages = databaseMonuments.rawQuery("SELECT language FROM languages", null);
        cursorLanguages.moveToFirst();
        while (!cursorLanguages.isAfterLast()) {
            String language = cursorLanguages.getString(0);
            listLanguages.add(language);
            cursorLanguages.moveToNext();
        }
        cursorLanguages.close();

        if (databaseMonuments != null) {
            databaseMonuments.close();
        }
    }

    private int getMonumentId(String monumentName) {
        this.databaseMonuments = openHelperMonuments.getWritableDatabase();

        String query = "SELECT id FROM monuments_English WHERE monument = ?";
        String[] selectionArgs = {monumentName};

        Cursor cursor = databaseMonuments.rawQuery(query, selectionArgs);

        int monumentId = -1; // Default value if monument ID is not found

        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex("id");
            if (columnIndex != -1) {
                monumentId = cursor.getInt(columnIndex);
            }
        }

        cursor.close();
        this.databaseMonuments.close();

        return monumentId;
    }

    public void log(String id) {

        int idMonument = getMonumentId(id);

        if (idMonument == -1) {
            Log.d(TAG, "[ERROR] log: monument " + id + " not found");
            return;
        }

        this.databaseMonuments = openHelperMonuments.getWritableDatabase();

        // Get the current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(System.currentTimeMillis());

        // Create the "logs" table if it doesn't exist
        //databaseMonuments.execSQL("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, monumentId INTEGER, " +
        //        "FOREIGN KEY (monumentId) REFERENCES monuments_English(id))");

        // Insert a record
        ContentValues values = new ContentValues();
        values.put("date", dateTime);
        values.put("monumentId", idMonument);
        long newRowId = databaseMonuments.insert("logs", null, values);

        Log.d(TAG, "[LOG] log: monument " + id + " logged with id " + newRowId);

        if (this.databaseMonuments != null) {
            this.databaseMonuments.close();
        }
    }


    public static double[] getCoordinates(String monument) {

        for (Element e : listMonuments) {
            if (Objects.equals(e.getMonument(), monument)) {
                Log.d(TAG, "Coordinates: " + e.getCoordX() + " " + e.getCoordY());
                return e.getCoordinates();
            }
        }

        return null;
    }

    public static String getImageLink(String monument) {
        for (Element e : listMonuments) {
            if (Objects.equals(e.getMonument(), monument)) {
                Log.d(TAG, "Image link: " + e.getPath());
                return e.getPath();
            }
        }

        return null;
    }

    public static float distance2Positions(double[] m1, double[] m2) {
        Location location1 = new Location("");
        location1.setLatitude(m1[0]);
        location1.setLongitude(m1[1]);

        Location location2 = new Location("");
        location2.setLatitude(m2[0]);
        location2.setLongitude(m2[1]);

        return location1.distanceTo(location2);
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
            Log.d(TAG, "Distance to " + element.getMonument() + ": " + distance + " meters");
            if (distance < minDistance) {
                minDistance = distance;
                nearestElement = element;
            }
        }

        if (minDistance >= maxDistance) {
            Log.d(TAG, "No monument found within " + maxDistance + " meters");
            Log.d(TAG, "Nearest monument: " + nearestElement.getMonument() + " at " + minDistance + " meters");
            return null;
        }

        Log.d(TAG, "Nearest monument: " + nearestElement.getMonument() + " at " + minDistance + " meters");

        return nearestElement.getMonument();
    }

    public static String getRandomMonumentFromPreferredCategories(){
        List<String> selectedCategories = new ArrayList<>();

        for (String category : listCategories) {
            boolean b = sharedPreferences.getBoolean("category_checkbox_" + category.toLowerCase(), false);
            if (b) {
                selectedCategories.add(category);
            }
        }

        if (selectedCategories.isEmpty()) {
            return getRandomMonument();
        }

        List<String> monuments = getMonumentsByCategoryOrdered(selectedCategories.get(0));
        Collections.shuffle(monuments);
        return monuments.get(0);
    }

    public static String getRandomMonument(){
        Collections.shuffle(listMonuments);
        return listMonuments.get(0).getMonument();
    }

    public static double calculateDistance(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

}