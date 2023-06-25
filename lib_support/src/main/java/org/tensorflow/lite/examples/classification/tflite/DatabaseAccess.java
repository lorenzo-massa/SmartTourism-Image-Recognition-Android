package org.tensorflow.lite.examples.classification.tflite;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class DatabaseAccess {

    private static final String TAG = "DatabaseAccess";
    private static DatabaseAccess instance;
    private static ArrayList<Element> listDB = new ArrayList<>();
    private static ArrayList<Element> listDocToVec = new ArrayList<>();

    private static ArrayList<String> listCategories = new ArrayList<>();

    private static ArrayList<String> listAttributes = new ArrayList<>();


    private final SQLiteOpenHelper openHelper;
    private final SQLiteOpenHelper openHelperDocToVec;
    private SQLiteOpenHelper openHelperColdStart;
    private SQLiteOpenHelper openHelperLoggers;

    private SQLiteDatabase database;
    private SQLiteDatabase databaseDocToVec;
    private SQLiteDatabase databaseColdStart;
    private SQLiteDatabase databaseLoggers;


    public static Thread thread;




    /**
     * Private constructor to aboid object creation from outside classes.
     *
     * @param activity to get the contex
     */
    private DatabaseAccess(Activity activity, String dbName, String dbName2,
                           String dbName3, String dbName4) {
        this.openHelper = new DatabaseOpenHelper(activity, dbName);
        this.openHelperDocToVec = new DatabaseOpenHelper(activity, dbName2);
        this.openHelperColdStart = new DatabaseOpenHelper(activity, dbName3);
        this.openHelperLoggers = new DatabaseOpenHelper(activity, dbName4);
    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param activity the Activity
     * @return the instance of DabaseAccess
     */
    public static DatabaseAccess getInstance(Activity activity, String dbName, String dbName2,
                                             String dbName3, String dbName4) {
        if (instance == null){
            instance = new DatabaseAccess(activity, dbName, dbName2, dbName3, dbName4);
        }

        return instance;
    }

    public static DatabaseAccess getInstance() {
        if (instance == null){
            Log.d(TAG, "[ERROR] during Cold Start getInstance or Logging: instance is null");
        }
        return instance;
    }

    public static ArrayList<String> getListCategories() {
        return listCategories;
    }

    public static ArrayList<String> getListAttributes() {
        return listAttributes;
    }



    public static ArrayList<Element> getListDB() {
        return listDB;
    }
    public static ArrayList<Element> getListDocToVec() {
        return listDocToVec;
    }

    public static float[][] getMatrixDB() {
        int n = listDB.size();
        float[][] a = new float[n][];
        for (int i = 0; i < n; i++) {
            a[i] = listDB.get(i).getMatrix();
        }

        return a;
    }

    public void log(String id) {


        Log.d(TAG, "[INFO] log: logging monument " + id);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(System.currentTimeMillis());

        /*
        databaseLoggers.execSQL("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, monument_id TEXT)");
        databaseLoggers.execSQL("INSERT INTO logs (date, monument) VALUES ('" + dateTime + "', '" + id + "')");
        */

        // Create the "logs" table if it doesn't exist
        databaseLoggers.execSQL("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, monument TEXT)");



        // Insert a record
        ContentValues values = new ContentValues();
        values.put("date", dateTime);
        values.put("monument", id);
        long newRowId = databaseLoggers.insert("logs", null, values);

        Log.d(TAG, "[INFO] log: monument " + id + " logged with id " + newRowId);

      }

    /**
     * Open the database connection.
     */
    public void open() {
        this.database = openHelper.getWritableDatabase();
        this.databaseDocToVec = openHelperDocToVec.getWritableDatabase();
    }

    public void setOpenHelperColdStart() {
        this.databaseColdStart = openHelperColdStart.getWritableDatabase();
    }

    public void closeOpenHelperColdStart() {
        this.databaseColdStart.close();
    }

    public void setOpenHelperLoggers() {
        this.databaseLoggers = openHelperLoggers.getWritableDatabase();
    }

    public void closeOpenHelperLoggers() {
        this.databaseLoggers.close();
    }

    /**
     * Close the database connection.
     */
    public void close() {
        if (database != null) {
            this.database.close();
        }

        if (databaseDocToVec != null) {
            this.databaseDocToVec.close();
        }
    }

    public void updateDatabaseColdStart(String lang){

        setOpenHelperColdStart();

        Log.d(TAG, "[INFO] updateDatabaseColdStart: updating categories...");

        //Update database categories
        Cursor cursorCategories = databaseColdStart.rawQuery("SELECT name FROM categories_"+ lang, null);
        cursorCategories.moveToFirst();
        while (!cursorCategories.isAfterLast()) {
            String category = cursorCategories.getString(0);
            listCategories.add(category);
            cursorCategories.moveToNext();
        }

        Log.d(TAG, "[INFO] updateDatabaseColdStart: updating attributes...");

        //Update database attributes
        Cursor cursorAttributes = databaseColdStart.rawQuery("SELECT name FROM attributes_"+ lang, null);
        cursorAttributes.moveToFirst();
        while (!cursorAttributes.isAfterLast()) {
            String attribute = cursorAttributes.getString(0);
            listAttributes.add(attribute);
            cursorAttributes.moveToNext();
        }

        closeOpenHelperColdStart();
    }

    public void updateDatabase(int k, Classifier.Language lang) {

        Log.d(TAG, "[INFO] updateDatabase: updating features...");

        //run the following code in a separate thread
        thread = new Thread(() -> {
            //open the database
            open();
            //check if the database is open
            database.isOpen();
            listDB = new ArrayList<>();
            //i<k
            for (int i = 0; i < k; i++) {
                Log.v("DatabaseAccess", "id from " + i + "/" + k + " to " + (i + 1) + "/" + k);
                Cursor cursor = database.rawQuery("SELECT * FROM monuments " +
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
                    listDB.add(e);
                    cursor.moveToNext();
                }
                cursor.close();
            }

        });
        thread.start();

        //wait for the thread to finish
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /*

        database.isOpen();
        listDB = new ArrayList<>();
        //i<k
        for (int i = 0; i < k; i++) {
            Log.v("DatabaseAccess", "id from " + i + "/" + k + " to " + (i + 1) + "/" + k);
            Cursor cursor = database.rawQuery("SELECT * FROM monuments " +
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
                Element e = new Element(monument, listMatrix, -1);
                listDB.add(e);

                cursor.moveToNext();
            }
            cursor.close();
        }

         */

        //Update database monuments

        Log.d(TAG, "[INFO] updateDatabase: updating monuments...");


        Cursor cursorMonuments = databaseDocToVec.rawQuery("SELECT monument,vec,coordX,coordY,categories,attributes FROM monuments_"+ lang, null);
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

            listDocToVec.add(e);

            cursorMonuments.moveToNext();
        }
        cursorMonuments.close();

    }


    public String getNearestMonument(double latitude, double longitude) {
        Log.d(TAG, "Latitude: " + latitude + " Longitude: " + longitude);

        Element nearestElement = listDocToVec.get(0);
        double minDistance = calculateDistance(latitude, longitude,
                nearestElement.getCoordX(), nearestElement.getCoordY());

        for (Element element : listDocToVec) {
            double distance = calculateDistance(latitude, longitude, element.getCoordX(), element.getCoordY());
            if (distance < minDistance) {
                minDistance = distance;
                nearestElement = element;
            }
        }

        Log.d(TAG, "[INFO] Latitude: " + latitude + " Longitude: " + longitude);
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