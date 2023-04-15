package org.tensorflow.lite.examples.classification.tflite;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;


public class Retrievor {

    public static final String TAG = "Retrievor";
    private static final double MAX_DISTANCE = 1000000000;
    private static final int K = 5; //Divisor to upload database

    public Retrievor(Activity activity, Classifier.Model model) {
        String dbName = "";


        if (model == Classifier.Model.PRECISE) {
            dbName = "MobileNetV3_Large_100_db.sqlite";
        } else if (model == Classifier.Model.MEDIUM) {
            dbName = "MobileNetV3_Large_075_db.sqlite";
        } else if (model == Classifier.Model.FAST) {
            dbName = "MobileNetV3_Small_100_db.sqlite";
        } else {
            throw new UnsupportedOperationException();
        }

        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(activity, dbName);
        databaseAccess.open();
        databaseAccess.updateDatabase(K);
        databaseAccess.close();

        System.loadLibrary("faiss");
    }

    public static native String stringFromJNI(float[] imgFeatures, float[][] data, int k);

    /**
     * Min distance without Faiss
     */
    public ArrayList<Element> getNearestByDistance(float[] imgFeatures, int k) {
        ArrayList<Element> list = new ArrayList<Element>();

        for (Element element : DatabaseAccess.getListDB()) {
            double distance = euclideanDistance(imgFeatures, element.getMatrix());

            if (distance < MAX_DISTANCE) {
                Element e = new Element(element.getMonument(), element.getMatrix(), distance);
                list.add(e);
            }
        }
        return list;

    }

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

    private double euclideanDistance(float[] x, float[] y) {
        return Math.sqrt(dot(x, x) - 2 * dot(x, y) + dot(y, y));
    }

    private double dot(float[] xlist, float[] ylist) {
        double result = 0.0;
        int size = Math.min(xlist.length, ylist.length);

        for (int i = 0; i < size; i++)
            result += xlist[i] * ylist[i];

        return result;
    }
}


