package org.tensorflow.lite.examples.classification.tflite;


import android.app.Activity;

import java.util.ArrayList;


public class Retrievor {

    public static final String TAG = "Retrievor";

    public Retrievor(Activity activity, Classifier.Model model, String lang) {
        System.loadLibrary("faiss");
    }

    public static native String stringFromJNI(float[] imgFeatures, float[][] data, int k);

    public ArrayList<Element> faissSearch(float[] imgFeatures, int k) {
        ArrayList<Element> resultList = new ArrayList<Element>();
        String result = stringFromJNI(imgFeatures, DatabaseAccess.getMatrixDB(), k);

        String[] splitted = result.split("\\s+");

        ArrayList<Element> DbList = DatabaseAccess.getListRetrieval();

        // XXX each element has two splitted components (monument ID and distance). We have k nearest monuments
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


