package eu.foxcom.gtphotos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import eu.foxcom.gtphotos.model.AppDatabase;
import eu.foxcom.gtphotos.model.LoggedUser;
import eu.foxcom.gtphotos.model.Requestor;
import eu.foxcom.gtphotos.model.SyncQueue;
import eu.foxcom.gtphotos.model.Task;
import eu.foxcom.gtphotos.model.TaskList;
import eu.foxcom.gtphotos.model.Util;
import eu.foxcom.gtphotos.model.pathTrack.PTPath;

public class MainService extends Service {

    public interface LocationReceiver {
        public void receive(Location location);
    }

    public enum BROADCAST_MSG {
        BROADCAST_ID("mainServiceBroadcast"),
        TYPE("brodcastId"),
        STARTED("started"),
        REFRESH_TASKS_STARTED("refreshTasksStarted"),
        REFRESH_TASKS("refreshTasks"),
        UPLOAD_TASK_STATUS("uploadTaskStatus"),
        REFRESH_PHOTOS("refreshPhoto");

        public static BROADCAST_MSG createFromID(String id) {
            for (BROADCAST_MSG broadcast_msg : BROADCAST_MSG.values()) {
                if (broadcast_msg.ID.equals(id)) {
                    return broadcast_msg;
                }
            }
            return null;
        }

        public final String ID;

        BROADCAST_MSG(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_REFRESH_TASKS_PARAMS {
        SUCCESS("success"),
        ERROR_MSG("errMsg");

        public final String ID;

        BROADCAST_REFRESH_TASKS_PARAMS(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_UPLOAD_TASK_STATUS_PARAMS {
        SUCCESS("success"),
        ERROR_MSG("errMsg");

        public final String ID;

        BROADCAST_UPLOAD_TASK_STATUS_PARAMS(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_REFRESH_PHOTOS_PARAMS {
        SUCCESS("success"),
        ERROR_MSG("errMsg");

        public final String ID;

        BROADCAST_REFRESH_PHOTOS_PARAMS(String id) {
            ID = id;
        }
    }

    public static final String TAG = MainService.class.getSimpleName();

    public static final boolean IS_FOREGROUND = false;
    private static final int LOCATION_REQUEST_MILS = 100;
    private static final int LOCATION_REQUEST_MILS_FASTEST = 100;
    public static final String CHANNEL_ID = "EGNSS4CAPChannel";
    public static final int FOREGROUND_SERVICE_ID = 1;
    private static final int MODE_START = START_STICKY;
    private static final int TASK_UPDATER_INTERVALS_MILS = 3600000; // 1 hod

    private Class mainClass = MainActivity.class;
    private Notification notification;
    private boolean isRunnig = false;
    private AppDatabase appDatabase;
    private Requestor requestor;
    private boolean syncSuccess = true;
    private List<String> lastSyncErrors = new ArrayList<>();
    private Boolean isSync = false;
    private Phaser syncPhaser;

    private Handler tasksUpdaterHandler;
    private Runnable tasksUpdaterRunnable;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location currentLocation;

    public class LocalBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    public final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        initAppDatabase();
        initRequestor();
        // initTasksUpdator();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void initAppDatabase() {
        appDatabase = AppDatabase.build(getApplicationContext());
    }

    private void initRequestor() {
        requestor = new Requestor(this);
    }

    private void initTasksUpdator() {
        tasksUpdaterHandler = new Handler();
        tasksUpdaterRunnable = new Runnable() {
            @Override
            public void run() {
                syncAll();
                tasksUpdaterHandler.postDelayed(this, TASK_UPDATER_INTERVALS_MILS);
            }
        };
        tasksUpdaterHandler.postDelayed(tasksUpdaterRunnable, TASK_UPDATER_INTERVALS_MILS);
    }

    private void finishTaskSync() {
        Log.d("SYNC", "tasks finished");
        syncPhaser.arriveAndDeregister();
    }

    private void finishPathSync() {
        Log.d("SYNC", "paths finished");
        syncPhaser.arriveAndDeregister();
    }

    public void syncAll() {
        if (!LoggedUser.isLogged(appDatabase)) {
            return;
        }
        synchronized (isSync) {
            if (isSync) {
                return;
            }
            isSync = true;
        }
        syncErrorClear();

        syncPhaser = new Phaser(3);
        syncTasks();
        syncPaths();

        Handler joiner = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().submit(() -> {
            syncPhaser.awaitAdvance(syncPhaser.arriveAndDeregister());
            joiner.post(() -> {
                synchronized (isSync) {
                    isSync = false;
                    Log.d("SYNC", "finished all");
                }
            });
        });
    }

    public void syncTasks() {
        sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.REFRESH_TASKS_STARTED));
        TaskList currentTaskList = TaskList.createFromAppDatabase(appDatabase);
        final SyncQueue syncQ = new SyncQueue("syncTasks");
        Task.UpdateTaskReceiver updateTaskReceiver = new Task.UpdateTaskReceiver() {

            @Override
            public void success(AppDatabase appDatabase, Task task) {
                sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.UPLOAD_TASK_STATUS)
                        .putExtra(BROADCAST_UPLOAD_TASK_STATUS_PARAMS.SUCCESS.ID, true));
            }

            @Override
            protected void success(AppDatabase appDatabase) {
                // not used
            }

            @Override
            public void failed(String error) {
                syncErrorAdd("SyncTasks failed\n" + error);
                sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.UPLOAD_TASK_STATUS)
                        .putExtra(BROADCAST_UPLOAD_TASK_STATUS_PARAMS.SUCCESS.ID, false)
                        .putExtra(BROADCAST_UPLOAD_TASK_STATUS_PARAMS.ERROR_MSG.ID, error));
            }

