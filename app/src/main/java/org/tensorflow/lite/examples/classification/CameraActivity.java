/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.classification;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.LocationManager;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Mode;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Recognition;
import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;
import org.tensorflow.lite.examples.classification.tflite.DetectionHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
        View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        SensorEventListener {
    private static final Logger LOGGER = new Logger();

    //Remember to change this value according to the metric/score you use in Classifier.java
    private static final float RECOGNITION_THRESHOLD = 1.6f; //Threshold to show the popup
    private static final long SUGGESTION_INTERVAL = 1000 * 10; //Interval between 2 suggestions (in milliseconds) (10 seconds)
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    protected final float[] accelerometerReading = new float[3];
    protected final float[] magnetometerReading = new float[3];
    protected final float[] rotationMatrix = new float[9];
    protected final float[] orientationAngles = new float[3];
    private final byte[][] yuvBytes = new byte[3][];
    //Popup Recognized
    private final ArrayList<String> recognitionList = new ArrayList<String>();
    private final String TAG = "Camera Activity";
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    protected TextView recognitionTextView,
            recognition1TextView,
            recognition2TextView,
            recognitionValueTextView,
            recognition1ValueTextView,
            recognition2ValueTextView;
    protected TextView frameValueTextView,
            cropValueTextView,
            cameraResolutionTextView,
            rotationTextView,
            inferenceTimeTextView;
    protected ImageView bottomSheetArrowImageView;
    protected boolean dialogIsOpen = false;
    protected boolean sheetIsOpen = false;
    //Language
    //private Spinner languageSpinner;
    protected String language;
    protected String uniqueID;
    protected SharedPreferences sharedPreferences;
    protected SensorManager sensorManager;
    private long lastSuggestedPopup = System.currentTimeMillis();
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    //Mode
    private Spinner modeSpinner;

    // Sensor variables to calculate the orientation
    private final Mode mode = Classifier.Mode.Standard;
    //Loading
    private CircularProgressIndicator loadingIndicator;
    private ImageView imageViewBG;
    //To check if help (ORB or OBJ_DET) is needed
    private int nClearedList = 0;
    //To check if we are too far from the monument
    private int nFarMonuments = 0;
    private boolean activateRecognition = false;
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tfe_ic_activity_camera);

        // Set up the sensor manager to get the orientation
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        language = getIntent().getStringExtra("language");
//        if (language == null)
//            language = "English";

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (hasPermission() && hasPermissionGPS()) {
            setFragment(); //first creation of classifier (maybe never called)
        }

        //Toolbar
        Toolbar toolbar = findViewById(R.id.topAppBar);

        toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        loadingIndicator = findViewById(R.id.progressIndicator);

        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        gestureLayout = findViewById(R.id.gesture_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);


        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        //                int width = bottomSheetLayout.getMeasuredWidth();
                        int height = gestureLayout.getMeasuredHeight();

                        sheetBehavior.setPeekHeight(height);
                    }
                });
        sheetBehavior.setHideable(false);

        sheetBehavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED: {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                                Log.v("CameraActivity", "STATE_EXPANDED");
                                sheetIsOpen = true;
                            }
                            break;
                            case BottomSheetBehavior.STATE_COLLAPSED: {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                                Log.v("CameraActivity", "STATE_COLLAPSED");
                                sheetIsOpen = false;

                            }
                            break;
                            case BottomSheetBehavior.STATE_DRAGGING:
                                Log.v("CameraActivity", "STATE_DRAGGING");
                                sheetIsOpen = true;

                                break;
                            case BottomSheetBehavior.STATE_SETTLING:
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                                Log.v("CameraActivity", "STATE_SETTLING");

                                break;
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    }
                });

        recognitionTextView = findViewById(R.id.detected_item);
        recognitionValueTextView = findViewById(R.id.detected_item_value);
        recognition1TextView = findViewById(R.id.detected_item1);
        recognition1ValueTextView = findViewById(R.id.detected_item1_value);
        recognition2TextView = findViewById(R.id.detected_item2);
        recognition2ValueTextView = findViewById(R.id.detected_item2_value);

    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    /**
     * Callback for android.hardware.Camera API
     */

    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 270); /** Camera rotation*/
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }


        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };


        processImage();
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();

        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        loadingIndicator.setVisibility(View.VISIBLE);
        lastSuggestedPopup = System.currentTimeMillis(); // Reset the last suggestion time

        setFragment(); //first creation of classifier

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);
        super.onPause();

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }


        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    private boolean hasPermissionGPS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }


    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API =
                        (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                || isHardwareLevelSupported(
                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() { //creation of the classifier

        String cameraId = chooseCamera();

        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            fragment =
                    new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();

    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {


        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "rotationMatrix" now has up-to-date information.

        //SensorManager.getOrientation(rotationMatrix, orientationAngles);

        // Remap coordinate system
        float[] outGravity = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, outGravity);
        SensorManager.getOrientation(outGravity, orientationAngles);

        // "orientationAngles" now has up-to-date information.


    }

    @UiThread
    protected void showResultsInBottomSheet(List<Recognition> results, final Bitmap bitmap, int sensorOrientation) {
        Recognition recognition = null;
        Recognition recognition1 = null;
        Recognition recognition2 = null;
        Boolean helped = false;


        // Activate recognition if orientation is almost vertical
        updateOrientationAngles();
        float pitch = (float) Math.toDegrees(orientationAngles[1]);

        activateRecognition = pitch >= -60 && pitch <= 30;


        //Getting results
        if (results != null && results.size() >= 1 && !dialogIsOpen && !sheetIsOpen && activateRecognition) {
            recognition = results.get(0);
            if (results.size() >= 2)
                recognition1 = results.get(1);
            if (results.size() >= 3)
                recognition2 = results.get(2);

            //Title of the winner monument
            String firstPosition = recognition.getTitle();

            // Add check on the relative difference between the first and the second position
            // If the difference is too small, you do not show the popup

            if (recognition.getConfidence() >= RECOGNITION_THRESHOLD
                    && (recognition1 == null ||
                    ((recognition.getConfidence() - recognition1.getConfidence()) / recognition.getConfidence() >= 0.3)
            )
            ) {
                if (recognitionList.isEmpty())
                    recognitionList.add(firstPosition);
                else if (recognitionList.get(recognitionList.size() - 1).equals(firstPosition)) {
                    recognitionList.add(firstPosition);
                    nFarMonuments = 0;
                    Log.d(TAG, "nFarMonuments (=0): " + nFarMonuments);
                } else { // If you do not recognize the same monument in a row, you clear the list
                    recognitionList.clear();
                    recognitionList.add(firstPosition);

                    //If you clear the list 2-3 times in a row
                    //If the option is selected
                    //You use ORB or OBJ_DET

                    nClearedList += 1;
                    Log.i(TAG, "Pre-Helping classifier using mode " + mode.toString());
                    helped = checkIfHelpIsNeeded(bitmap, sensorOrientation, firstPosition);

                }
            } else {
                nFarMonuments += 1;
                Log.d(TAG, "nFarMonuments (+1): " + nFarMonuments);
            }


            //Printing results with confidences
            if (recognition.getTitle() != null) recognitionTextView.setText(recognition.getTitle());
            if (recognition.getConfidence() != null) {
                recognitionValueTextView.setText(
                        //String.format("%.2f", (100 * recognition.getConfidence())) + "%"
                        String.format("%.2f", recognition.getConfidence())

                );
            } else {
                recognitionTextView.setText(null);
                recognitionValueTextView.setText(null);
            }


            if (recognition1 != null) {
                if (recognition1.getTitle() != null)
                    recognition1TextView.setText(recognition1.getTitle());
                if (recognition1.getConfidence() != null) {
                    recognition1ValueTextView.setText(
                            //String.format("%.2f", (100 * recognition1.getConfidence())) + "%"
                            String.format("%.2f", recognition1.getConfidence())

                    );
                } else {
                    recognition1TextView.setText(null);
                    recognition1ValueTextView.setText(null);
                }
            }

            if (recognition2 != null) {
                if (recognition2.getTitle() != null)
                    recognition2TextView.setText(recognition2.getTitle());
                if (recognition2.getConfidence() != null) {
                    recognition2ValueTextView.setText(
                            //String.format("%.2f", (100 * recognition2.getConfidence())) + "%"
                            String.format("%.2f", recognition2.getConfidence())
                    );
                } else {
                    recognition2TextView.setText(null);
                    recognition2ValueTextView.setText(null);
                }
            }


            //If you recognize the same monument 3 times in a row, you show the popup
            if (recognitionList.size() >= 3 || helped) {
                boolean openPopup = true;
                if (DatabaseAccess.getSharedPreferences().getBoolean("pref_key_gps_classifier", true)) {
                    // If you want to use the GPS
                    // Check if the recognized monument is in the range of the nearest monument
                    openPopup = monumentIsNearMyLocation(firstPosition);
                }

                //Reset cleanings
                nClearedList = 0;
                nFarMonuments = 0;

                //Show popup to advice to visit the monument
                if (openPopup) {
                    loadingIndicator.setVisibility(View.GONE);

                    Recognition finalRecognition = recognition;

                    dialogBuilder = new AlertDialog.Builder(this);
                    final View popupRecognizedView = getLayoutInflater().inflate(R.layout.popup_recognized, null);
                    TextView recognizedMonumentTextView = popupRecognizedView.findViewById(R.id.monument_recognized);
                    recognizedMonumentTextView.setText(finalRecognition.getTitle());

                    dialogBuilder.setView(popupRecognizedView);
                    dialog = dialogBuilder.create();
                    dialog.show();

                    Button moreInfoButton = popupRecognizedView.findViewById(R.id.more_info_button);

                    moreInfoButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //define button function
                            Intent intent = new Intent(CameraActivity.this, GuideActivity.class);
                            intent.putExtra("monument_id", finalRecognition.getId());
                            intent.putExtra("language", language);
                            intent.putExtra("user_id", uniqueID);

                            startActivity(intent);
                            dialog.dismiss();
                            dialogIsOpen = false;
                        }
                    });

                    Button cancelButton = popupRecognizedView.findViewById(R.id.cancel_button);
                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //define button function
                            dialog.dismiss();
                            dialogIsOpen = false;
                            loadingIndicator.setVisibility(View.VISIBLE);

                        }
                    });

                    recognitionList.clear();
                    dialogIsOpen = true;
                }
            }

            if (nFarMonuments >= 3) { //If the monument recognized is too far 3 times in a row, you show the popup
                nFarMonuments = 0;
                recognitionList.clear();
                String[] nearestMonuments = find3SuggestedMonuments();
                if (nearestMonuments != null
                        && System.currentTimeMillis() - lastSuggestedPopup >= SUGGESTION_INTERVAL
                    // Check if enough time has passed since the last suggestion
                ) {
                    dialogIsOpen = true;
                    loadingIndicator.setVisibility(View.GONE);

                    //Show popup to advice to visit the nearest monument

                    dialogBuilder = new AlertDialog.Builder(this);
                    final View popupRecognizedView = getLayoutInflater().inflate(R.layout.popup_nearest_monument, null);
                    dialogBuilder.setView(popupRecognizedView);
                    dialog = dialogBuilder.create();
                    dialog.show();

                    Button p1_button = popupRecognizedView.findViewById(R.id.sButton1);
                    Log.d(TAG, p1_button.toString());
                    String monument1 = nearestMonuments[0];
                    p1_button.setText(monument1);
                    p1_button.setOnClickListener(view -> {
                        Intent intent = new Intent(CameraActivity.this, GuideActivity.class);
                        intent.putExtra("monument_id", monument1);
                        intent.putExtra("language", language);
                        intent.putExtra("user_id", uniqueID);

                        startActivity(intent);
                    });

                    Button p2_button = popupRecognizedView.findViewById(R.id.sButton2);
                    String monument2 = nearestMonuments[1];
                    p2_button.setText(monument2);
                    p2_button.setOnClickListener(view -> {
                        Intent intent = new Intent(CameraActivity.this, GuideActivity.class);
                        intent.putExtra("monument_id", monument2);
                        intent.putExtra("language", language);
                        intent.putExtra("user_id", uniqueID);
                        startActivity(intent);
                    });

                    Button p3_button = popupRecognizedView.findViewById(R.id.sButton3);
                    String monument3 = nearestMonuments[2];
                    p3_button.setText(monument3);
                    p3_button.setOnClickListener(view -> {
                        Intent intent = new Intent(CameraActivity.this, GuideActivity.class);
                        intent.putExtra("monument_id", monument3);
                        intent.putExtra("language", language);
                        intent.putExtra("user_id", uniqueID);
                        startActivity(intent);
                    });


                    Button cancelButton = popupRecognizedView.findViewById(R.id.cancel_button);
                    //TODO forse è meglio mettere icona con X invece che scrivere CANCEL
                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //define button function
                            dialog.dismiss();
                            dialogIsOpen = false;
                            loadingIndicator.setVisibility(View.VISIBLE);
                            lastSuggestedPopup = System.currentTimeMillis(); // Update the last suggestion time
                        }
                    });
                }

            }
        } else {

            // If you do not recognize anything, clear the output

            recognitionTextView.setText(null);
            recognitionValueTextView.setText(null);

            recognition1TextView.setText(null);
            recognition1ValueTextView.setText(null);

            recognition2TextView.setText(null);
            recognition2ValueTextView.setText(null);

        }


    }

    private boolean checkIfLocationOpened() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        // otherwise return false
    }

    private String[] find3SuggestedMonuments() {
        //Using the GPS, you find 2 nearest monument

        String[] array = new String[3];

        //Check if the GPS is enabled
        if (checkIfLocationOpened() && MainActivity.locationListener != null) {

            //If the GPS is enabled, you get the last known location
            double[] location = MainActivity.locationListener.getCurrentLocation();

            if (location[0] != 0.0) {

                // The first one is the nearest monument
                array[0] = DatabaseAccess.getNearestMonument(location[0], location[1], MainActivity.MAX_DISTANCE);
                int i = 0;
                do {
                    // The second one is randomly chosen from the preferred categories
                    array[1] = DatabaseAccess.getRandomMonumentFromPreferredCategories();
                    i++;
                } while (Objects.equals(array[1], array[0]) && i < 3);
                while (Objects.equals(array[1], array[0]))
                    array[1] = DatabaseAccess.getRandomMonument();
                // if the second one is the same as the first one, you choose another one
                i = 0;
                do {
                    // The third one is randomly chosen
                    array[2] = DatabaseAccess.getRandomMonument();
                    i++;
                } while (Objects.equals(array[2], array[0]) || Objects.equals(array[2], array[1]));
                // if the third one is the same as the first or the second one, you choose another one

                if (array[0] == null || array[1] == null || array[2] == null)
                    return null;

                return array;
            }
        }

        return null;
    }

    private boolean monumentIsNearMyLocation(String monumentName) {

        Log.d(TAG, "locationListener: " + MainActivity.locationListener);

        //Check if the GPS is enabled
        if (checkIfLocationOpened() && MainActivity.locationListener != null) {

            //If the GPS is enabled, you get the last known location
            double[] location = MainActivity.locationListener.getCurrentLocation();
            Log.d(TAG, "Location: " + location[0] + " " + location[1]);

            if (location[0] != 0.0) {

                //Calculate distance between my location and the monument
                double[] coord = DatabaseAccess.getCoordinates(monumentName);
                if (coord == null) {
                    Log.d(TAG, "Monument not found");
                    return false; // monument not found (return false to avoid showing the popup)
                }


                double distance = DatabaseAccess.distance2Positions(location, coord);
                Log.d(TAG, "Distance between Me and " + monumentName + ": " + distance + " meters");


                if (distance > MainActivity.MAX_DISTANCE_RECOGNIZED)
                    return false; // monument too far (return false to avoid showing the popup)
            }
        }
        Log.d(TAG, "Location is enabled: " + checkIfLocationOpened());
        return true; // location is not enabled
    }

    /**
     * If you clear the list 2-3 times in a row && If the option is selected
     * You call ORB or OBJ_DET
     * If the result is good enough, you use it to recognize the monument
     */
    @UiThread
    protected Boolean checkIfHelpIsNeeded(final Bitmap bitmap, int sensorOrientation, String firstPosition) {
        if (nClearedList >= 3 && mode != Mode.Standard) {

            Log.i(TAG, "Helping classifier using mode " + mode.toString());
            DetectionHelper dH = new DetectionHelper(getApplicationContext(), mode, bitmap, firstPosition);
            float good_ratio = dH.help();

            Log.i(TAG, "Good ratio: " + good_ratio);

            if (good_ratio >= 0.1) {
                return true;
            }


            return true;
        }
        return false;
    }

    protected Model getModel() {
        return Model.valueOf(sharedPreferences.getString("pref_key_model", "Precise"));
    }

    /*
    private void setModel(Model model) {
        if (this.model != model) {
            LOGGER.d("Updating  model: " + model);
            this.model = model;
            onInferenceConfigurationChanged();
        }
    }
     */

    protected Mode getMode() {
        return Mode.valueOf(sharedPreferences.getString("pref_key_mode", "Standard"));
    }

    protected Device getDevice() {
        return Device.valueOf(sharedPreferences.getString("pref_key_device", "CPU"));
    }

    protected int getNumThreads() {
        return Integer.parseInt(sharedPreferences.getString("pref_key_num_threads", "1"));
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void onInferenceConfigurationChanged();
}
