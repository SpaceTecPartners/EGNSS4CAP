package eu.foxcom.gtphotos.model.pathTrack;

import android.app.Service;
import android.content.Context;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.foxcom.gtphotos.ServiceController;
import eu.foxcom.gtphotos.ServiceGetter;
import eu.foxcom.gtphotos.ServiceInit;
import eu.foxcom.gtphotos.model.AppDatabase;

public class PTManager implements ServiceInit {

    private Context context;
    PTMapActivity ptMapActivity;
    private ServiceController serviceController;
    private PTService ptService;
    private PTDrawer ptDrawer;
    private AppDatabase appDatabase;
    private List<PTService.IsTrackingDisposable> isTrackingDisposablesPending = new ArrayList<>();
    // true (shown -> start), false (start -> shown)
    private boolean isFirstPointKnown = false;
    private boolean isDestroying = false;

    public PTManager(PTMapActivity ptMapActivity, AppDatabase appDatabase) {
        this.context = (Context) ptMapActivity;
        this.ptMapActivity = ptMapActivity;
        this.ptDrawer = new PTDrawer(this);
        this.appDatabase = appDatabase;

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
                if (ptDrawer.getCurrentPtPath() != null &&
                        PTPath.createFromAppDatabase(appDatabase, ptDrawer.getCurrentPtPath().getAutoId()) == null) {
                    ptDrawer.removePath();
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
                if(aBoolean) {
                    ptDrawer.drawAllPathAsPolyline(ptService.currentPath);
                    ptMapActivity.onStartedPT();
                } else {
                    ptMapActivity.onStoppedPT(ptService.currentPath);
                }
            });
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

    // region get, set


    // endregion

}
