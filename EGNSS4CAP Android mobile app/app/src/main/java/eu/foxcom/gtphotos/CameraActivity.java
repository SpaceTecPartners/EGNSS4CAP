package eu.foxcom.gtphotos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.location.Location;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.DateTime;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.foxcom.gtphotos.model.PersistData;
import eu.foxcom.gtphotos.model.Photo;
import eu.foxcom.gtphotos.model.PhotoDataController;
import eu.foxcom.gtphotos.model.Util;
import eu.foxcom.gtphotos.model.component.SeekBarAPI26;
import eu.foxcom.gtphotos.model.component.VerticalSeekBarAPI26;

public class CameraActivity extends BaseActivity implements CameraXConfig.Provider {

    enum MSG {
        WARNING(R.id.ca_textView_warning),
        INFO(R.id.ca_textView_info);

        public final int TEXT_VIEW_ID;

        MSG(int textViewId) {
            TEXT_VIEW_ID = textViewId;
        }
    }

    enum WARNING_MSG {
        BAD_ANGLE,
        NO_DATA_LOCATION,
        NO_CENTROID_LOCATION;
    }

    enum INFO_MSG {
        TAKE_PICTURE_WAIT,
    }

    private static final int SETTINGS_WIDTH_DP = 50;
    private static final int SETTINGS_HEIGHT_DP = 350;
    private static final int SNAP_BUTTON_WIDTH_DP = 200;
    private static final int SNAP_BUTTON_HEIGHT_DP = 50;


    public static final String INTENT_ACTION_START = "intentStart";
    public static final String INTENT_ACTION_START_TASK_ID = "taskId";
    public static final int MAX_INFO_LOCATION_AGE_MILS = 5000;
    public static final int INTERVAL_UPDATE_PHOTO_CONTROLLER_DATA_MILS = 500;
    public static final int INTERVAL_UPDATE_MSG_MILS = 4000;
    public static final int INTERVAL_TAKE_PHOTO_MILS = 5000;
    public static final int INTERVAL_REFRESH_COUNT_DOWN_TAKE_PHOTO_MILS = 1000;

    public static final String TAG = CameraActivity.class.getSimpleName();

    private String taskId;
    private String currentPhotoPath;

    private ImageCapture imageCapture;
    private Preview preview;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private CameraCaptureSession cameraCaptureSessionPreview;
    private CameraCaptureSession cameraCaptureSessionImageCapture;
    private CameraSelector cameraSelector;
    private Camera camera;
    private CameraExposureCorrector cameraExposureCorrector;

    private CameraViewModel cameraViewModel;

    private DecimalFormat decimalFormat0;
    private DecimalFormat decimalFormat2;
    private DecimalFormat decimalFormat7;
    private Location lastLocation;
    private Runnable updatorPhotoControllerDataRunnable;
    private Handler updatorPhotoControllerDataHandler;

    private Map<String, String> warningMessages = new LinkedHashMap<>();
    private Map<String, String> infoMessages = new LinkedHashMap<>();
    private Handler updatorMessagesHandler;
    private Runnable updatorMessagesRunnable;
    private Display display;
    private AtomicBoolean isTakingPhoto;
    private Handler countDownTakePhotoHandler;
    private Runnable countDownTakePhotoRunnable;
    private DateTime countDownTakePhotoDateTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!intentActionStart()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_camera);
        toggleMainLayout(getResources().getConfiguration());
        getSupportActionBar().hide();
        ConstraintLayout mainLayout = findViewById(R.id.ca_constraintLayout_main);
        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainClickResolver();
            }
        });
        display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        isTakingPhoto = new AtomicBoolean(false);
        previewView = (PreviewView) findViewById(R.id.ca_previewView_camera);
        decimalFormat0 = new DecimalFormat("#");
        decimalFormat2 = new DecimalFormat("#.##");
        decimalFormat7 = new DecimalFormat("#.#######");
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        decimalFormat0.setDecimalFormatSymbols(decimalFormatSymbols);
        decimalFormat2.setDecimalFormatSymbols(decimalFormatSymbols);
        decimalFormat7.setDecimalFormatSymbols(decimalFormatSymbols);
        toFullScreen();
        initConfigExposureCorrection();
        startCamera();
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);
        cameraViewModel.init(MS);
        cameraViewModel.getCurrentLocation().observe(this, new Observer<Location>() {
            @Override
            public void onChanged(Location location) {
                lastLocation = location;
            }
        });

        updatorPhotoControllerDataHandler = new Handler(Looper.getMainLooper());
        updatorPhotoControllerDataRunnable = new Runnable() {
            @Override
            public void run() {
                updateLocationInfo();
                updateSensorData();
                checkData();
                updatorPhotoControllerDataHandler.postDelayed(this, INTERVAL_UPDATE_PHOTO_CONTROLLER_DATA_MILS);
            }
        };

        updatorMessagesHandler = new Handler(Looper.getMainLooper());
        updatorMessagesRunnable = new Runnable() {
            @Override
            public void run() {
                refreshMessages(MSG.WARNING);
                refreshMessages(MSG.INFO);
                updatorMessagesHandler.postDelayed(this, INTERVAL_UPDATE_PHOTO_CONTROLLER_DATA_MILS);
            }
        };
        updatorPhotoControllerDataHandler.postDelayed(updatorPhotoControllerDataRunnable, INTERVAL_UPDATE_PHOTO_CONTROLLER_DATA_MILS);
        updatorMessagesHandler.postDelayed(updatorMessagesRunnable, INTERVAL_UPDATE_MSG_MILS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            initComputeCentroidLocation();
        }
    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }


    private void updateLocationInfo() {
        TextView altitudeTextView = findViewById(R.id.ca_textView_altitude);
        TextView longitudeTextView = findViewById(R.id.ca_textView_longitude);
        TextView latitudeTextView = findViewById(R.id.ca_textView_latitude);
        TextView accuracyTextView = findViewById(R.id.ca_textView_accuracy);
        if (lastLocation != null && lastLocation.getTime() + MAX_INFO_LOCATION_AGE_MILS > DateTime.now().toDate().getTime()) {
            latitudeTextView.setText(String.valueOf(lastLocation.getLatitude()));
            longitudeTextView.setText(String.valueOf(lastLocation.getLongitude()));
            altitudeTextView.setText(decimalFormat2.format(lastLocation.getAltitude()));
            accuracyTextView.setText(decimalFormat2.format(lastLocation.getAccuracy()));
        } else {
            String unavailable = getString(R.string.ca_unavailable);
            latitudeTextView.setText(unavailable);
            longitudeTextView.setText(unavailable);
            altitudeTextView.setText(unavailable);
            accuracyTextView.setText(unavailable);
        }

    }

    private void updateSensorData() {
        TextView azimTextView = findViewById(R.id.ca_textView_azimuth);
        cameraViewModel.getPhotoDataController().getPositionSensorController().updateOrientationAnglesAverage();
        double azimuth = cameraViewModel.getPhotoDataController().getPositionSensorController().getAzimuthDegreesAverage();
        azimTextView.setText(decimalFormat0.format(azimuth));
        TextView tiltTextView = findViewById(R.id.ca_textView_tilt);
        double tilt = cameraViewModel.getPhotoDataController().computeTilt(display.getRotation());
        tiltTextView.setText(decimalFormat0.format(tilt));
        TextView photoHeadingTextView = findViewById(R.id.ca_textView_photoHeading);
        photoHeadingTextView.setText(decimalFormat0.format(cameraViewModel.getPhotoDataController().computePhotoHeading(display.getRotation(), tilt, azimuth)));
    }

    private boolean checkData() {
        int orientation = getResources().getConfiguration().orientation;
        boolean angleCorrect = cameraViewModel.getPhotoDataController().isAngleCorrect(cameraViewModel.getPhotoDataController().computeTilt(display.getRotation()), orientation);
        // kontrola naklonění vypnuta
        angleCorrect = true;
        if (!angleCorrect) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                addWarning(WARNING_MSG.BAD_ANGLE, getString(R.string.ca_wrongAnglePortrait,
                        decimalFormat0.format(PhotoDataController.MIN_PORTRAIT_ANGLE),
                        decimalFormat0.format(PhotoDataController.MAX_PORTRAIT_ANGLE)));
            } else {
                addWarning(WARNING_MSG.BAD_ANGLE, getString(R.string.ca_wrongAngleLandscape,
                        decimalFormat0.format(PhotoDataController.MIN_LANDSCAPE_ANGLE),
                        decimalFormat0.format(PhotoDataController.MAX_LANDSCAPE_ANGLE)));
            }
        } else {
            removeWarning(WARNING_MSG.BAD_ANGLE);
        }
        boolean locationsCorrect = cameraViewModel.photoDataController.isLocationCorrect();
        if (!locationsCorrect) {
            addWarning(WARNING_MSG.NO_DATA_LOCATION, getString(R.string.ca_unsatifactoryPosition));
        } else {
            removeWarning(WARNING_MSG.NO_DATA_LOCATION);
        }
        boolean centroidCorrect = (
                !PersistData.getPhotoWithCentroiLocation(this)
                        || (
                        cameraViewModel.getPhotoDataController().getCentroidLatitude() != null
                                && cameraViewModel.getPhotoDataController().getCentroidLongitude() != null));
        if (!centroidCorrect) {
            addWarning(WARNING_MSG.NO_CENTROID_LOCATION, getString(R.string.ca_noCentroidLocation));
        } else {
            removeWarning(WARNING_MSG.NO_CENTROID_LOCATION);
        }
        boolean correct = angleCorrect && locationsCorrect && centroidCorrect;
        toggleButtonSnapShot(correct && !isTakingPhoto.get());
        return correct;
    }

    private void toggleButtonSnapShot(boolean enabled) {
        ImageButton imageButton = findViewById(R.id.ca_imageButton_snap);
        if (enabled) {
            imageButton.setVisibility(View.VISIBLE);
        } else {
            imageButton.setVisibility(View.INVISIBLE);
        }
        imageButton.setEnabled(enabled);
    }

    private boolean intentActionStart() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        if (intent.getAction() != null && !intent.getAction().equals(INTENT_ACTION_START)) {
            return false;
        }
        if (intent.hasExtra(INTENT_ACTION_START_TASK_ID)) {
            taskId = intent.getStringExtra(INTENT_ACTION_START_TASK_ID);
        }
        return true;
    }

    private void initConfigExposureCorrection() {
        ImageButton settingsImageButton = findViewById(R.id.ca_imageButton_settingsToggle);
        if (!SettingsActivity.isManualBrightnessActive(this)) {
            settingsImageButton.setVisibility(View.GONE);
            return;
        }
        VerticalSeekBarAPI26 bar = findViewById(R.id.ca_verticalSeekBarApi26_exposureCorrection);
        TextView expCorTextureView = findViewById(R.id.ca_textView_exposureCorrection);
        CameraExposureCorrector exposureCorrector = acquireExposureCorrector();
        bar.setMinMax(0, 100);
        bar.setEnabled(exposureCorrector.isAvailable());
        bar.setOnSeekBarChangeListenerAPI26(new SeekBarAPI26.OnSeekBarChangeListenerAPI26() {
            @Override
            public void onProgressChanged(SeekBarAPI26 seekBar, int progress, boolean fromUser) {
                // mrtvá zóna mezi <-15;15> % na 0 %
                if (progress >= 42 && progress <= 58 && progress != 50) {
                    seekBar.setProgress(50);
                    return;
                }
                if (!exposureCorrector.isAvailable()) {
                    return;
                }
                int correction = exposureCorrector.scaleToRealValue(progress);
                exposureCorrector.setCorrection(exposureCorrector.scaleToRealValue(progress));
                int extPer = exposureCorrector.scaleToExtendedPercent(correction);
                expCorTextureView.setText(extPer + " %");
                cameraExposureCorrectPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBarAPI26 seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBarAPI26 seekBar) {

            }
        });
    }

    public void alertExposureCorrectionSupport(View view) {
        if (cameraExposureCorrector == null || !cameraExposureCorrector.isInitilized()) {
            return;
        }
        if (!cameraExposureCorrector.isAvailable()) {
            alert(getString(R.string.ca_unsupportedSettingsTitle), getString(R.string.ca_unssuprotedSettingsText));
        }
    }

    private CameraExposureCorrector acquireExposureCorrector() {
        if (cameraExposureCorrector == null) {
            cameraExposureCorrector = new CameraExposureCorrector(this);
        }
        return cameraExposureCorrector;
    }

    private ImageCapture.Builder createBuilderImageCaptureUseCase() {
        return new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                //.setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetResolution(new Size(1920, 1080))
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                // oprava s garancí pro samsung a MI 9
                .setTargetRotation(Surface.ROTATION_90)
                ;
    }

    private Preview.Builder createBuilderPreviewUseCase() {
        return new Preview.Builder();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                toFullScreen();
            }
        });

        if (serviceController.isServiceInitialized()) {
            updatorPhotoControllerDataHandler.postDelayed(updatorPhotoControllerDataRunnable, INTERVAL_UPDATE_PHOTO_CONTROLLER_DATA_MILS);
            updatorMessagesHandler.postDelayed(updatorMessagesRunnable, INTERVAL_UPDATE_MSG_MILS);
        }

        if (cameraProvider != null) {
            cameraProviderRebindPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceController.isServiceInitialized()) {
            updatorPhotoControllerDataHandler.removeCallbacks(updatorPhotoControllerDataRunnable);
            updatorMessagesHandler.removeCallbacks(updatorMessagesRunnable);
        }
    }

    private void toFullScreen() {
        if (true) {
            return;
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraFuture = ProcessCameraProvider.getInstance(this);
        cameraFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = (ProcessCameraProvider) cameraFuture.get();
                    cameraProviderRebindPreview();
                } catch (ExecutionException e) {
                    Log.e(TAG, "startCamera failed", e);
                } catch (InterruptedException e) {
                    Log.e(TAG, "startCamera failed", e);
                } catch (Exception e) {
                    Log.e(TAG, "startCamera failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private CameraSelector acquireCameraSelector() {
        if (cameraSelector == null) {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        }
        return cameraSelector;
    }

    private void configExtender(Camera2Interop.Extender extender) {
        if (SettingsActivity.isManualBrightnessActive(this)) {
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, PersistData.getExposureCorrection(this));
        }
    }

    private void cameraRebindSettings() {
        if (!SettingsActivity.isManualBrightnessActive(this)) {
            return;
        }
        CameraExposureCorrector cameraExposureCorrector = acquireExposureCorrector();
        if (!cameraExposureCorrector.isInitilized()) {
            cameraExposureCorrector.init(camera);
            if (cameraExposureCorrector.isAvailable()) {
                ImageButton settingsImageButton = findViewById(R.id.ca_imageButton_settingsToggle);
                settingsImageButton.setVisibility(View.VISIBLE);
            }
            VerticalSeekBarAPI26 barAPI26 = findViewById(R.id.ca_verticalSeekBarApi26_exposureCorrection);
            if (cameraExposureCorrector.isAvailable()) {
                barAPI26.setEnabled(true);
                int correction = cameraExposureCorrector.getCorrection();
                barAPI26.setProgress(cameraExposureCorrector.scaleToPercent(correction));
                TextView correctionTextView = findViewById(R.id.ca_textView_exposureCorrection);
                correctionTextView.setText(cameraExposureCorrector.scaleToExtendedPercent(correction) + " %");
            } else {
                barAPI26.setEnabled(false);
            }
        }
    }

    // rebind with only preview
    private void cameraProviderRebindPreview() {
        Preview.Builder previewBuilder = createBuilderPreviewUseCase();
        Camera2Interop.Extender extender = new Camera2Interop.Extender(previewBuilder);
        configExtender(extender);
        extender.setSessionStateCallback(new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                cameraCaptureSessionPreview = session;
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }

            @Override
            public void onClosed(@NonNull CameraCaptureSession session) {
                super.onClosed(session);
                cameraCaptureSessionPreview = null;
            }
        });
        if (preview != null) {
            cameraProvider.unbind(preview);
        }
        preview = previewBuilder.build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        camera = cameraProvider.bindToLifecycle(this, acquireCameraSelector(), preview);
        cameraRebindSettings();
    }

    private void cameraProviderResetToPreview() {
        cameraProvider.unbindAll();
        cameraProviderRebindPreview();
    }

    @SuppressLint("RestrictedApi")
    private boolean cameraExposureCorrectPreview() {
        if (cameraCaptureSessionPreview == null) {
            return false;
        }
        CameraExposureCorrector exposureCorrector = acquireExposureCorrector();
        if (!exposureCorrector.isAvailable()) {
            return false;
        }
        try {
            CameraDevice cameraDevice = cameraCaptureSessionPreview.getDevice();
            List<DeferrableSurface> surfaces = preview.getSessionConfig().getSurfaces();
            if (surfaces.size() == 0) {
                Log.e(TAG, "Error during runtime exposure compensation. No surface available.");
                return false;
            }
            Surface surface = surfaces.get(0).getSurface().get();
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(preview.getSessionConfig().getTemplateType());
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureCorrector.getCorrection());
            cameraCaptureSessionPreview.setRepeatingRequest(captureRequestBuilder.build(), null, null);
            return true;
        } catch (CameraAccessException | IllegalStateException | InterruptedException | ExecutionException e) {
            Log.e(TAG, "Error during runtime exposure compensation.", e);
            return false;
        }
    }

    // rebind with only imageCapture
    private void cameraProviderRebindImageCapture() {
        ImageCapture.Builder imageCaptureBuilder = createBuilderImageCaptureUseCase();
        Camera2Interop.Extender extender = new Camera2Interop.Extender(imageCaptureBuilder);
        configExtender(extender);
        extender.setSessionStateCallback(new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                cameraCaptureSessionImageCapture = session;
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }

            @Override
            public void onClosed(@NonNull CameraCaptureSession session) {
                super.onClosed(session);
                cameraCaptureSessionImageCapture = null;
            }
        });
        if (imageCapture != null) {
            cameraProvider.unbind(imageCapture);
        }
        imageCapture = imageCaptureBuilder.build();
        camera = cameraProvider.bindToLifecycle(this, acquireCameraSelector(), imageCapture);
        cameraRebindSettings();
    }

    private void cameraProviderResetToImageCapture() {
        cameraProvider.unbindAll();
        cameraProviderRebindImageCapture();
    }

    public void takePhoto(View view) {
        synchronized (isTakingPhoto) {
            if (isTakingPhoto.get()) {
                return;
            }
            hideCameraSettings();
            if (!checkData()) {
                return;
            }
            isTakingPhoto.set(true);
            cameraViewModel.getPhotoDataController().startSnapShot();
            countDownTakingPhoto();
        }
    }

    private void countDownTakingPhoto() {
        countDownTakePhotoDateTime = DateTime.now();
        countDownTakePhotoHandler = new Handler();
        countDownTakePhotoRunnable = new Runnable() {
            @Override
            public void run() {
                long remain = INTERVAL_TAKE_PHOTO_MILS - (DateTime.now().toDate().getTime() - countDownTakePhotoDateTime.toDate().getTime());
                if (remain < 0) {
                    remain = 0;
                }
                if (remain == 0) {
                    capturePhoto();
                } else {
                    addInfo(INFO_MSG.TAKE_PICTURE_WAIT, getString(R.string.ca_takePictureWait) + "\n" +
                            getString(R.string.ca_takePictureCountDown, decimalFormat0.format(remain / 1000)));
                    countDownTakePhotoHandler.postDelayed(this, INTERVAL_REFRESH_COUNT_DOWN_TAKE_PHOTO_MILS);
                }
            }
        };
        countDownTakePhotoHandler.postDelayed(countDownTakePhotoRunnable, 0);
    }

    private void capturePhoto() {
        synchronized (isTakingPhoto) {
            addInfo(INFO_MSG.TAKE_PICTURE_WAIT, getString(R.string.ca_takePictureWait) + "\n" +
                    getString(R.string.ca_takePictureCalibrating));
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                alert(getString(R.string.gn_failure), getString(R.string.ca_failedCreateFile, e.getMessage()));
                isTakingPhoto.set(false);
                removeInfo(INFO_MSG.TAKE_PICTURE_WAIT);
                return;
            }

            cameraViewModel.getPhotoDataController().startSnapShotFinish(display.getRotation());

            //imageCaptureTakePicture_manual(photoFile);
            // oprava s garancí pro samsung a MI 9
            imageCaptureTakePicture_buggy(photoFile);
        }
    }

    // nefunguje na starších verzích
    // trvá zbytečně dlouho
    private void imageCaptureTakePicture_manual(File photoFile) {
        cameraProviderResetToImageCapture();
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                synchronized (isTakingPhoto) {
                    cameraProvider.unbind(preview);
                    cameraViewModel.getPhotoDataController().stop();
                    if (checkData()) {
                        /* manuální oprava rotace */
                        int rotationDegrees = image.getImageInfo().getRotationDegrees();
                        @SuppressLint("UnsafeExperimentalUsageError") Image img = image.getImage();
                        ImageProxy.PlaneProxy[] planeProxy = image.getPlanes();
                        ByteBuffer buffer = planeProxy[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotationDegrees);
                        bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), matrix, true);
                        super.onCaptureSuccess(image);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] byteArray = stream.toByteArray();
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(photoFile);
                            fileOutputStream.write(byteArray);
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        } catch (IOException e) {
                            alert(getString(R.string.gn_failure), getString(R.string.ca_failedSavePhoto, e.getMessage()));
                            isTakingPhoto.set(false);
                            removeInfo(INFO_MSG.TAKE_PICTURE_WAIT);
                            cameraViewModel.getPhotoDataController().startImmediately();
                            cameraProviderResetToPreview();
                            return;
                        }
                        sendFile();
                    } else {
                        isTakingPhoto.set(false);
                        removeInfo(INFO_MSG.TAKE_PICTURE_WAIT);
                        cameraViewModel.getPhotoDataController().startImmediately();
                        cameraProviderResetToPreview();
                        alert(getString(R.string.ca_pictureRejectedTitle), getString(R.string.ca_pictureRejectedText));
                    }
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                synchronized (isTakingPhoto) {
                    alert(getString(R.string.gn_failure), getString(R.string.ca_failedSavePhoto, exception.getMessage()));
                    isTakingPhoto.set(false);
                    super.onError(exception);
                }
            }
        });
    }

    // nerespektuje rotaci
    private void imageCaptureTakePicture_buggy(File photoFile) {
        cameraProviderResetToImageCapture();
        ImageCapture.OutputFileOptions fileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(fileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                cameraProvider.unbind(preview);
                cameraViewModel.getPhotoDataController().stop();
                if (checkData()) {
                    sendFile();
                } else {
                    isTakingPhoto.set(false);
                    cameraViewModel.getPhotoDataController().startImmediately();
                    cameraProviderResetToPreview();
                    removeInfo(INFO_MSG.TAKE_PICTURE_WAIT);
                    alert(getString(R.string.ca_pictureRejectedTitle), getString(R.string.ca_pictureRejectedText));
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                synchronized (isTakingPhoto) {
                    alert(getString(R.string.gn_failure), getString(R.string.ca_failedSavePhoto, exception.getMessage()));
                    removeInfo(INFO_MSG.TAKE_PICTURE_WAIT);
                    isTakingPhoto.set(false);
                }
            }
        });
    }

    private void sendFile() {
        synchronized (isTakingPhoto) {
            try {
                Photo photo = Photo.createFromPhotoDataController(taskId, cameraViewModel.getPhotoDataController(), DateTime.now(), currentPhotoPath, getApplicationContext(), MS.getAppDatabase());
                photo.refreshToDB(MS.getAppDatabase());
                sendFileIntent(photo);
            } catch (IOException | JSONException e) {
                alert(getString(R.string.gn_failure), getString(R.string.ca_failedSavePhoto, e.getMessage()));
                isTakingPhoto.set(false);
                cameraProviderResetToPreview();
                cameraViewModel.getPhotoDataController().startImmediately();
            }
            removeInfo(INFO_MSG.TAKE_PICTURE_WAIT);
        }
    }

    private void sendFileIntent(Photo photo) {
        Intent intent;
        if (taskId != null) {
            intent = new Intent(this, TaskFulfillActivity.class);
            intent.setAction(TaskFulfillActivity.INTENT_ACTION_SNAP_PHOTO);
            intent.putExtra(TaskFulfillActivity.INTENT_ACTION_START_SNAP_PHOTO_ID, photo.getId());
        } else {
            /* přechod na overview
            intent = new Intent(this, UnownedPhotoOverviewActivity.class);
            intent.setAction(UnownedPhotoOverviewActivity.INTENT_ACTION_REFRESH_PHOTOS);
            /**/

            // přechod na detail
            intent = new Intent(this, UnownedPhotoDetailActivity.class);
            intent.setAction(UnownedPhotoDetailActivity.INTENT_ACTION_START);
            intent.putExtra(UnownedPhotoDetailActivity.INTENT_ACTION_START_PHOTO_ID, photo.getId());
            intent.putExtra(UnownedPhotoDetailActivity.INTENT_ACTION_START_NEW_PHOTO, true);
        }
        startActivity(intent);
        finish();
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(
                storageDir,
                taskId + "_photo_temp.data"
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void addWarning(WARNING_MSG warningMsg, String text) {
        addMessage(MSG.WARNING, warningMsg.name(), text);
    }

    private void addInfo(INFO_MSG infoMsg, String text) {
        addMessage(MSG.INFO, infoMsg.name(), text);
    }

    private void removeWarning(WARNING_MSG warningMsg) {
        removeMessage(MSG.WARNING, warningMsg.name());
    }

    private void removeInfo(INFO_MSG infoMsg) {
        removeMessage(MSG.INFO, infoMsg.name());
    }

    private void removeMessage(MSG msg, String id) {
        Map<String, String> messages = getMesssages(msg);
        synchronized (messages) {
            messages.remove(id);
        }
        refreshMessages(msg);
    }

    private Map<String, String> getMesssages(MSG msg) {
        if (msg.equals(MSG.INFO)) {
            return infoMessages;
        } else if (msg.equals(MSG.WARNING)) {
            return warningMessages;
        } else {
            return null;
        }
    }

    private void addMessage(MSG msg, String id, String text) {
        Map<String, String> messages = getMesssages(msg);
        synchronized (messages) {
            messages.put(id, text);
        }
        refreshMessages(msg);
    }

    private void refreshMessages(MSG msg) {
        Map<String, String> messages = getMesssages(msg);
        synchronized (messages) {
            TextView textView = findViewById(msg.TEXT_VIEW_ID);
            if (messages.size() > 0) {
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setVisibility(View.INVISIBLE);
            }
            String text = "";
            int indx = 0;
            int size = messages.size();
            for (Map.Entry<String, String> entry : messages.entrySet()) {
                text += entry.getValue();
                if (indx != size - 1) {
                    text += "\n\n";
                }
                ++indx;
            }
            textView.setText(text);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTakePhotoHandler != null && countDownTakePhotoRunnable != null) {
            countDownTakePhotoHandler.removeCallbacks(countDownTakePhotoRunnable);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggleMainLayout(newConfig);
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void initComputeCentroidLocation() {
        LinearLayout infoLinearLayout = findViewById(R.id.ca_linearLayout_centroidLocation);
        if (PersistData.getPhotoWithCentroiLocation(this)) {
            infoLinearLayout.setVisibility(View.VISIBLE);
            TextView sampleCountTextView = findViewById(R.id.ca_textView_sampleCount);
            sampleCountTextView.setText("0 / " + PersistData.getSamplingNumber(this));
            cameraViewModel.getPhotoDataController().startCentroid(new PhotoDataController.CentroidComputedReceiver() {
                @Override
                public void receive(double latitude, double longitude) {
                    TextView latTextView = findViewById(R.id.ca_textView_centroidLatitude);
                    latTextView.setText(decimalFormat7.format(latitude));
                    TextView lngTextView = findViewById(R.id.ca_textView_centroidLongitude);
                    lngTextView.setText(decimalFormat7.format(longitude));
                }
            }, new PhotoDataController.CentroidSampleAddReceiver() {
                @Override
                public void receive(int count) {
                    sampleCountTextView.setText(String.valueOf(count) + "/" + PersistData.getSamplingNumber(CameraActivity.this));
                }
            });
        } else {
            infoLinearLayout.setVisibility(View.GONE);
        }
    }

    private void toggleMainLayout(Configuration newConfig) {
        ConstraintLayout mainConstraintLayout = findViewById(R.id.ca_constraintLayout_main);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainConstraintLayout);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // message
            constraintSet.connect(R.id.ca_linearLayout_messages, ConstraintSet.TOP, R.id.ca_constraintLayout_info, ConstraintSet.BOTTOM);
            constraintSet.connect(R.id.ca_linearLayout_messages, ConstraintSet.END, R.id.ca_constraintLayout_main, ConstraintSet.END);
            constraintSet.connect(R.id.ca_linearLayout_messages, ConstraintSet.BOTTOM, R.id.ca_imageButton_snap, ConstraintSet.TOP);
            constraintSet.connect(R.id.ca_linearLayout_messages, ConstraintSet.START, R.id.ca_constraintLayout_main, ConstraintSet.START);
            // exit button
            constraintSet.clear(R.id.ca_imageButton_exit, ConstraintSet.END);
            constraintSet.connect(R.id.ca_imageButton_exit, ConstraintSet.START, R.id.ca_constraintLayout_main, ConstraintSet.START);
            constraintSet.setMargin(R.id.ca_imageButton_exit, ConstraintSet.START, Util.dpToPixels(this, 8));
            // snap button
            constraintSet.connect(R.id.ca_imageButton_snap, ConstraintSet.START, R.id.ca_constraintLayout_main, ConstraintSet.START);
            constraintSet.clear(R.id.ca_imageButton_snap, ConstraintSet.TOP);
            constraintSet.setMargin(R.id.ca_imageButton_snap, ConstraintSet.END, 0);
            constraintSet.setMargin(R.id.ca_imageButton_snap, ConstraintSet.BOTTOM, Util.dpToPixels(this, 8));
            constraintSet.constrainWidth(R.id.ca_imageButton_snap, Util.dpToPixels(this, SNAP_BUTTON_WIDTH_DP));
            constraintSet.constrainHeight(R.id.ca_imageButton_snap, Util.dpToPixels(this, SNAP_BUTTON_HEIGHT_DP));
            // exposure button
            constraintSet.clear(R.id.ca_imageButton_settingsToggle, ConstraintSet.TOP);
            constraintSet.connect(R.id.ca_imageButton_settingsToggle, ConstraintSet.BOTTOM, R.id.ca_constraintLayout_main, ConstraintSet.BOTTOM);
            constraintSet.setMargin(R.id.ca_imageButton_settingsToggle, ConstraintSet.BOTTOM, Util.dpToPixels(this, 8));
            constraintSet.connect(R.id.ca_imageButton_settingsToggle, ConstraintSet.END, R.id.ca_constraintLayout_main, ConstraintSet.END);
            constraintSet.setMargin(R.id.ca_imageButton_settingsToggle, ConstraintSet.END, Util.dpToPixels(this, 8));
            // exposure settings
            constraintSet.clear(R.id.ca_linearLayout_settings, ConstraintSet.TOP);
            constraintSet.clear(R.id.ca_linearLayout_settings, ConstraintSet.END);
            constraintSet.connect(R.id.ca_linearLayout_settings, ConstraintSet.END, R.id.ca_constraintLayout_main, ConstraintSet.END);
            constraintSet.setMargin(R.id.ca_linearLayout_settings, ConstraintSet.END, Util.dpToPixels(this, 8));
            constraintSet.connect(R.id.ca_linearLayout_settings, ConstraintSet.BOTTOM, R.id.ca_imageButton_settingsToggle, ConstraintSet.TOP);
            constraintSet.constrainWidth(R.id.ca_linearLayout_settings, Util.dpToPixels(this, SETTINGS_WIDTH_DP));
            constraintSet.constrainHeight(R.id.ca_linearLayout_settings, Util.dpToPixels(this, SETTINGS_HEIGHT_DP));
            LinearLayout correctionLinearLayout = findViewById(R.id.ca_linearLayout_exposureCorrection);
            correctionLinearLayout.setOrientation(LinearLayout.VERTICAL);
            VerticalSeekBarAPI26 correctionBar = findViewById(R.id.ca_verticalSeekBarApi26_exposureCorrection);
            correctionBar.setRotation(0);
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // message
            constraintSet.connect(R.id.ca_linearLayout_messages, ConstraintSet.TOP, R.id.ca_constraintLayout_main, ConstraintSet.TOP);
            constraintSet.connect(R.id.ca_linearLayout_messages, ConstraintSet.END, R.id.ca_imageButton_snap, ConstraintSet.START);
            constraintSet.connect(R.id.ca_linearLayout_messages, ConstraintSet.BOTTOM, R.id.ca_constraintLayout_main, ConstraintSet.BOTTOM);
            constraintSet.connect(R.id.ca_linearLayout_messages, ConstraintSet.START, R.id.ca_constraintLayout_info, ConstraintSet.END);
            // exit button
            constraintSet.clear(R.id.ca_imageButton_exit, ConstraintSet.START);
            constraintSet.connect(R.id.ca_imageButton_exit, ConstraintSet.END, R.id.ca_constraintLayout_main, ConstraintSet.END);
            constraintSet.setMargin(R.id.ca_imageButton_exit, ConstraintSet.END, Util.dpToPixels(this, 8));
            // snap button
            constraintSet.connect(R.id.ca_imageButton_snap, ConstraintSet.TOP, R.id.ca_constraintLayout_main, ConstraintSet.TOP);
            constraintSet.clear(R.id.ca_imageButton_snap, ConstraintSet.START);
            constraintSet.setMargin(R.id.ca_imageButton_snap, ConstraintSet.BOTTOM, 0);
            constraintSet.setMargin(R.id.ca_imageButton_snap, ConstraintSet.END, Util.dpToPixels(this, 8));
            constraintSet.constrainWidth(R.id.ca_imageButton_snap, Util.dpToPixels(this, SNAP_BUTTON_HEIGHT_DP));
            constraintSet.constrainHeight(R.id.ca_imageButton_snap, Util.dpToPixels(this, SNAP_BUTTON_WIDTH_DP));
            // exposure button
            constraintSet.clear(R.id.ca_imageButton_settingsToggle, ConstraintSet.BOTTOM);
            constraintSet.connect(R.id.ca_imageButton_settingsToggle, ConstraintSet.TOP, R.id.ca_constraintLayout_main, ConstraintSet.TOP);
            constraintSet.setMargin(R.id.ca_imageButton_settingsToggle, ConstraintSet.TOP, Util.dpToPixels(this, 8));
            constraintSet.connect(R.id.ca_imageButton_settingsToggle, ConstraintSet.END, R.id.ca_constraintLayout_main, ConstraintSet.END);
            constraintSet.setMargin(R.id.ca_imageButton_settingsToggle, ConstraintSet.END, Util.dpToPixels(this, 8));
            // exposure settings
            constraintSet.clear(R.id.ca_linearLayout_settings, ConstraintSet.BOTTOM);
            constraintSet.clear(R.id.ca_linearLayout_settings, ConstraintSet.END);
            constraintSet.connect(R.id.ca_linearLayout_settings, ConstraintSet.END, R.id.ca_imageButton_settingsToggle, ConstraintSet.START);
            constraintSet.connect(R.id.ca_linearLayout_settings, ConstraintSet.TOP, R.id.ca_constraintLayout_main, ConstraintSet.TOP);
            constraintSet.setMargin(R.id.ca_linearLayout_settings, ConstraintSet.TOP, Util.dpToPixels(this, 8));
            constraintSet.constrainWidth(R.id.ca_linearLayout_settings, Util.dpToPixels(this, SETTINGS_HEIGHT_DP));
            constraintSet.constrainHeight(R.id.ca_linearLayout_settings, Util.dpToPixels(this, SETTINGS_WIDTH_DP));
            LinearLayout correctionLinearLayout = findViewById(R.id.ca_linearLayout_exposureCorrection);
            correctionLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            VerticalSeekBarAPI26 correctionBar = findViewById(R.id.ca_verticalSeekBarApi26_exposureCorrection);
            correctionBar.setRotation(90);
        }
        constraintSet.applyTo(mainConstraintLayout);
    }

    public void exit(View view) {
        finish();
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (PersistData.getButtonSnapshotActive(this) && event.getKeyCode() == PersistData.getButtonSnapshotKeyCode(this)) {
            ImageButton snapImageButton = findViewById(R.id.ca_imageButton_snap);
            snapImageButton.performClick();
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void toggleCameraSettings(View view) {
        LinearLayout settingsLinearLayout = findViewById(R.id.ca_linearLayout_settings);
        if (settingsLinearLayout.getVisibility() == View.VISIBLE) {
            hideCameraSettings();
        } else {
            showCameraSettings();
        }
    }

    private void hideCameraSettings() {
        LinearLayout settingsLinearLayout = findViewById(R.id.ca_linearLayout_settings);
        if (settingsLinearLayout.getVisibility() == View.GONE) {
            return;
        }
        settingsLinearLayout.setVisibility(View.GONE);
    }

    private void showCameraSettings() {
        LinearLayout settingsLinearLayout = findViewById(R.id.ca_linearLayout_settings);
        if (settingsLinearLayout.getVisibility() == View.VISIBLE) {
            return;
        }
        settingsLinearLayout.setVisibility(View.VISIBLE);
    }

    private void mainClickResolver() {
        hideCameraSettings();
    }
}