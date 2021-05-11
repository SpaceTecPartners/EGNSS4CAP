package eu.foxcom.stp.gsa.egnss4cap.model.pathTrack;

import android.app.Service;
import android.content.Context;
import android.location.Location;
import android.os.IBinder;

import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.foxcom.stp.gsa.egnss4cap.ServiceController;
import eu.foxcom.stp.gsa.egnss4cap.ServiceGetter;
import eu.foxcom.stp.gsa.egnss4cap.ServiceInit;
import eu.foxcom.stp.gsa.egnss4cap.model.AppDatabase;

public class PTManager implements ServiceInit {

    private Context context;
    PTMapActivity ptMapActivity;
    PTOnNoPointToAdd ptOnNoPointToAdd;
    PTOnNoPointToDelete ptOnNoPointToDelete;

    private ServiceController serviceController;
    private PTService ptService;
    private PTDrawer ptDrawer;
    private AppDatabase appDatabase;
    private List<PTService.IsTrackingDisposable> isTrackingDisposablesPending = new ArrayList<>();
    // true (shown -> start), false (start -> shown)
    private boolean isFirstPointKnown = false;
    private boolean isDestroying = false;
    private PTIsPathsUploadingBinder ptIsPathsUploadingBinder;

    public PTManager(PTMapActivity ptMapActivity, AppDatabase appDatabase) {
        this.context = (Context) ptMapActivity;
        this.ptMapActivity = ptMapActivity;
        this.ptDrawer = new PTDrawer(this);
        this.appDatabase = appDatabase;
        this.ptIsPathsUploadingBinder = ptMapActivity.getPtIsPathsUploadingBinder();

        if (PTOnNoPointToAdd.class.isInstance(ptMapActivity)) {
            this.ptOnNoPointToAdd = (PTOnNoPointToAdd) ptMapActivity;
        }
        if (PTOnNoPointToDelete.class.isInstance(ptMapActivity)) {
            this.ptOnNoPointToDelete = (PTOnNoPointToDelete) ptMapActivity;
        }

        serviceController = new ServiceController(context, new ServiceGetter() {
            @Override
            public Service getService(IBinder binder) {
                return ((PTService.LocalBinder) binder).getService();
            }
        }, PTService.class);
        serviceController.setServiceInit(this);
        serviceController.startService();
    }

    public void onResume() {
        isTrackingDisposable(isTracking -> {
            if (isTracking) {
                ptDrawer.removePath();
                if (ptService != null) {
                    ptDrawer.drawAllPathAsPolyline(ptService.currentPath);
                }
            } else {
                if (!ptDrawer.reloadCurrentPathData(appDatabase)) {
                    ptDrawer.removePath();
                } else {
                    ptDrawer.drawCurrentPathInfo();
                }
            }
        });
    }

    @Override
    public void serviceInit() {
        ptService = (PTService) serviceController.getService();
        if (isDestroying) {
            disconnectFromPTService();
            return;
        }

        ptService.isTracking.observe(ptMapActivity.getAppCompatActivity(), aBoolean -> {
            ptService.addIsTrackingDisposable(isTracking -> {
                ptDrawer.removePath();
                if (aBoolean) {
                    ptDrawer.drawAllPathAsPolyline(ptService.currentPath);
                    ptMapActivity.onStartedPT();
                } else {
                    ptMapActivity.onStoppedPT(ptService.currentPath);
                }
            });
        });
        ptService.isPause.observe(ptMapActivity.getAppCompatActivity(), isPause -> {
            ptDrawer.drawPathInfoMode(isPause);
            if (isPause) {
                ptMapActivity.onPausePT();
            } else {
                ptMapActivity.onContinuePT();
            }
        });
        AtomicBoolean isLStartEFirstRun = new AtomicBoolean(true);
        ptService.lastStartException.observe(ptMapActivity.getAppCompatActivity(), e -> {
            if (isLStartEFirstRun.get()) {
                isLStartEFirstRun.set(false);
                return;
            }
            if (e == null) {
                return;
            }
            ptMapActivity.startingPTException(e);
        });
        AtomicBoolean isLStopEFirstRun = new AtomicBoolean(true);
        ptService.lastStopException.observe(ptMapActivity.getAppCompatActivity(), e -> {
            if (isLStopEFirstRun.get()) {
                isLStopEFirstRun.set(false);
                return;
            }
            if (e == null) {
                return;
            }
            ptMapActivity.stoppingPTException(e);
        });
        AtomicBoolean isLastPointFirstRun = new AtomicBoolean(true);
        ptService.lastPoint.observe(ptMapActivity.getAppCompatActivity(), ptPoint -> {
            if (isLastPointFirstRun.get()) {
                isLastPointFirstRun.set(false);
                return;
            }
            if (ptPoint == null) {
                return;
            }
            ptDrawer.addPointToPath(ptPoint);
        });
        AtomicBoolean isNoPointExcFirstRun = new AtomicBoolean(true);
        ptService.noPointException.observe(ptMapActivity.getAppCompatActivity(), e -> {
            if (isNoPointExcFirstRun.get()) {
                isNoPointExcFirstRun.set(false);
                return;
            }
            if (e == null) {
                return;
            }
            ptMapActivity.onNoPointsInPath();
        });
        ptService.lastLocation.observe(ptMapActivity.getAppCompatActivity(), location -> {
            if (isFirstPointKnown) {
                return;
            }
            ptDrawer.moveToLocation(location);
            isFirstPointKnown = true;
        });
        AtomicBoolean isLastUnfilteredLocationFirstRun = new AtomicBoolean(true);
        ptService.lastUnfilteredLocation.observe(ptMapActivity.getAppCompatActivity(), location -> {
            if (isLastUnfilteredLocationFirstRun.get()) {
                isLastUnfilteredLocationFirstRun.set(false);
                return;
            }
            ptMapActivity.onNewUnfilteredLocationPT(location);
        });

        executeIsTrackingDisposablePending();
    }

