package eu.foxcom.stp.gsa.egnss4cap.model.pathTrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import org.joda.time.DateTime;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.foxcom.stp.gsa.egnss4cap.R;
import eu.foxcom.stp.gsa.egnss4cap.ServiceController;
import eu.foxcom.stp.gsa.egnss4cap.ServiceInit;
import eu.foxcom.stp.gsa.egnss4cap.model.AppDatabase;
import eu.foxcom.stp.gsa.egnss4cap.model.FileLogger;
import eu.foxcom.stp.gsa.egnss4cap.model.PersistData;
import eu.foxcom.stp.gsa.egnss4cap.model.convexHullMap.CHService;

/**
 * P1 = centroid travel service canceled indefinitely
 */

public class PTService extends LifecycleService implements ServiceInit {

    class LocalBinder extends Binder {
        public PTService getService() {
            return PTService.this;
        }
    }

    public interface IsTrackingDisposable {
        void onIsTrackingDisposable(boolean isTracking);
    }

    private static final int LOCATION_REQUEST_MILS = 800;
    private static final int PERIOD_DELTA_MILS = 200;
    private static final int UNFILTERED_LOCATION_LIFETIME_MILS = 5000;

    private FileLogger fileLogger;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    MutableLiveData<Boolean> isTracking = new MutableLiveData<>(false);
    MutableLiveData<Boolean> isPause = new MutableLiveData<>(false);
    MutableLiveData<Exception> lastStartException = new MutableLiveData<>(null);
    MutableLiveData<Exception> lastStopException = new MutableLiveData<>(null);
    MutableLiveData<Exception> noPointException = new MutableLiveData<>(null);
    // if is true then force add one point in pause state
    private boolean isTrackingStarting = false;
    private Queue<IsTrackingDisposable> isTrackingDisposableQueue = new ArrayDeque<>();
    private ToneGenerator toneGenerator;
    private DateTime lastPointReceived;

    PTPath currentPath;
    MutableLiveData<PTPoint> lastPoint = new MutableLiveData<>(null);
    MutableLiveData<Location> lastLocation = new MutableLiveData<>(null);
    MutableLiveData<Location> lastUnfilteredLocation = new MutableLiveData<>(null);
    AppDatabase appDatabase;

    private boolean withCentroid = false;
    private ServiceController chServiceController;
    private CHService chService;
    private Observer<CHService.Centroid> centroidObserver;

