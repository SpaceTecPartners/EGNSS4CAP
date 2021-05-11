package eu.foxcom.stp.gsa.egnss4cap.model.fusedLocation;

import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.RequiresPermission;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.atomic.AtomicBoolean;

public class FLManager {

    private Context appContext;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Looper looper;
    private FLDelegateActivity flDelegateActivity;
    private FLLocationSource flLocationSource;
    /**
     * used to move the camera to the nearest future position once
     */
    private boolean isCameraMoveRequested = false;
    private int cameraZoom = 16;
    private int cameraAnimateDurationMils = 700;

    private int interval = 1000;
    private int fastestInterval= 500;
    private int priority = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private Observer<Boolean> isRunningObserver;

    private MutableLiveData<LocationResult> lastLocationResult = new MutableLiveData<>(null);
    private Observer<LocationResult> lastLocationResultObserver;

    public FLManager(Context appContext) {
        this.appContext = appContext;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext);
        setRequest(interval, fastestInterval, priority);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                newLocationResult(locationResult);
            }
        };

        flLocationSource = new FLLocationSource(this);
    }

    public void setRequest(int interval, int fastestInterval, int priority) {
        if (isRunning.getValue()) {
            return;
        }
        this.interval = interval;
        this.fastestInterval = fastestInterval;
        this.priority = priority;
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(fastestInterval);
        locationRequest.setPriority(priority);
    }

    public void setFlDelegateActivity(FLDelegateActivity flDelegateActivity) {
        if (isRunning.getValue()) {
            return;
        }

        if (lastLocationResultObserver != null) {
            lastLocationResult.removeObserver(lastLocationResultObserver);
        }
        if (isRunningObserver != null) {
            isRunning.removeObserver(isRunningObserver);
        }

        this.flDelegateActivity = flDelegateActivity;
        if (flDelegateActivity == null) {
            return;
        }

        AtomicBoolean lastLocationIsFirst = new AtomicBoolean(true);
        lastLocationResultObserver = locationResult -> {
            if (lastLocationIsFirst.get()) {
                lastLocationIsFirst.set(false);
                return;
            }
            flDelegateActivity.onNewFusedLocations(locationResult);
            if (isCameraMoveRequested && locationResult != null && locationResult.getLastLocation() != null) {
                isCameraMoveRequested = false;
                moveCameraToLocation(locationResult.getLastLocation());
            }
        };

        lastLocationResult.observe(flDelegateActivity.getAppCompatActivity(), lastLocationResultObserver);
        AtomicBoolean isRunningIsFirst = new AtomicBoolean(true);
        isRunningObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (isRunningIsFirst.get()) {
                    isRunningIsFirst.set(false);
                    return;
                }
                if (aBoolean){
                    flDelegateActivity.onFLStarted();
                } else {
                    flDelegateActivity.onFLEnded();
                }
            }
        };

        isRunning.observe(flDelegateActivity.getAppCompatActivity(), isRunningObserver);
    }

    private void moveCameraToLocation(Location location) {
        if (location == null) {
            return;
        }

        GoogleMap map = flDelegateActivity.getGoogleMap();
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(cameraZoom)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), cameraAnimateDurationMils, null);
    }

    @RequiresPermission(
            anyOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}
    )
    public void start() {
        if (isRunning.getValue()) {
            return;
        }
        if (looper == null) {
            looper = appContext.getMainLooper();
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, looper);
        isRunning.setValue(true);
    }

    public void stop() {
        if (!isRunning.getValue()) {
            return;
        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        isRunning.setValue(false);
    }

    public LocationResult getLastLocationResult() {
        return lastLocationResult.getValue();
    }

    public Location getLastLocation() {
        if (lastLocationResult.getValue() == null) {
            return null;
        }
        return lastLocationResult.getValue().getLastLocation();
    }

    private void newLocationResult(LocationResult locationResult) {
        lastLocationResult.setValue(locationResult);
        flLocationSource.onNewLocation(locationResult.getLastLocation());
    }

    public boolean isRunning() {
        return isRunning.getValue();
    }

    public void requestCameraMoveToNewLocation() {
        isCameraMoveRequested = true;
    }

    // region get, set

    public Looper getLooper() {
        return looper;
    }

    public void setLooper(Looper looper) {
        this.looper = looper;
    }

    public int getInterval() {
        return interval;
    }

    public int getFastestInterval() {
        return fastestInterval;
    }

    public int getPriority() {
        return priority;
    }

    public FLLocationSource getFlLocationSource() {
        return flLocationSource;
    }

    public void setCameraZoom(int cameraZoom) {
        this.cameraZoom = cameraZoom;
    }

    public void setCameraAnimateDurationMils(int cameraAnimateDurationMils) {
        this.cameraAnimateDurationMils = cameraAnimateDurationMils;
    }

    // endregion

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
