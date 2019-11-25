package com.ezartech.ezar.videooverlay;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.view.OrientationEventListener;
import android.view.Surface;

import java.util.List;

//from https://android.googlesource.com/platform/packages/apps/Camera/+/jb-release/src/com/android/camera/Util.java
/**
 * Created by Zirk on 7/1/2016.
 */
public class Util {

    private static final String TAG = "Util";

    // Orientation hysteresis amount used in rounding, in degrees
    public static final int ORIENTATION_HYSTERESIS = 5;

    private Util() {
    }

    public static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    public static int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
		
		int	rotationToRet = 0;
        switch (rotation) {
            case Surface.ROTATION_0: {
				rotationToRet = 0;
				break;
			}
            case Surface.ROTATION_90: {
				rotationToRet = 90;
				break;
			}
            case Surface.ROTATION_180:{
				rotationToRet = 180;
				break;
			}
            case Surface.ROTATION_270:{
				rotationToRet = 270;
				break;
			}
        }
        return rotationToRet;
    }

    public static int getDisplayOrientation(int degrees, int cameraId) {
        // See android.hardware.Camera.setDisplayOrientation for
        // documentation.
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static int getCameraOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        return info.orientation;
    }

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min( dist, 360 - dist );
            changeOrientation = ( dist >= 45 + ORIENTATION_HYSTERESIS );
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                                     int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

}