            @Override
            public void finish(boolean success) {
            }
        };
        updateTaskReceiver.setSyncQueue(syncQ);
        currentTaskList.uploadStatus(appDatabase, this, updateTaskReceiver, requestor);

        syncQ.addAsyncExecutor(new SyncQueue.AsyncExecutor("loadTaskExecutor") {
            @Override
            protected void run() {
                if (syncSuccess) {
                    loadTasks(syncQ);
                } else {
                    finishTaskSync();
                    syncQ.executionFinish();
                }
            }
        });
        syncQ.executeQueue();
    }

    private void loadTasks(final SyncQueue syncQueue) {
        requestor.requestAuth("https://server/ws/comm_tasks.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status").trim();
                    if (!status.equals("ok")) {
                        String errMsg = jsonObject.getString("error_msg");
                        syncErrorAdd(errMsg);
                        sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.REFRESH_TASKS).putExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false)
                                .putExtra(BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID, errMsg));
                    }
                    final TaskList taskList = TaskList.createFromJSONArray(appDatabase, jsonObject.getJSONArray("tasks"), LoggedUser.createFromAppDatabase(appDatabase).getId());
                    taskList.refreshToDB();
                    String userId = LoggedUser.createFromAppDatabase(appDatabase).getId();

                    Task virtualTask = Task.createFromAppDatabaseSpecialUnownedPhotos(appDatabase, userId);
                    virtualTask.setOnlySentPhotos(true);
                    virtualTask.deletePhotos(appDatabase, getApplicationContext());
                    taskList.addTask(virtualTask);

                    taskList.updatePhotos(syncQueue, new Task.UpdateTaskReceiver(syncQueue) {
                        @Override
                        protected void success(AppDatabase appDatabase, Task task) {
                            sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.REFRESH_PHOTOS)
                                    .putExtra(BROADCAST_REFRESH_PHOTOS_PARAMS.SUCCESS.ID, true));
                        }

                        @Override
                        protected void success(AppDatabase appDatabase) {
                            // not used
                        }

                        @Override
                        protected void failed(String error) {
                            syncErrorAdd(error);
                            sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.REFRESH_PHOTOS)
                                    .putExtra(BROADCAST_REFRESH_PHOTOS_PARAMS.SUCCESS.ID, false)
                                    .putExtra(BROADCAST_REFRESH_PHOTOS_PARAMS.ERROR_MSG.ID, error));
                        }

                        @Override
                        protected void finish(boolean success) {
                            // nothing
                        }
                    }, requestor, MainService.this);
                    syncQueue.addAsyncExecutor(new SyncQueue.AsyncExecutor() {
                        @Override
                        protected void run() {
                            if (syncSuccess) {
                                sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.REFRESH_TASKS).putExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, true));
                            }
                            finishTaskSync();
                            syncQueue.executionFinish();
                        }
                    });
                } catch (JSONException e) {
                    syncErrorAdd(e.getMessage());
                    sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.REFRESH_TASKS).putExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false)
                            .putExtra(BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID, e.getMessage()));
                } finally {
                    if (syncQueue.isLastRunning()) {
                        finishTaskSync();
                    }
                    syncQueue.executionFinish();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                syncErrorAdd(error.getMessage());
                sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.REFRESH_TASKS).putExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false)
                        .putExtra(BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID, error.getMessage()));
                finishTaskSync();
                syncQueue.executionFinish();
            }
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", LoggedUser.createFromAppDatabase(appDatabase).getId());
                return params;
            }
        });
    }

    private void syncPaths() {
        downloadPaths();
    }

    private void downloadPaths() {
        requestor.requestAuth("https://server/ws/comm_get_paths.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status").trim();
                    if (!status.equals("ok")) {
                        String errMsg = jsonObject.getString("error_msg");
                        syncErrorAdd("downloadPaths failed, bad status\n" + errMsg);
                        return;
                    }
                    JSONArray jsonPaths = jsonObject.getJSONArray("paths");
                    List<PTPath> paths = new ArrayList<>();
                    for (int i = 0; i < jsonPaths.length(); ++i) {
                        PTPath.createFromJSON(appDatabase, jsonPaths.getJSONObject(i)).saveToDB(appDatabase);
                    }
                    uploadPaths();
                } catch (JSONException jsonException) {
                    syncErrorAdd("downloadPaths failed, json error\n" + jsonException.getMessage());
                    finishPathSync();
                } finally {
                    Log.d("SYNC PATH DOWNLOAD", response);
                    Log.d("SYNC", "paths finished");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                syncErrorAdd("downloadPaths failed, network error\n" + Util.volleyErrorMsg(error));
                finishPathSync();
            }
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", LoggedUser.createFromAppDatabase(appDatabase).getId());
                return params;
            }
        });
    }

    private void uploadPaths() {
        List<PTPath> unsentPaths = PTPath.createListFromAppDatabaseUnsent(appDatabase);
        Phaser phaser = new Phaser(unsentPaths.size() + 1);
        for (PTPath ptPath : unsentPaths) {
            requestor.requestAuth("https://server/ws/comm_path.php", new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String status = jsonObject.getString("status").trim();
                        if (!status.equals("ok")) {
                            String errMsg = jsonObject.getString("error_msg");
                            syncErrorAdd("uploadPaths failed, bad status\n" + errMsg);
                            return;
                        }
                        ptPath.setRealId(jsonObject.getLong("path_id"));
                        ptPath.saveToDB(appDatabase);
                    } catch (JSONException jsonException) {
                        syncErrorAdd("uploadPaths failed, response json error\n" + jsonException.getMessage());
                    } finally {
                        Log.d("SYNC PATH", "ptPath: " + ptPath.getAutoId() + "uploaded");
                        phaser.arriveAndDeregister();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    syncErrorAdd("uploadPaths failed, network error\n" + Util.volleyErrorMsg(error));
                    Log.d("SYNC PATH", "ptPath: " + ptPath.getAutoId() + "uploaded");
                    phaser.arriveAndDeregister();
                }
            }, new Requestor.Req() {
                @Override
                public Map<String, String> getParams() {
                    try {
                        Map<String, String> params = new HashMap<>();
                        params.put("user_id", LoggedUser.createFromAppDatabase(appDatabase).getId());
                        params.put("name", ptPath.getName());
                        params.put("start", ptPath.getStartT().toString(PTPath.DATETIME_RECEIVED_FORMAT));
                        params.put("end", ptPath.getEndT().toString(PTPath.DATETIME_RECEIVED_FORMAT));
                        params.put("area", ptPath.getArea() == null ? "0" : PTPath.AREA_FORMAT.format(ptPath.getArea()));
                        params.put("points", ptPath.pointsToJSONArray().toString());
                        return params;
                    } catch (JSONException jsonException) {
                        this.forceCancel = true;
                        syncErrorAdd("uploadPaths failed, params json error\n" + jsonException.getMessage());
                        Log.d("SYNC PATH", "ptPath: " + ptPath.getAutoId() + "uploaded");
                        phaser.arriveAndDeregister();
                        return null;
                    }
                }
            });
        }

        Handler joiner = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().submit(() -> {
            phaser.awaitAdvance(phaser.arriveAndDeregister());
            joiner.post(() -> finishPathSync());
        });
    }

    private void syncErrorAdd(String msg) {
        syncSuccess = false;
        lastSyncErrors.add(msg);
    }

    private void syncErrorClear() {
        syncSuccess = true;
        lastSyncErrors.clear();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunnig) {
            return MODE_START;
        }

        Intent notificationIntent = new Intent(this, StartActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.ms_notificationContent))
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        /* DEBUGCOM
        // testování neodchycených výjímek
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("BAF BAF BAF");
            }
        }, 10000);
        /**/

        // region example for simple background notification
        /*
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(111, notification);
        /**/
        // endregion

        if (IS_FOREGROUND) {
            startForeground(FOREGROUND_SERVICE_ID, notification);
        }

        isRunnig = true;
        sendBroadcastMessage(createBroadcastIntent(BROADCAST_MSG.STARTED));
        return MODE_START;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunnig = false;

        if (tasksUpdaterHandler != null && tasksUpdaterRunnable != null) {
            tasksUpdaterHandler.removeCallbacks(tasksUpdaterRunnable);
            tasksUpdaterHandler = null;
            tasksUpdaterRunnable = null;
        }
    }


    public Intent createBroadcastIntent(BROADCAST_MSG broadcast_msg) {
        Intent intent = new Intent(BROADCAST_MSG.BROADCAST_ID.ID);
        intent.putExtra(BROADCAST_MSG.TYPE.ID, broadcast_msg.ID);
        return intent;
    }

    public void sendBroadcastMessage(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void startLocationMonitoring(final LocationReceiver receiver) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_REQUEST_MILS);
        locationRequest.setFastestInterval(LOCATION_REQUEST_MILS_FASTEST);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        if (receiver != null) {
                            receiver.receive(location);
                        }
                    }
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // nothing
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }

    }

    public void stopLocationMonitoring(OnSuccessListener onSuccessListener, OnFailureListener onFailureListener, OnCompleteListener onCompleteListener) {
        if (fusedLocationClient != null) {
            com.google.android.gms.tasks.Task task = fusedLocationClient.removeLocationUpdates(locationCallback);
            if (onSuccessListener != null) {
                task.addOnSuccessListener(onSuccessListener);
            }
            if (onFailureListener != null) {
                task.addOnFailureListener(onFailureListener);
            }
            if (onCompleteListener != null) {
                task.addOnCompleteListener(onCompleteListener);
            }
        }
    }

    // region set, get

    public boolean isRunnig() {
        return isRunnig;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }

    public Requestor getRequestor() {
        return requestor;
    }

    public Class getMainClass() {
        return mainClass;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public Boolean isSync() {
        return isSync;
    }

    public boolean isSyncSuccess() {
        return syncSuccess;
    }

    public List<String> getLastSyncErrors() {
        return new ArrayList<>(lastSyncErrors);
    }

    // endregion
}
