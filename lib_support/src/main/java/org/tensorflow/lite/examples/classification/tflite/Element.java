package org.tensorflow.lite.examples.classification.tflite;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class Element {
    private final String monument;
    private double distance;
    private final float[] matrix;

    private double coordX;
    private double coordY;
    private ArrayList<String> categories = new ArrayList<>();
    private ArrayList<String> attributes = new ArrayList<>();



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

    public void setCategories(ArrayList<String> categories){
        this.categories=categories;
    }

    public void setAttributes(ArrayList<String> attributes){
        this.attributes=attributes;
    }

    public void setCoordinates(double coordX, double coordY){
        this.coordX=coordX;
        this.coordY=coordY;
    }

    public double getCoordX(){
        return coordX;
    }
    public double getCoordY(){
        return coordY;
    }

    @NonNull
    @Override
    public String toString() {
        return "Element{" +
                "monument='" + monument + '\'' +
                ", distance=" + distance + '\'' +
                ", coordX=" + coordX + '\'' +
                ", coordY=" + coordY + '\'' +
                ", categories=" + categories + '\'' +
                ", attributes=" + attributes + '\'' +
                '}';
    }

    public double[] getCoordinates() {
        double[] coordinates = new double[2];
        coordinates[0] = coordX;
        coordinates[1] = coordY;

        return coordinates;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public ArrayList<String> getAttributes() {
        return attributes;
    }
}
