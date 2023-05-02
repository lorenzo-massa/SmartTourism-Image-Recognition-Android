package org.tensorflow.lite.examples.classification.tflite;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Objects;

public class DatabaseAccess {
    private static DatabaseAccess instance;
    private static ArrayList<Element> listDB = new ArrayList<>();
    private final SQLiteOpenHelper openHelper;
    private SQLiteDatabase database;


    /**
     * Private constructor to aboid object creation from outside classes.
     *
     * @param activity to get the contex
     */
    private DatabaseAccess(Activity activity, String dbName) {
        this.openHelper = new DatabaseOpenHelper(activity, dbName);

    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param activity the Activity
     * @return the instance of DabaseAccess
     */
    public static DatabaseAccess getInstance(Activity activity, String dbName) {
        //if (instance == null) {
        instance = new DatabaseAccess(activity, dbName);
        //}
        return instance;
    }

    public static ArrayList<Element> getListDB() {
        return listDB;
    }

    public static float[][] getMatrixDB() {
        int n = listDB.size();
        float[][] a = new float[n][];
        for (int i = 0; i < n; i++) {
            a[i] = listDB.get(i).getMatrix();
        }

        return a;
    }

    /**
     * Open the database connection.
     */
    public void open() {
        this.database = openHelper.getWritableDatabase();
    }

    /**
     * Close the database connection.
     */
    public void close() {
        if (database != null) {
            this.database.close();
        }
    }

    public void updateDatabase(int k) {

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

    }


}