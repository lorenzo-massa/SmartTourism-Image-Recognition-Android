package org.tensorflow.lite.examples.classification.tflite;


import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;


public class Retrievor {

    public static final String TAG = "Retrievor";
    private static final int K = 5; //Divisor to upload database
    private static boolean firstRun = true;

    public Retrievor(Activity activity, Classifier.Model model, Classifier.Language lang) {
        String dbName = "";

        switch (model) {
            case PRECISE:
                dbName = "MobileNetV3_Large_100_db.sqlite";
                break;
            case MEDIUM:
                dbName = "MobileNetV3_Large_075_db.sqlite";
                break;
            case FAST:
                dbName = "MobileNetV3_Small_100_db.sqlite";
                break;
            default:
                throw new UnsupportedOperationException();
        }

        if (firstRun) {

            firstRun = false;
        }else{
            //TODO: Find a method to do it in background, once is finished, the app can be used
            DatabaseAccess databaseAccess = DatabaseAccess.getInstance(activity, dbName);
            databaseAccess.open();
            databaseAccess.updateDatabaseColdStart(lang.toString());
            databaseAccess.close();
            Log.d(TAG, "[RETRIEVOR] Database updated");
        }

        System.loadLibrary("faiss");
    }

    public static native String stringFromJNI(float[] imgFeatures, float[][] data, int k);

    public ArrayList<Element> faissSearch(float[] imgFeatures, int k) {
        ArrayList<Element> resultList = new ArrayList<Element>();
        String result = stringFromJNI(imgFeatures, DatabaseAccess.getMatrixDB(), k);

        String[] splitted = result.split("\\s+");

        ArrayList<Element> DbList = DatabaseAccess.getListDB();

        for (int z = 0; z < k * 2; z = z + 2) {
            int index = Integer.parseInt(splitted[z]);
            double squaredDistance = Double.parseDouble(splitted[z + 1]);

            if (index != -1) {
                Element oldElement = DbList.get(index);
                Element e = new Element(oldElement.getMonument(), oldElement.getMatrix(), squaredDistance);

                resultList.add(e);
            }
        }

        return resultList;
    }

}