    public final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        fileLogger = new FileLogger(getApplicationContext(), this.getClass());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_REQUEST_MILS);
        locationRequest.setFastestInterval(LOCATION_REQUEST_MILS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                addIsTrackingDisposable(isTracking -> {
                    if (!isTracking) {
                        return;
                    }
                    Location location = locationResult.getLastLocation();
                    receiveNewLocation(location);
                });
            }
        };

        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        /* P1
        if (PersistData.getPhotoWithCentroiLocation(this)) {
            chServiceController = new ServiceController(this, new ServiceGetter() {
                @Override
                public Service getService(IBinder binder) {
                    return ((CHService.LocalBinder) binder).getService();
                }
            }, CHService.class);
            chServiceController.setServiceInit(this);
            chServiceController.startService();
        }
        /**/
    }

    @Override
    public void serviceInit() {
        chService = (CHService) chServiceController.getService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void startTracking(String name, AppDatabase appDatabase) {
        addIsTrackingDisposable((isTracked) -> {
            if (isTracking.getValue()) {
                return;
            }

            if (isTrackingStarting) {
                return;
            } else {
                isTrackingStarting = true;
            }

            lastStartException.setValue(null);
            this.appDatabase = appDatabase;

            isPause.setValue(false);

            /* P1 */
            withCentroid = PersistData.getPhotoWithCentroiLocation(this);
            withCentroid = false;
            /**/

            if (withCentroid) {
                startTrackingWithCentroid(name);
            } else {
                startTrackingWithFusedLocation(name);
            }
        });
    }

    public void stopTracking() {
        addIsTrackingDisposable(isTracked -> {
            if (!isTracking.getValue()) {
                return;
            }

            lastStopException.setValue(null);

            if (withCentroid) {
                stopTrackingWithCentroid();
            } else {
                stopTrackingWithFusedLocation();
            }
        });
    }

    private void startTrackingComplete(boolean success, String name) {
        isTrackingStarting = false;
        if (success) {
            currentPath = PTPath.createNew(appDatabase);
            currentPath.setName(name);
            currentPath.setStartT(DateTime.now());
            currentPath.setByCentroids(withCentroid);
            isTracking.setValue(true);
        }
        executeIsTrackingDisposable();
    }

    private void stopTrackingComplete() {
        if (currentPath.getPoints().size() > 0) {
            currentPath.setEndT(DateTime.now());
            currentPath.setDeviceInfos();
            currentPath.calculateArea();
            currentPath.saveToDB(appDatabase);
        } else {
            currentPath = null;
            noPointException.setValue(new IllegalStateException("No points in path."));
        }

        isTracking.setValue(false);
    }

    private void startTrackingWithFusedLocation(String name) {
        if (ActivityCompat.checkSelfPermission(PTService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            isTrackingStarting = false;
            lastStartException.setValue(new SecurityException("Insufficient permissions"));
            return;
        }
        Task task = fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        AtomicBoolean success = new AtomicBoolean(false);
        task.addOnSuccessListener(o -> {
            success.set(true);
        });
        task.addOnFailureListener(e -> {
            success.set(false);
            fileLogger.logException(e);
            lastStartException.setValue(e);
        });
        task.addOnCompleteListener(task1 -> {
                /* DEBUGCOM
                success.set(false);
                Exception e = new Exception("Pokus chyba");
                fileLogger.logException(e);
                lastStartException.setValue(e);
                /**/

            startTrackingComplete(success.get(), name);
        });
    }

    private void stopTrackingWithFusedLocation() {
        Task task = fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        task.addOnFailureListener(e -> {
            fileLogger.logException(e);
            lastStopException.setValue(e);
        });
        task.addOnCompleteListener(task1 -> {
                /* DEBUGCOM
                Exception e = new Exception("Pokus chyba");
                fileLogger.logException(e);
                lastStopException.setValue(e);
                /**/

            stopTrackingComplete();
        });
    }

    private void startTrackingWithCentroid(String name) {
        if (!chServiceController.isServiceInitialized()) {
            return;
        }
        centroidObserver = centroid -> addIsTrackingDisposable(isTracking -> {
            if (!isTracking) {
                return;
            }
            if (centroid == null) {
                return;
            }
            Location location = new Location("location");
            location.setLatitude(centroid.latitude);
            location.setLongitude(centroid.longitude);
            location.setTime(DateTime.now().toDate().getTime());
            receiveNewLocation(location);
        });

        boolean success;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            success = true;
            chService.getLastCentroid().observe(this, centroidObserver);
        } else {
            success = false;
            Exception e = new Exception(getString(R.string.pt_androidVersionException));
            fileLogger.logException(e);
            lastStartException.setValue(e);
        }

        startTrackingComplete(success, name);
    }

    private void stopTrackingWithCentroid() {
        if (!chServiceController.isServiceInitialized()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            chService.getLastCentroid().removeObserver(centroidObserver);
        }

        stopTrackingComplete();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();
    }

    private void executeIsTrackingDisposable() {
        while (true) {
            if (!isDisposable()) {
                return;
            }
            IsTrackingDisposable isTrackingDisposable = isTrackingDisposableQueue.poll();
            if (isTrackingDisposable == null) {
                return;
            }
            isTrackingDisposable.onIsTrackingDisposable(isTracking.getValue());
        }

    }

    public void addIsTrackingDisposable(IsTrackingDisposable isTrackingDisposable) {
        isTrackingDisposableQueue.add(isTrackingDisposable);
        executeIsTrackingDisposable();
    }

    private boolean isDisposable() {
        return !isTrackingStarting;
    }

    private void receiveNewLocation(Location location) {
        if (location == null) {
            return;
        }
        lastUnfilteredLocation.setValue(location);
        if (isPause.getValue()) {
            return;
        }
        if (lastPointReceived != null
                && DateTime.now().toDate().getTime() - lastPointReceived.toDate().getTime() < PersistData.getCapturePointPeriod(this) * 1000 - PERIOD_DELTA_MILS) {
            return;
        }

        addNewLocation(location);
    }

    private void addNewLocation(Location location) {
        if (PersistData.getBeepPathPoint(this)) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
        lastLocation.setValue(location);
        currentPath.addPoint(location);
        lastPoint.setValue(currentPath.getPoints().get(currentPath.getPoints().size() - 1));
        lastPointReceived = DateTime.now();
    }

    boolean forceAddOnePoint() {
        if (lastUnfilteredLocation == null) {
            return false;
        }
        if (DateTime.now().toDate().getTime() - lastUnfilteredLocation.getValue().getTime() > UNFILTERED_LOCATION_LIFETIME_MILS) {
            return false;
        }
        addNewLocation(lastUnfilteredLocation.getValue());
        return true;
    }

    boolean deletePoint(PTPoint point) {
        if (!isTracking.getValue() || currentPath == null) {
            return false;
        }
        if (currentPath.getPoints().size() == 0) {
            return false;
        }
        int indx = currentPath.getPoints().indexOf(point);
        if (indx == -1) {
            return false;
        }

        currentPath.getPoints().remove(point);
        return true;
    }

    boolean addLocation(Location location) {
        if (!isTracking.getValue()) {
            return false;
        }

        addNewLocation(location);
        return true;
    }

    // region get, set

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */