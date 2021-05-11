package eu.foxcom.stp.gsa.egnss4cap.model;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eu.foxcom.gnss_scan.NMEAParser;
import eu.foxcom.gnss_scan.NMEAScanner;
import eu.foxcom.gnss_scan.NetworkInfoScanner;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EKFStartExeception;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfController;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfCreateException;
import eu.foxcom.stp.gsa.egnss4cap.model.gnss.NMEAParserApp;

public class PhotoDataController {

    public static abstract class CentroidComputedReceiver {
        public abstract void receive(double latitude, double longitude);
    }

    public static abstract class CentroidSampleAddReceiver {
        public abstract void receive(int count);
    }

    private static final String TAG = PhotoDataController.class.getSimpleName();

    private static final int MAX_DISTANCE_METER = 20;
    private static final int MAX_TIME_DELAY_MILS = 5000;
    private static final int LOCATIONS_INTERVALS_REFRESH_MILS = 2000;
    private static final int MAX_AGE_LOCATION_MILS = 5000;
    public static final double MAX_PORTRAIT_ANGLE = 100;
    public static final double MIN_PORTRAIT_ANGLE = 50;
    public static final double MAX_LANDSCAPE_ANGLE = 120;
    public static final double MIN_LANDSCAPE_ANGLE = 60;


    private Context context;

    private List<Location> locations = new ArrayList<>();
    private Runnable refreshLocationsRunnable;
    private Handler refreshLocationsHandler;
    private PositionSensorController positionSensorController;
    private CameraController cameraController;
    private NetworkInfoScanner networkInfoScanner;
    private JSONObject networkInfoData;
    private NMEAScanner nmeaScanner;
    private NMEAParser nmeaParser;

    private Double centroidLatitude;
    private Double centroidLongitude;
    private Integer extraSatNumber;

    private int lastScreenRotation;
    private double lastTilt;

    private EkfController ekfController;

