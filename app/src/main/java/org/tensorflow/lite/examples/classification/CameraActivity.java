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
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.provider.Settings;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Language;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Mode;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Recognition;
import org.tensorflow.lite.examples.classification.tflite.DetectionHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
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
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private final byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    private ImageView plusImageView, minusImageView;
    private Spinner modelSpinner;
    private Spinner deviceSpinner;
    private TextView threadsTextView;

    private TextView idView;

    private Model model = Model.PRECISE;
    private Device device = Device.CPU;
    private int numThreads = -1;

    //Popup Recognized
    private final ArrayList<String> recognitionList = new ArrayList<String>();
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private boolean dialogIsOpen = false;
    private boolean sheetIsOpen = false;

    //Language
    private Spinner languageSpinner;
    private Language language = Language.English;

    //Language
    private Spinner modeSpinner;
    private Mode mode = Classifier.Mode.Standard;

    //Loading
    private CircularProgressIndicator loadingIndicator;

    //To check if help (ORB or OBJ_DET) is needed
    private int nClearedList = 0;

    private final String TAG = "Camera Activity";

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.tfe_ic_activity_camera);

        loadingIndicator = findViewById(R.id.progressIndicator);

        loadingIndicator.bringToFront();


        if (hasPermission()) {
            setFragment(); //first creation of classifier
        } else {
            requestPermission();
        }

        threadsTextView = findViewById(R.id.threads);
        plusImageView = findViewById(R.id.plus);
        minusImageView = findViewById(R.id.minus);
        modelSpinner = findViewById(R.id.model_spinner);
        deviceSpinner = findViewById(R.id.device_spinner);
        languageSpinner = findViewById(R.id.language_spinner);
        modeSpinner = findViewById(R.id.mode_spinner);
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        gestureLayout = findViewById(R.id.gesture_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);

        idView = findViewById(R.id.idView);

        //SharedPreferences
        SharedPreferences sharedPref;
        sharedPref = getSharedPreferences("myPref", MODE_PRIVATE);

        String uniqueID = sharedPref.getString("unique_id", "");
        if(uniqueID.equals("")){
            uniqueID = UUID.randomUUID().toString();
            sharedPref.edit().putString("unique_id", uniqueID).commit();
        }

        LOGGER.d("uniqueID: " + uniqueID);
        idView.setText(uniqueID);



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

        /*
        frameValueTextView = findViewById(R.id.frame_info);
        cropValueTextView = findViewById(R.id.crop_info);
        cameraResolutionTextView = findViewById(R.id.view_info);
        rotationTextView = findViewById(R.id.rotation_info);
        inferenceTimeTextView = findViewById(R.id.inference_info);
         */

        modelSpinner.setOnItemSelectedListener(this);
        deviceSpinner.setOnItemSelectedListener(this);
        languageSpinner.setOnItemSelectedListener(this);
        modeSpinner.setOnItemSelectedListener(this);

        plusImageView.setOnClickListener(this);
        minusImageView.setOnClickListener(this);

        model = Model.valueOf(modelSpinner.getSelectedItem().toString().toUpperCase());
        device = Device.valueOf(deviceSpinner.getSelectedItem().toString());
        language = Language.valueOf(languageSpinner.getSelectedItem().toString());
        mode = Mode.valueOf(modeSpinner.getSelectedItem().toString());

        numThreads = Integer.parseInt(threadsTextView.getText().toString().trim());

        //languageSpinner.setAdapter(new SpinnerAdapter(getContext(), new String[]{"Overview", "Story", "Specifications", "Poll", "Video"}, accentColor, backgroundColor));
        languageSpinner.setSelection(0, true);
        View v = languageSpinner.getSelectedView();
        ((TextView) v).setTextSize(15);
        ((TextView) v).setTextColor(Color.WHITE);
        ((TextView) v).bringToFront();
        ((TextView) v).setTypeface((((TextView) v).getTypeface()), Typeface.BOLD);
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
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
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        loadingIndicator.setVisibility(View.VISIBLE);


        //I added this if to continue using camera after having closed app
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());


        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
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

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                                CameraActivity.this,
                                "Camera permission is required for this demo",
                                Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
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

    protected void setFragment() {

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

    @UiThread
    protected void showResultsInBottomSheet(List<Recognition> results, final Bitmap bitmap, int sensorOrientation) {
        Recognition recognition = null;
        Recognition recognition1 = null;
        Recognition recognition2 = null;
        Boolean helped = false;

        //Getting results
        if (results != null && results.size() >= 1 && !dialogIsOpen && !sheetIsOpen) {
            recognition = results.get(0);
            if (results.size() >= 2)
                recognition1 = results.get(1);
            if (results.size() >= 3)
                recognition2 = results.get(2);

            //Title of the winner monument
            String firstPosition = recognition.getTitle();

            if (recognitionList.isEmpty())
                recognitionList.add(firstPosition);
            else if (recognitionList.get(recognitionList.size() - 1).equals(firstPosition))
                recognitionList.add(firstPosition);
            else { // If you do not recognize the same monument in a row, you clear the list
                recognitionList.clear();
                recognitionList.add(firstPosition);

                //If you clear the list 2-3 times in a row
                //If the option is selected
                //You use ORB or OBJ_DET

                nClearedList += 1;
                Log.i(TAG, "Pre-Helping classifier using mode " + mode.toString());
                helped = checkIfHelpIsNeeded(bitmap, sensorOrientation, firstPosition);

            }


            //Printing results with confidences
            if (recognition.getTitle() != null) recognitionTextView.setText(recognition.getTitle());
            if (recognition.getConfidence() != null)
                recognitionValueTextView.setText(
                        //String.format("%.2f", (100 * recognition.getConfidence())) + "%"
                        recognition.getConfidence().toString()
                );


            if (recognition1 != null) {
                if (recognition1.getTitle() != null)
                    recognition1TextView.setText(recognition1.getTitle());
                if (recognition1.getConfidence() != null)
                    recognition1ValueTextView.setText(
                            //String.format("%.2f", (100 * recognition1.getConfidence())) + "%"
                            recognition1.getConfidence().toString()
                    );
            }

            if (recognition2 != null) {
                if (recognition2.getTitle() != null)
                    recognition2TextView.setText(recognition2.getTitle());
                if (recognition2.getConfidence() != null)
                    recognition2ValueTextView.setText(
                            //String.format("%.2f", (100 * recognition2.getConfidence())) + "%"
                            recognition2.getConfidence().toString()
                    );
            }


            //If you recognize the same monument 3 times in a row, you show the popup
            if (recognitionList.size() >= 3 || helped) {

                //Reset cleanings
                nClearedList = 0;

                //Show button more info
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
                        intent.putExtra("language", language.toString());
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

    protected void showFrameInfo(String frameInfo) {
        frameValueTextView.setText(frameInfo);
    }

    protected void showCropInfo(String cropInfo) {
        cropValueTextView.setText(cropInfo);
    }

    protected void showCameraResolution(String cameraInfo) {
        cameraResolutionTextView.setText(cameraInfo);
    }

    protected void showRotationInfo(String rotation) {
        rotationTextView.setText(rotation);
    }

    protected void showInference(String inferenceTime) {
        inferenceTimeTextView.setText(inferenceTime);
    }

    protected Model getModel() {
        return model;
    }

    private void setModel(Model model) {
        if (this.model != model) {
            LOGGER.d("Updating  model: " + model);
            this.model = model;
            onInferenceConfigurationChanged();
        }
    }

    protected Mode getMode() {
        return mode;
    }

    private void setMode(Mode mode) {
        if (this.mode != mode) {
            LOGGER.d("Updating  mode: " + mode);
            this.mode = mode;
            onInferenceConfigurationChanged();
        }
    }

    protected Device getDevice() {
        return device;
    }

    private void setDevice(Device device) {
        if (this.device != device) {
            LOGGER.d("Updating  device: " + device);
            this.device = device;
            final boolean threadsEnabled = device == Device.CPU;
            plusImageView.setEnabled(threadsEnabled);
            minusImageView.setEnabled(threadsEnabled);
            threadsTextView.setText(threadsEnabled ? String.valueOf(numThreads) : "N/A");
            onInferenceConfigurationChanged();
        }
    }

    private void setLanguage(Language language) {
        if (this.language != language) {
            LOGGER.d("Updating  Language: " + language);
            this.language = language;
        }
    }

    protected int getNumThreads() {
        return numThreads;
    }

    private void setNumThreads(int numThreads) {
        if (this.numThreads != numThreads) {
            LOGGER.d("Updating  numThreads: " + numThreads);
            this.numThreads = numThreads;
            onInferenceConfigurationChanged();
        }
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void onInferenceConfigurationChanged();

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.plus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads >= 9) return;
            setNumThreads(++numThreads);
            threadsTextView.setText(String.valueOf(numThreads));
        } else if (v.getId() == R.id.minus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads == 1) {
                return;
            }
            setNumThreads(--numThreads);
            threadsTextView.setText(String.valueOf(numThreads));
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if (parent == modelSpinner) {
            setModel(Model.valueOf(parent.getItemAtPosition(pos).toString().toUpperCase()));
        } else if (parent == deviceSpinner) {
            setDevice(Device.valueOf(parent.getItemAtPosition(pos).toString()));
        } else if (parent == languageSpinner) {
            String s = parent.getItemAtPosition(pos).toString();
            setLanguage(Language.valueOf(s));
            ((TextView) view).setText(s.substring(0, 2).toUpperCase());
            ((TextView) view).setTextSize(15);
            ((TextView) view).setTextColor(Color.WHITE);
            ((TextView) view).bringToFront();
            ((TextView) view).setTypeface((((TextView) view).getTypeface()), Typeface.BOLD);

        } else if (parent == modeSpinner) {
            setMode(Mode.valueOf(parent.getItemAtPosition(pos).toString()));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }
}
