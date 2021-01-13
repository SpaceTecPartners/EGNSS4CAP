package eu.foxcom.gtphotos.model;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.LinkedList;
import java.util.Queue;

public class PositionSensorController implements SensorEventListener {

    public static final int CACHE_MAX_COUNT = 50;

    public abstract class Receiver {
        public abstract void receive();
    }

    private Context context;

    private Receiver receiver;

    private SensorManager sensorManager;
    Sensor accelerometer;
    Sensor magneticField;
    Sensor rotationVector;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] rotationMatrixFused = new float[9];
    private final float[] orientationAnglesFused = new float[3];

    private final Queue<float[]> cacheRotationReadings = new LinkedList<>();
    float[] avgRotationMatrixFused = new float[9];
    private final float[] avgAnglesFused = new float[3];

    private final Queue<float[]> cacheAccelerometerReadings = new LinkedList<>();
    private final Queue<float[]> cacheMagnetometerReadings = new LinkedList<>();
    float[] avgRotationMatrix = new float[9];
    private final float[] avgAngles = new float[3];


    public PositionSensorController(Context context) {
        this.context = context;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public boolean isAvailable() {
        return accelerometer != null && magneticField != null;
    }

    public void start() {
        if (!isAvailable()) {
            return;
        }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void updateOrientationAngles() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
    }

    public void updateOrientationAnglesFused() {
        SensorManager.getRotationMatrixFromVector(rotationMatrixFused, rotationReading);
        SensorManager.getOrientation(rotationMatrixFused, orientationAnglesFused);
    }

    public void updateOrientationAnglesAverage() {
        float[] averageAccelerometer = new float[3];
        float[] averageMagnetometer = new float[3];
        int countAccelerometer = cacheAccelerometerReadings.size();
        int countMagnetometer = cacheMagnetometerReadings.size();
        for (float[] acc : cacheAccelerometerReadings) {
            averageAccelerometer[0] += acc[0];
            averageAccelerometer[1] += acc[1];
            averageAccelerometer[2] += acc[2];
        }
        averageAccelerometer[0] = averageAccelerometer[0] / countAccelerometer;
        averageAccelerometer[1] = averageAccelerometer[1] / countAccelerometer;
        averageAccelerometer[2] = averageAccelerometer[2] / countAccelerometer;
        for (float[] mag : cacheMagnetometerReadings) {
            averageMagnetometer[0] += mag[0];
            averageMagnetometer[1] += mag[1];
            averageMagnetometer[2] += mag[2];
        }
        averageMagnetometer[0] += averageMagnetometer[0] / countMagnetometer;
        averageMagnetometer[1] += averageMagnetometer[1] / countMagnetometer;
        averageMagnetometer[2] += averageMagnetometer[2] / countMagnetometer;
        SensorManager.getRotationMatrix(avgRotationMatrix, null, averageAccelerometer, averageMagnetometer);
        SensorManager.getOrientation(avgRotationMatrix, avgAngles);
    }

    public void updateOrientationAnglesFusedAverage() {
        float[] averageRotation = new float[3];
        float sum0 = 0;
        float sum1 = 0;
        float sum2 = 0;
        int count = cacheRotationReadings.size();
        for (float[] rotation : cacheRotationReadings) {
            sum0 += rotation[0];
            sum1 += rotation[1];
            sum2 += rotation[2];
        }
        averageRotation[0] = sum0 / count;
        averageRotation[1] = sum1 / count;
        averageRotation[2] = sum2 / count;
        SensorManager.getRotationMatrixFromVector(avgRotationMatrixFused, averageRotation);
        SensorManager.getOrientation(avgRotationMatrixFused, avgAnglesFused);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
                addCacheAcceleration();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
                addCacheMagnetic();
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                System.arraycopy(event.values, 0, rotationReading, 0, rotationReading.length);
                addCacheRotation();
                break;
        }
        if (receiver != null) {
            receiver.receive();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing
    }

    private void addCacheRotation() {
        if (cacheRotationReadings.size() == CACHE_MAX_COUNT) {
            cacheRotationReadings.remove();
        }
        float[] rotationReadingCopy = new float[3];
        System.arraycopy(rotationReading, 0, rotationReadingCopy, 0, rotationReadingCopy.length);
        cacheRotationReadings.add(rotationReadingCopy);
    }

    private void addCacheAcceleration() {
        if (cacheAccelerometerReadings.size() == CACHE_MAX_COUNT) {
            cacheAccelerometerReadings.remove();
        }
        float[] accCopy = new float[3];
        System.arraycopy(accelerometerReading, 0, accCopy, 0, accCopy.length);
        cacheAccelerometerReadings.add(accCopy);
        float[] magCopy = new float[3];
    }

    private void addCacheMagnetic() {
        if (cacheMagnetometerReadings.size() == CACHE_MAX_COUNT) {
            cacheMagnetometerReadings.remove();
        }
        float[] magCopy = new float[3];
        System.arraycopy(magnetometerReading, 0, magCopy, 0, magCopy.length);
        cacheMagnetometerReadings.add(magCopy);
    }

    private double getPositiveDegrees(float value) {
        double degree = Math.toDegrees(value);
        if (degree < 0) {
            degree += 360;
        }
        return degree;
    }

    // region get, set

    public double getAzimuthDegrees() {
        return getPositiveDegrees(orientationAngles[0]);
    }

    public double getAzimuthDegreesFused() {
        return getPositiveDegrees(orientationAnglesFused[0]);
    }

    public double getAzimuthDegreesFusedAverage() {
        return getPositiveDegrees(avgAnglesFused[0]);
    }

    public double getAzimuthDegreesAverage() {
        return getPositiveDegrees(avgAngles[0]);
    }

    public double getPitchDegreesAverage() {
        return getPositiveDegrees(avgAngles[1]);
    }

    public double getRollDegreesAverage() {
        return getPositiveDegrees(avgAngles[2]);
    }

    public boolean isLandscape() {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public float[] getOrientationAngles() {
        return orientationAngles;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    // endregion
}
