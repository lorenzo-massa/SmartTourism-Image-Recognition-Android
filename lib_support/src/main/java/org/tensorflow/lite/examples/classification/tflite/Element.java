package org.tensorflow.lite.examples.classification.tflite;

import androidx.annotation.NonNull;

public class Element {
    private final String monument;
    private final double distance;
    private final float[] matrix;


    public Element(String monument, float[] matrix, double distance) {
        this.monument = monument;
        this.distance = distance;
        this.matrix = matrix;
    }

    public double getDistance() {
        return distance;
    }

    public String getMonument() {
        return monument;
    }

    public float[] getMatrix() {
        return matrix;
    }

    @NonNull
    @Override
    public String toString() {
        return "Element{" +
                "monument='" + monument + '\'' +
                ", distance=" + distance +
                '}';
    }
}
