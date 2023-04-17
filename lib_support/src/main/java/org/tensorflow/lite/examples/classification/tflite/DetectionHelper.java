package org.tensorflow.lite.examples.classification.tflite;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DetectionHelper {
    private Classifier.Mode mode;
    private Context context;
    private Bitmap bitmap;
    private String exampleString;

    private Bitmap bitmapExample;

    public DetectionHelper(Context context, Classifier.Mode mode, Bitmap bitmap, String exampleString) {
        this.context = context;
        this.mode = mode;
        this.bitmap = bitmap;
        this.exampleString = exampleString;

        loadImage(exampleString);
    }

    private void loadImage(String fileName) {
        AssetManager am = context.getAssets();
        InputStream is = null;
        try {
            is = am.open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        bitmapExample = BitmapFactory.decodeStream(is);
    }

    public void help(){
        if(mode == Classifier.Mode.ORB){
            //orb();
        }else if(mode == Classifier.Mode.OBJ){
            //obj();
        }
    }

    /*
    public void orb() {

        Mat input_rgba = new Mat();
        Utils.bitmapToMat(bitmap, input_rgba);


        //Keypoint Detection for Object image
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        ORB ORBdetector = ORB.create();
        ORBdetector.detect(input_rgba, keyPoints);

        //Calculate the descriptors/feature vectors
        ORBdetector.compute(input_rgba, keyPoints, descriptors);
        Features2d.drawKeypoints(input_rgba, keyPoints, input_rgba);

        //Matching the descriptors using Brute-Force
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
        matcher.knnMatch(descriptors, descriptorsExample, matches, 2);

        // ratio test
        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (Iterator<MatOfDMatch> iterator = matches.iterator(); iterator.hasNext();) {
            MatOfDMatch matOfDMatch = (MatOfDMatch) iterator.next();
            if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 0.9) {
                good_matches.add(matOfDMatch.toArray()[0]);
            }
        }

        if(good_matches.size() <= 10){ //it was 6
            //Log.i(TAG,"Wrong Detection");
        }
        else {
            //Log.i(TAG,"Good Detection");

            // get keypoint coordinates of good matches to find homography and remove outliers using ransac
            List<Point> pts1 = new ArrayList<Point>();
            List<Point> pts2 = new ArrayList<Point>();
            for (int i = 0; i < good_matches.size(); i++) {
                pts1.add(keyPoints.toList().get(good_matches.get(i).queryIdx).pt);
                pts2.add(keyPointsExample.toList().get(good_matches.get(i).trainIdx).pt);
            }

            // convertion of data types - there is maybe a more beautiful way
            Mat outputMask = new Mat();
            MatOfPoint2f pts1Mat = new MatOfPoint2f();
            pts1Mat.fromList(pts1);
            MatOfPoint2f pts2Mat = new MatOfPoint2f();
            pts2Mat.fromList(pts2);

            // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
            // the smaller the allowed reprojection error (here 15), the more matches are filtered
            Mat Homog = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 15, outputMask, 2000, 0.995);

            // outputMask contains zeros and ones indicating which matches are filtered
            LinkedList<DMatch> better_matches = new LinkedList<DMatch>();
            for (int i = 0; i < good_matches.size(); i++) {
                if (outputMask.get(i, 0)[0] != 0.0) {
                    better_matches.add(good_matches.get(i));
                }
            }

            // DRAWING OUTPUT
            Mat outputImg = new Mat();
            // this will draw all matches
            MatOfDMatch better_matches_mat = new MatOfDMatch();
            better_matches_mat.fromList(better_matches);
            Features2d.drawMatches(input_rgba, keyPoints, input_rgba_example, keyPointsExample, better_matches_mat, outputImg);

            // save image
            //Imgcodecs.imwrite("C:/result.jpg", outputImg);

            Imgproc.resize(outputImg, input_rgba, input_rgba.size(), 0, 0, INTER_AREA);

            float good_ratio = (float)better_matches.size()/matches.size();
            //#good matches/#all matches in order to get a ratio
            //Log.i(TAG, "good/all matches ratio: " +good_ratio);


            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    // Stuff that updates the UI

                    ratioView.setText("ratio: " +good_ratio);
                    ratioView.setTextColor(Color.GREEN);

                }
            });




        }
        return input_rgba;

    }


     */

    private void  OBJ(){

    }
}