    public PhotoDataController(Context context) throws EkfCreateException {
        this.context = context;
        this.positionSensorController = new PositionSensorController(context);
        this.ekfController = new EkfController(context);
        ekfController.addDefaultModules();
        this.cameraController = new CameraController(context);
        this.networkInfoScanner = new NetworkInfoScanner(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.nmeaScanner = new NMEAScanner(context);
            this.nmeaParser = new NMEAParserApp(context);
        }

        refreshLocationsHandler = new Handler(Looper.getMainLooper());
        refreshLocationsRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (locations) {
                    for (Iterator<Location> iterator = locations.iterator(); iterator.hasNext(); ) {
                        Location location = iterator.next();
                        if (location.getTime() + MAX_AGE_LOCATION_MILS < DateTime.now().toDate().getTime()) {
                            iterator.remove();
                        }
                    }
                    refreshLocationsHandler.postDelayed(this, LOCATIONS_INTERVALS_REFRESH_MILS);
                }
            }
        };
        refreshLocationsHandler.postDelayed(refreshLocationsRunnable, LOCATIONS_INTERVALS_REFRESH_MILS);
    }

    public void startImmediately() throws EKFStartExeception {
        positionSensorController.start();
        ekfController.start();
    }

    public void startSnapShot() {
        networkInfoScanner.registerReceiver(new NetworkInfoScanner.NetworkInfoReceiver() {
            @Override
            public void receive(NetworkInfoScanner.NetworkInfoHolder networkInfoHolder) {
                try {
                    networkInfoData = networkInfoHolder.toJSONObject();
                } catch (JSONException e) {
                    networkInfoData = null;
                    Log.e(TAG, "Error occured in " + NetworkInfoScanner.NetworkInfoReceiver.class.getSimpleName() + " initialized in " + this.getClass().getEnclosingMethod().getName(), e);
                }
            }
        });


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                networkInfoScanner.scan();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                nmeaScanner.startScan();
                nmeaParser = new NMEAParserApp(context);
                nmeaScanner.registerReceiver(nmeaParser);
            }
        }
    }

    public void startSnapShotFinish(int screenRotation) {
        lastScreenRotation = screenRotation;
        lastTilt = computeTilt(screenRotation);

    }

    @RequiresApi(Build.VERSION_CODES.N)
    public void startCentroid(CentroidComputedReceiver centroidComputedReceiver, CentroidSampleAddReceiver centroidSampleAddReceiver) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        nmeaParser = new NMEAParserApp(context);
        NMEAParser.CentroidComputedReceiver centroidComputedReceiverExtractor = new NMEAParser.CentroidComputedReceiver() {
            @Override
            public void receive(double latitude, double longitude) {
                centroidLatitude = latitude;
                centroidLongitude = longitude;
                centroidComputedReceiver.receive(latitude, longitude);
            }
        };
        nmeaParser.setCentroidComputedReceiver(centroidComputedReceiverExtractor);
        NMEAParser.CentroidSampleAddReceiver centroidSampleAddReceiverExtractor = new NMEAParser.CentroidSampleAddReceiver() {
            @Override
            public void receive(int count) {
                centroidSampleAddReceiver.receive(count);
            }
        };
        nmeaParser.setCentroidSampleAddReceiver(centroidSampleAddReceiverExtractor);
        nmeaScanner.registerReceiver(nmeaParser);
        nmeaScanner.startScan();
    }

    @Deprecated
    public void start() {
        try {
            startImmediately();
        } catch (EKFStartExeception ekfStartExeception) {
        }
        startSnapShot();
    }

    public void stop() {
        positionSensorController.stop();
        networkInfoScanner.unregisterAllReceivers();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nmeaScanner.stopScan();
            nmeaScanner.unregisterAllReceivers();
        }
        ekfController.stop();
        ekfController.release();
    }


    public void addLocation(Location location) {
        synchronized (location) {
            locations.add(location);
            if (location.getExtras() != null) {
                extraSatNumber = location.getExtras().getInt("satellites");
            }
        }
    }

    public Location getLocation() {
        synchronized (locations) {
            int size = locations.size();
            if (size > 0) {
                return locations.get(size - 1);
            }
            return null;
        }
    }

    public boolean isLocationCorrect() {
        synchronized (locations) {
            if (locations.size() < 2) {
                return false;
            }
            double sumDistance = 0;
            for (Location location1 : locations) {
                for (Location location2 : locations) {
                    sumDistance += location1.distanceTo(location2);
                }
            }
            if ((sumDistance / (locations.size() * (locations.size() - 1))) > MAX_DISTANCE_METER) {
                return false;
            }
            int sumTime = 0;
            for (int i = 0; i < locations.size() - 1; ++i) {
                sumTime += locations.get(i + 1).getElapsedRealtimeNanos() - locations.get(i).getElapsedRealtimeNanos();
            }
            return (sumTime / locations.size()) <= MAX_TIME_DELAY_MILS * 1000000L;
        }
    }

    public boolean isAngleCorrect(double tilt, int orientation) {
        double minAngle;
        double maxAngle;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            minAngle = MIN_PORTRAIT_ANGLE;
            maxAngle = MAX_PORTRAIT_ANGLE;
        } else {
            minAngle = MIN_LANDSCAPE_ANGLE;
            maxAngle = MAX_LANDSCAPE_ANGLE;
        }
        return (tilt >= minAngle && tilt <= maxAngle);
    }

    public double computeTilt(int screenRotation) {
        double tilt = 0;
        double roll = positionSensorController.getRollDegreesAverage();
        double pitch = positionSensorController.getPitchDegreesAverage();
        boolean topPortraitFrom = true;
        if (roll >= 180 - 90 && roll <= 180 + 90) {
            topPortraitFrom = false;
        } else {
            topPortraitFrom = true;
        }
        switch (screenRotation) {
            case Surface.ROTATION_0:
                // portrait (270)
                pitch -= 180;
                if (topPortraitFrom) {
                    tilt = 180 - pitch;
                } else {
                    tilt = pitch;
                }
                break;
            case Surface.ROTATION_90:
                // landscape (270)
                roll = 360 - roll;
                tilt = roll;
                break;
            case Surface.ROTATION_180:
                // portrait (90)
                if (topPortraitFrom) {
                    tilt = pitch;
                } else {
                    tilt = 180 - pitch;
                }
                break;
            case Surface.ROTATION_270:
                // landscape (90)
                tilt = roll;
                break;
        }
        return tilt;
    }

    public double computePhotoHeading(int screenRotation, double tilt, double azimuth) {
        double photoHeading = azimuth;
        switch (screenRotation) {
            case Surface.ROTATION_0:
                if (tilt > 90) {
                    photoHeading += 180;
                }
                break;
            case Surface.ROTATION_90:
                photoHeading += 90;
                break;
            case Surface.ROTATION_180:
                if (tilt < 90) {
                    photoHeading += 180;
                }
                break;
            case Surface.ROTATION_270:
                photoHeading -= 90;
                break;
        }
        photoHeading %= 360;
        if (photoHeading < 0) {
            photoHeading += 360;
        }
        return photoHeading;
    }

    public void reset() {
        synchronized (locations) {
            locations.clear();
            nmeaParser = new NMEAParserApp(context);
        }
    }

    // region get, set

    public PositionSensorController getPositionSensorController() {
        return positionSensorController;
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    public void setCameraController(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    public void setPositionSensorController(PositionSensorController positionSensorController) {
        this.positionSensorController = positionSensorController;
    }

    public JSONObject getNetworkInfoData() {
        return networkInfoData;
    }

    public NMEAParser getNmeaParser() {
        return nmeaParser;
    }

    public Double getCentroidLatitude() {
        return centroidLatitude;
    }

    public Double getCentroidLongitude() {
        return centroidLongitude;
    }

    public int getLastScreenRotation() {
        return lastScreenRotation;
    }

    public void setLastScreenRotation(int lastScreenRotation) {
        this.lastScreenRotation = lastScreenRotation;
    }

    public double getLastTilt() {
        return lastTilt;
    }

    public void setLastTilt(double lastTilt) {
        this.lastTilt = lastTilt;
    }

    public Integer getExtraSatNumber() {
        return extraSatNumber;
    }

    public EkfController getEkfController() {
        return ekfController;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */