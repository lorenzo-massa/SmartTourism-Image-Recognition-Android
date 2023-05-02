package org.tensorflow.lite.examples.classification.tflite;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;

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

    private Mat descriptors;
    private Mat descriptorsExample;
    private MatOfKeyPoint keyPointsExample;
    private Mat input_rgba_example;

    private final String TAG = "DetectionHelper";

    public DetectionHelper(Context context, Classifier.Mode mode, Bitmap bitmap, String exampleString) {
        this.context = context;
        this.mode = mode;
        this.bitmap = bitmap;
        this.exampleString = exampleString;


        if (mode == Classifier.Mode.ORB) {
            descriptors = new Mat();
            descriptorsExample = new Mat();
            try {
                createDescriptorsExample(exampleString);
            } catch (Exception e) {

            }
        }

    }

    public float help() {
        if (mode == Classifier.Mode.ORB) {
            return orb();
        } else if (mode == Classifier.Mode.OBJ) {
            //obj();
            return 0; //TODO
        }

        return 0;
    }

    private void createDescriptorsExample(String exampleString) throws IOException {

        Mat input_rgba = new Mat();
        //Mat input_gray = new Mat();
        AssetManager assetManager = context.getAssets();
        InputStream istr = assetManager.open("guides/" + exampleString + "/img.jpg");
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        Utils.bitmapToMat(bitmap, input_rgba);

        //Imgproc.cvtColor(input_rgba, input_gray, Imgproc.COLOR_RGB2GRAY);
        //img1.convertTo(img1, 0); //converting the image to match with the type of the cameras image

        keyPointsExample = new MatOfKeyPoint();
        ORB ORBdetector = ORB.create();
        ORBdetector.detect(input_rgba, keyPointsExample);

        ORBdetector.compute(input_rgba, keyPointsExample, descriptorsExample);

        //Features2d.drawKeypoints(input_rgba, keyPointsExample, input_rgba);

        Utils.matToBitmap(input_rgba, bitmap);
        //imageExampleView.setImageBitmap(bitmap);

        input_rgba_example = input_rgba;

    }

    public float orb() {

        Mat input_rgba = new Mat();
        Utils.bitmapToMat(bitmap, input_rgba);

        //Keypoint Detection for Object image
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        ORB ORBdetector = ORB.create();
        ORBdetector.detect(input_rgba, keyPoints);

        //Calculate the descriptors/feature vectors
        ORBdetector.compute(input_rgba, keyPoints, descriptors);
        //Features2d.drawKeypoints(input_rgba, keyPoints, input_rgba);

        //Matching the descriptors using Brute-Force
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
        matcher.knnMatch(descriptors, descriptorsExample, matches, 2);

        // ratio test
        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (Iterator<MatOfDMatch> iterator = matches.iterator(); iterator.hasNext(); ) {
            MatOfDMatch matOfDMatch = (MatOfDMatch) iterator.next();
            if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 0.9) {
                good_matches.add(matOfDMatch.toArray()[0]);
            }
        }

        if (good_matches.size() <= 10) { //it was 6
            //Log.i(TAG,"Wrong Detection");
        } else {
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

            float good_ratio = (float) better_matches.size() / matches.size();

            return good_ratio;

        }

        return 0;
    }

    private void OBJ() {

    }
}
