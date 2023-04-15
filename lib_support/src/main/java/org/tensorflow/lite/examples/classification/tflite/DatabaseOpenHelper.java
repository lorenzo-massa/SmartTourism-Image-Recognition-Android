package org.tensorflow.lite.examples.classification.tflite;


import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;


public class DatabaseOpenHelper extends SQLiteAssetHelper {
    //private static final String DATABASE_NAME = "MN3nZoom.sqlite";
    private static final int DATABASE_VERSION = 1;

    public DatabaseOpenHelper(Context context, String dbNAme) {
        super(context, dbNAme, null, DATABASE_VERSION);
    }
}