    public void startTracking(String name) {
        if (ptService == null) {
            return;
        }
        isFirstPointKnown = false;
        ptService.startTracking(name, appDatabase);
    }

    public void isTrackingDisposable(PTService.IsTrackingDisposable isTrackingDisposable) {
        if (ptService != null) {
            ptService.addIsTrackingDisposable(isTrackingDisposable);
        } else {
            isTrackingDisposablesPending.add(isTrackingDisposable);
        }
    }

    private void executeIsTrackingDisposablePending() {
        for (PTService.IsTrackingDisposable isTrackingDisposable : isTrackingDisposablesPending) {
            ptService.addIsTrackingDisposable(isTrackingDisposable);
        }
    }

    public void stopTracking() {
        if (ptService == null) {
            return;
        }
        ptService.stopTracking();
    }

    public void drawPathPolygon(PTPath ptPath) {
        ptDrawer.removePath();
        ptDrawer.drawAllPathAsPolygon(ptPath);
        ptDrawer.centralizeToPath(ptPath);
    }

    public void onDestroy() {
        disconnectFromPTService();
        isDestroying = true;
    }

    private void disconnectFromPTService() {
        if (ptService != null) {
            ptService.addIsTrackingDisposable(isTracking -> {
                if (isTracking) {
                    serviceController.unbindService();
                } else {
                    serviceController.stopService();
                }
            });
        }
    }

    public void uploadDrawnPath() {
        if (!serviceController.isServiceInitialized()) {
            return;
        }
        ptService.addIsTrackingDisposable(isTracking -> {
            if (isTracking) {
                return;
            }
            PTPath ptPath = ptDrawer.getCurrentPtPath();
            if (ptPath == null) {
                return;
            }
            if (ptPath.isSent()) {
                return;
            }
            if (ptIsPathsUploadingBinder == null) {
                return;
            }
            if (ptIsPathsUploadingBinder.isPathsUploading()) {
                return;
            } else {
                ptIsPathsUploadingBinder.setPathsUploading(true);
            }
            ptMapActivity.uploadDrawnPathStarted();
            ptPath.upload(appDatabase, ptMapActivity.getRequestor(), new PTPath.UploadReceiver() {
                @Override
                protected void success() {
                    ptMapActivity.uploadDrawnPathSuccess();
                }

                @Override
                protected void failed(String errMsg) {
                    ptMapActivity.uploadDrawnPathFailed(errMsg);
                }

                @Override
                protected void complete() {
                    ptIsPathsUploadingBinder.setPathsUploading(false);
                    if (!ptMapActivity.getLifecycle().getCurrentState().equals(Lifecycle.State.DESTROYED)) {
                        // may not be original
                        ptDrawer.drawCurrentPathInfo();
                    }
                    ptMapActivity.uploadDrawnPathComplete();
                }
            });
        });
    }

    public PTPath getDrawnPath() {
        return ptDrawer.getCurrentPtPath();
    }

    public void setPause(boolean isPause) {
        if (!serviceController.isServiceInitialized()) {
            return;
        }
        ptService.isPause.setValue(isPause);
    }

    public Boolean isPause() {
        if (!serviceController.isServiceInitialized()) {
            return null;
        }
        return ptService.isPause.getValue();
    }

    public void forceAddCurrentPoint() {
        if (ptService == null) {
            return;
        }

        if (!ptService.forceAddOnePoint() && ptOnNoPointToAdd != null) {
            ptOnNoPointToAdd.ptOnNoPointToAdd();
        }
    }

    public void addLocationManually(Location location) {
        if (ptService == null) {
            return;
        }

        ptService.addLocation(location);
    }

    public void deletePoint(PTPoint ptPoint) {
        if (ptService == null || ptService.currentPath == null) {
            return;
        }
        if (!ptService.deletePoint(ptPoint)) {
            if (ptService.currentPath.getPoints().size() == 0 && ptOnNoPointToDelete != null) {
                ptOnNoPointToDelete.ptOnNoPointToDelete();
            }
            return;
        }

        ptDrawer.removePath();
        ptDrawer.drawAllPathAsPolyline(ptService.currentPath);
    }

    public PTPath getCurrentPath() {
        if (ptService == null) {
            return null;
        }
        return ptService.currentPath;
    }

    // region get, set


    // endregion

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
