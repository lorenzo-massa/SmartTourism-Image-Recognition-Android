/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.examples.classification.tflite;

import static java.lang.Math.min;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * A classifier specialized to label images using TensorFlow Lite.
 */
public abstract class Classifier {
    public static final String TAG = "ClassifierWithSupport";
    private static final int K_TOP_RESULT = 4;
    /**
     * Number of results to show in the UI.
     */
    private static final int MAX_RESULTS = 3;
    private static Retrievor retrievor;
    /**
     * Image size along the x axis.
     */
    private final int imageSizeX;
    /**
     * Image size along the y axis.
     */
    private final int imageSizeY;

    /** The loaded TensorFlow Lite model. */
    /**
     * Options for configuring the Interpreter.
     */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    private final TensorBuffer outputProbabilityBufferZoom1;
    private final TensorBuffer outputProbabilityBufferZoom2;
    /**
     * Output probability TensorBuffer.
     */
    private final TensorBuffer outputProbabilityBuffer;
    /**
     * Processer to apply post processing of the output probability.
     */
    private final TensorProcessor probabilityProcessor;
    /**
     * An instance of the driver class to run model inference with Tensorflow Lite.
     */
    protected Interpreter tflite;

    /** Labels corresponding to the output of the vision model. */
    //private final List<String> labels;
    /**
     * Optional GPU delegate for accleration.
     */
    private GpuDelegate gpuDelegate = null;
    /**
     * Optional NNAPI delegate for accleration.
     */
    private NnApiDelegate nnApiDelegate = null;
    /**
     * Input image TensorBuffer.
     */
    private TensorImage inputImageBuffer;
    /**
     * TensorBuffer for augumentation
     */
    private TensorImage inputImageBufferZoom1;
    private TensorImage inputImageBufferZoom2;
    private final Context context;

    private Mode mode;

    /**
     * Initializes a {@code Classifier}.
     */
    protected Classifier(Activity activity, Device device, int numThreads, Mode mode) throws IOException {

        this.context = activity.getApplicationContext();

        this.mode = mode;

        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(activity, getModelPath());
        switch (device) {
            case NNAPI:
                nnApiDelegate = new NnApiDelegate();
                tfliteOptions.addDelegate(nnApiDelegate);
                break;
            case GPU:
                CompatibilityList compatList = new CompatibilityList();
                if (compatList.isDelegateSupportedOnThisDevice()) {
                    // if the device has a supported GPU, add the GPU delegate
                    GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
                    GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
                    tfliteOptions.addDelegate(gpuDelegate);
                    Log.d(TAG, "GPU supported. GPU delegate created and added to options");
                } else {
                    tfliteOptions.setUseXNNPACK(true);
                    Log.d(TAG, "GPU not supported. Default to CPU.");
                    Toast.makeText(context, "GPU not supported. Default to CPU.", Toast.LENGTH_SHORT).show();
                }
                break;
            case CPU:
                tfliteOptions.setUseXNNPACK(true);
                Log.d(TAG, "CPU execution");
                break;
        }
        tfliteOptions.setNumThreads(numThreads);
        tflite = new Interpreter(tfliteModel, tfliteOptions);

        // Loads labels out from the label file.
        //labels = FileUtil.loadLabels(activity, getLabelPath());

        // Reads type and shape of input and output tensors, respectively.
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];

        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        // Creates the input tensor.
        inputImageBuffer = new TensorImage(imageDataType);
        inputImageBufferZoom1 = new TensorImage(imageDataType);
        inputImageBufferZoom2 = new TensorImage(imageDataType);


        // Creates the output tensor and its processor.
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        outputProbabilityBufferZoom1 = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        outputProbabilityBufferZoom2 = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);


        // Creates the post processor for the output probability.
        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

        Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
    }

    /**
     * Creates a classifier with the provided configuration.
     *
     * @param activity   The current Activity.
     * @param model      The model to use for classification.
     * @param device     The device to use for classification.
     * @param numThreads The number of threads to use for classification.
     * @return A classifier with the desired configuration.
     */
    public static Classifier create(Activity activity, Model model, Device device, int numThreads, Mode mode, String lang)
            throws IOException {

        retrievor = new Retrievor(activity, model, lang);

        if (model == Model.Precise) {
            return new ClassifierMobileNetLarge100(activity, device, numThreads, mode);
        } else if (model == Model.Medium) {
            return new ClassifierMobileNetLarge075(activity, device, numThreads, mode);
        } else if (model == Model.Fast) {
            return new ClassifierMobileNetSmall100(activity, device, numThreads, mode);
        } else {
            throw new UnsupportedOperationException();
        }


    }

    private static Map<String, Double> createMap(List<Element> results, List<Element> resultsZoom1, List<Element> resultsZoom2) {

        Map<String, Double> labeledProbability = new TreeMap<String, Double>();

        // TODO better filtering based on distance

        //Result
        int size = min(K_TOP_RESULT, results.size());

        for (int i = 0; i < size; i++) {
            Element e = results.get(i);
            double distance = e.getDistance();

            String newKey = e.getMonument();

            if (labeledProbability.containsKey(newKey)) {
                double value = labeledProbability.get(newKey);
                //double newValue = (value*2+distance)/3; //average with more importance on first positions
                //labeledProbability.put(newKey,newValue);
                labeledProbability.put(newKey, value - 1);
            } else {
                //labeledProbability.put(newKey, distance);
                labeledProbability.put(newKey, K_TOP_RESULT * 3d);
            }
        }

        //Result Zoom 1
        size = min(K_TOP_RESULT, resultsZoom1.size());

        for (int i = 0; i < size; i++) {
            Element e = resultsZoom1.get(i);
            double distance = e.getDistance();

            String newKey = e.getMonument();

            if (labeledProbability.containsKey(newKey)) {
                double value = labeledProbability.get(newKey);
                //double newValue = (value*2+distance)/3; //average with more importance on first positions
                //labeledProbability.put(newKey,newValue);
                labeledProbability.put(newKey, value - 1);
            } else {
                //labeledProbability.put(newKey, distance);
                labeledProbability.put(newKey, K_TOP_RESULT * 3d);
            }
        }

        //Result Zoom 1
        size = min(K_TOP_RESULT, resultsZoom2.size());

        for (int i = 0; i < size; i++) {
            Element e = resultsZoom2.get(i);
            double distance = e.getDistance();

            String newKey = e.getMonument();

            if (labeledProbability.containsKey(newKey)) {
                double value = labeledProbability.get(newKey);
                //double newValue = (value*2+distance)/3; //average with more importance on first positions
                //labeledProbability.put(newKey,newValue);
                labeledProbability.put(newKey, value - 1);
            } else {
                //labeledProbability.put(newKey, distance);
                labeledProbability.put(newKey, K_TOP_RESULT * 3d);
            }
        }

        return labeledProbability;
    }

    /**
     * Gets the top-k results.
     */
    private static List<Recognition> getTopKProbability(Map<String, Double> labelProb) {
        // Find the best classifications.
        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        MAX_RESULTS,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                // Intentionally re-(from me)reversed to put high confidence at the head of the queue.
                                return Double.compare(lhs.getConfidence(), rhs.getConfidence());
                            }
                        });

        for (Map.Entry<String, Double> entry : labelProb.entrySet()) {
            pq.add(new Recognition("" + entry.getKey(), entry.getKey(), entry.getValue(), null));
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

    /**
     * Runs inference and returns the classification results.
     */
    public List<Recognition> recognizeImage(final Bitmap bitmap, int sensorOrientation) {
        // Logs this method so that it can be analyzed with systrace.

        Trace.beginSection("recognizeImage");

        //Load image
        Trace.beginSection("loadImage");
        long startTimeForLoadImage = SystemClock.uptimeMillis();
        loadImage(bitmap, sensorOrientation, 1f, 0.5f, 0.3f); //Load image (1 + 2 for augumentation)
        long endTimeForLoadImage = SystemClock.uptimeMillis();
        Trace.endSection();
        Log.v(TAG, "Timecost to load the image: " + (endTimeForLoadImage - startTimeForLoadImage));

        // Runs the inference call.
        Trace.beginSection("runInference");
        long startTimeForReference = SystemClock.uptimeMillis();
        tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
        tflite.run(inputImageBufferZoom1.getBuffer(), outputProbabilityBufferZoom1.getBuffer().rewind()); //augumentation zoom1
        tflite.run(inputImageBufferZoom2.getBuffer(), outputProbabilityBufferZoom2.getBuffer().rewind()); //augumentation zoom2
        long endTimeForReference = SystemClock.uptimeMillis();
        Trace.endSection();
        Log.v(TAG, "Timecost to run model inference: " + (endTimeForReference - startTimeForReference));

        //Get features
        /*float [] features = outputProbabilityBuffer.getFloatArray();
        float [] featuresZoom1 = outputProbabilityBufferZoom1.getFloatArray();
        float [] featuresZoom2 = outputProbabilityBufferZoom2.getFloatArray();
        */

        //Get features + Postprocess
        float[] features = probabilityProcessor.process(outputProbabilityBuffer).getFloatArray();
        float[] featuresZoom1 = probabilityProcessor.process(outputProbabilityBufferZoom1).getFloatArray();
        float[] featuresZoom2 = probabilityProcessor.process(outputProbabilityBufferZoom2).getFloatArray();

        //Faiss Search
        Trace.beginSection("runFaissSearch");
        long startTimeForFaiss = SystemClock.uptimeMillis();
        ArrayList<Element> result = retrievor.faissSearch(features, K_TOP_RESULT);
        ArrayList<Element> resultZoom1 = retrievor.faissSearch(featuresZoom1, K_TOP_RESULT);
        ArrayList<Element> resultZoom2 = retrievor.faissSearch(featuresZoom2, K_TOP_RESULT);
        long endTimeForFaiss = SystemClock.uptimeMillis();
        Log.v(TAG, "Timecost to run Faiss Search: " + (endTimeForFaiss - startTimeForFaiss));
        Trace.endSection();

        // Gets top-k results.
        Trace.beginSection("runPostProcess");
        long startTimeForPostProcess = SystemClock.uptimeMillis();
        Map<String, Double> labeledDistance = createMap(result, resultZoom1, resultZoom2);
        List<Recognition> finalResult = getTopKProbability(labeledDistance);
        long endTimeForPostProcess = SystemClock.uptimeMillis();
        Log.v(TAG, "Timecost to run Post Process: " + (endTimeForPostProcess - startTimeForPostProcess));
        Trace.endSection();

        Log.v(TAG, "# recognized results: " + finalResult.size());
        Trace.endSection(); //end recognize image section

        return finalResult;
    }

    /**
     * Closes the interpreter and model to release resources.
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (nnApiDelegate != null) {
            nnApiDelegate.close();
            nnApiDelegate = null;
        }
    }

    /**
     * Get the image size along the x axis.
     */
    public int getImageSizeX() {
        return imageSizeX;
    }

    /**
     * Get the image size along the y axis.
     */
    public int getImageSizeY() {
        return imageSizeY;
    }

    /**
     * Loads input image, and applies preprocessing.
     */
    private void loadImage(Bitmap bitmap, int sensorOrientation, float zoomRatio, float zoomRatioZoom1, float zoomRatioZoom2) {
        // Loads bitmap into a TensorImage

        inputImageBuffer.load(bitmap); //image in ARGB_8888
        inputImageBufferZoom1.load(bitmap); //image in ARGB_8888
        inputImageBufferZoom2.load(bitmap); //image in ARGB_8888


        // Creates processor for the TensorImage.
        int cropSize = min(bitmap.getWidth(), bitmap.getHeight());
        int numRotation = sensorOrientation / 90;

        int cropSizeZoom = (int) (cropSize * zoomRatio);

        /** Image preprocessors*/

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSizeZoom, cropSizeZoom))
                        // To get the same inference results as lib_task_api, which is built on top of the Task
                        // Library, use ResizeMethod.BILINEAR. ERA (ResizeMethod.NEAREST_NEIGHBOR)
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new Rot90Op(numRotation))
                        .add(getPreprocessNormalizeOp())
                        .build();

        int cropSizeZoom1 = (int) (cropSize * zoomRatioZoom1);

        ImageProcessor imageProcessorZoom1 =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSizeZoom1, cropSizeZoom1))
                        // To get the same inference results as lib_task_api, which is built on top of the Task
                        // Library, use ResizeMethod.BILINEAR. ERA (ResizeMethod.NEAREST_NEIGHBOR)
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new Rot90Op(numRotation))
                        .add(getPreprocessNormalizeOp())
                        .build();

        int cropSizeZoom2 = (int) (cropSize * zoomRatioZoom2);

        ImageProcessor imageProcessorZoom2 =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSizeZoom2, cropSizeZoom2))
                        // To get the same inference results as lib_task_api, which is built on top of the Task
                        // Library, use ResizeMethod.BILINEAR. ERA (ResizeMethod.NEAREST_NEIGHBOR)
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new Rot90Op(numRotation))
                        .add(getPreprocessNormalizeOp())
                        .build();

        inputImageBuffer = imageProcessor.process(inputImageBuffer);
        inputImageBufferZoom1 = imageProcessorZoom1.process(inputImageBufferZoom1);
        inputImageBufferZoom2 = imageProcessorZoom2.process(inputImageBufferZoom2);

    }

    /**
     * Gets the name of the model file stored in Assets.
     */
    protected abstract String getModelPath();

    /**
     * Gets the name of the label file stored in Assets.
     */
    protected abstract String getLabelPath();

    /**
     * Gets the TensorOperator to nomalize the input image in preprocessing.
     */
    protected abstract TensorOperator getPreprocessNormalizeOp();

    /**
     * Gets the TensorOperator to dequantize the output probability in post processing.
     *
     * <p>For quantized model, we need de-quantize the prediction with NormalizeOp (as they are all
     * essentially linear transformation). For float model, de-quantize is not required. But to
     * uniform the API, de-quantize is added to float model too. Mean and std are set to 0.0f and
     * 1.0f, respectively.
     */
    protected abstract TensorOperator getPostprocessNormalizeOp();

    /**
     * The model type used for classification.
     */
    public enum Model {
        Precise, //MOBILENET_V3_LARGE_100
        Medium, //MOBILENET_V3_LARGE_075
        Fast, //OBILENET_V3_SMALL_100
        QUANTIZED_MOBILENET //NOT USED
    }

    /**
     * The runtime device type used for executing classification.
     */
    public enum Device {
        CPU,
        NNAPI,
        GPU
    }

    /*public enum Language {
        English,
        Italian
    }

     */

    public enum Mode {
        Standard,
        ORB,
        OBJ
    }

    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    public static class Recognition {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private final String id;

        /**
         * Display name for the recognition.
         */
        private final String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private Double confidence;

        /**
         * Optional location within the source image for the location of the recognized object.
         */
        private RectF location;

        public Recognition(
                final String id, final String title, Double confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @NonNull
        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += confidence + " ";
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }
}
