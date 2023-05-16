package org.tensorflow.lite.examples.classification.tflite;

import androidx.annotation.NonNull;

public class Element {
    private final String monument;
    private double distance;
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

    public void setDistance(double distance){
        this.distance=distance;
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
