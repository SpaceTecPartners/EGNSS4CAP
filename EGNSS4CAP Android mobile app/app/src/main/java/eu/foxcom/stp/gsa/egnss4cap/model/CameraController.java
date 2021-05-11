package eu.foxcom.stp.gsa.egnss4cap.model;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.SizeF;

public class CameraController {

    private Context context;
    private CameraManager cameraManager;

    public CameraController(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    private SizeF getCameraResolution(int camNum) throws CameraAccessException {
        SizeF size = new SizeF(0, 0);
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIds = manager.getCameraIdList();
        if (cameraIds.length > camNum) {
            CameraCharacteristics character = manager.getCameraCharacteristics(cameraIds[camNum]);
            size = character.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        }
        return size;
    }

    public float[] calculateFOV() {
        float[] FOV = new float[2];
        try {
            for (final String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    float[] maxFocus = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    SizeF size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    float w = size.getWidth();
                    float h = size.getHeight();
                    float horizontalAngle = (float) (2*Math.atan(w/(maxFocus[0]*2)));
                    float verticalAngle = (float) (2*Math.atan(h/(maxFocus[0]*2)));
                    FOV[0] = horizontalAngle;
                    FOV[1] = verticalAngle;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return FOV;
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
