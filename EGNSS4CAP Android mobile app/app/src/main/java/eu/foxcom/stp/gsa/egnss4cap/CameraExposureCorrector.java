package eu.foxcom.stp.gsa.egnss4cap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;

import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.Camera;

import java.util.List;

import eu.foxcom.stp.gsa.egnss4cap.model.PersistData;

public class CameraExposureCorrector {

    private Context context;

    private boolean isAvailable = false;
    private boolean isInitilized = false;
    private int lowerLimit = 0;
    private int upperLimit = 0;

    public CameraExposureCorrector(Context context) {
        this.context = context;
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void init(Camera camera) {
        String cameraId = Camera2CameraInfo.extractCameraId(camera.getCameraInfo());
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            isAvailable = false;
            isInitilized = true;
            return;
        }
        List<CaptureRequest.Key<?>> requestKeys = cameraCharacteristics.getAvailableCaptureRequestKeys();
        for (CaptureRequest.Key<?> key : requestKeys) {
            if (key.equals(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)) {
                Range<Integer> range = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                if (range.getLower() != 0 && range.getUpper() != 0) {
                    lowerLimit = range.getLower();
                    upperLimit = range.getUpper();
                    isAvailable = true;
                    /* DEBUGCOM
                    isAvailable = false;
                    /**/
                } else {
                    isAvailable = false;
                }
                isInitilized = true;
                return;
            }
        }
        isAvailable = false;
        isInitilized = true;
    }

    public void setCorrection(int correction) {
        PersistData.saveExposureCorrection(context, correction);
    }

    public int getCorrection() {
        return PersistData.getExposureCorrection(context);
    }

    public int scaleToPercent(int correction) {
        if (correction < lowerLimit) {
            correction = lowerLimit;
        }
        if (correction > upperLimit) {
            correction = upperLimit;
        }
        return (int) Math.round((correction - lowerLimit) * (100.0 / (upperLimit - lowerLimit)));
    }

    public int scaleToExtendedPercent(int correction) {
        return scaleToPercent(correction) * 2 - 100;
    }

    public int scaleToRealValue(int percent) {
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        return (int) Math.round(percent * ((upperLimit - lowerLimit) / 100.0) + lowerLimit);
    }

    // region get, set

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean isInitilized() {
        return isInitilized;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */