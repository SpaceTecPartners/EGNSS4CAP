package eu.foxcom.stp.gsa.egnss4cap.model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.foxcom.stp.gsa.egnss4cap.R;
import eu.foxcom.stp.gsa.egnss4cap.model.functionInterface.BiConsumer;
import eu.foxcom.stp.gsa.egnss4cap.model.functionInterface.Consumer;
import eu.foxcom.stp.gsa.egnss4cap.model.functionInterface.Function;

@Entity
public class Task {

    public static abstract class UpdateTaskReceiver extends UpdateReceiver {

        private boolean isAutoSuccessDBSave = true;

        public UpdateTaskReceiver() {
            super();
        }

        public UpdateTaskReceiver(SyncQueue syncQueue) {
            super(syncQueue);
        }

        public UpdateTaskReceiver(Phaser phaser) {
            super(phaser);
        }

        protected final void successExec(AppDatabase appDatabase, Task task) {
            if (isAutoSuccessDBSave) {
                task.saveToDB(appDatabase);
            }
            success(appDatabase, task);
            finishExec(true);
        }

        protected abstract void success(AppDatabase appDatabase, Task task);

        @Override
        protected void failedExec(String error) {
            failed(error);
            finishExec(false);
        }

        // region get, set

        public boolean isAutoSuccessDBSave() {
            return isAutoSuccessDBSave;
        }

        public void setAutoSuccessDBSave(boolean autoSuccessDBSave) {
            isAutoSuccessDBSave = autoSuccessDBSave;
        }

        // endregion
    }

    public enum STATUS {
        NEW("new", R.drawable.point_yellow, R.string.stat_new),
        OPEN("open", R.drawable.point_blue, R.string.stat_open),
        RETURNED("returned", R.drawable.point_purple, R.string.stat_returned),
        DATA_PROVIDED("data provided", R.drawable.point_white, R.string.stat_dataProvided),
        DATA_CHECKED("data checked", R.drawable.point_white, R.string.stat_dataCheked),
        CLOSE("closed", R.drawable.point_gray, R.string.stat_closed);

        public static STATUS createFromDBVal(String dbVal) {
            for (STATUS status : STATUS.values()) {
                if (status.DB_VAL.equals(dbVal)) {
                    return status;
                }
            }
            return null;
        }

        public final String DB_VAL;
        public final int POINT_ID;
        public final int NAME_ID;

        STATUS(String dbVal, int pointId, int nameId) {
            DB_VAL = dbVal;
            POINT_ID = pointId;
            NAME_ID = nameId;
        }
    }

    private class PhotoRealIdsCollector {
        private boolean isPhotoRealIdsPrepared = false;
        private List<Long> photoRealIds = new ArrayList<>();
        private List<Long> toUpdatePhotoRealIds = new ArrayList<>();
        private List<Long> toRemovePhotoRealIds = new ArrayList<>();

        void setPhotoRealIds(List<Long> photoRealIds) {
            this.photoRealIds = photoRealIds;
            isPhotoRealIdsPrepared = false;
        }

        void prepareRealIdsForProcessing(AppDatabase appDatabase) {
            if (isPhotoRealIdsPrepared) {
                return;
            }
            if (photoRealIds.size() == 0) {
                isPhotoRealIdsPrepared = true;
                return;
            }

            List<Long> newRealIds = photoRealIds;
            List<Long> oldRealIds;
            if (id != null) {
                oldRealIds = appDatabase.photoDao().selectRealIdsByTaskId(id);
            } else {
                oldRealIds = appDatabase.photoDao().selectUnassignedIds(LoggedUser.createFromAppDatabase(appDatabase).getId());
                List<Long> toRemoveIds = new ArrayList<>(oldRealIds);
                toRemoveIds.removeAll(newRealIds);
                toRemovePhotoRealIds = toRemoveIds;
            }
            List<Long> toUpdateIds = new ArrayList<>(newRealIds);
            toUpdateIds.removeAll(oldRealIds);
            toUpdatePhotoRealIds = toUpdateIds;

            isPhotoRealIdsPrepared = true;
        }
    }

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @PrimaryKey
    @NonNull
    private String id;
    private String status;
    private Integer statusOrder;
    private String name;
    private String text;
    private String textReturned;
    private String dateCreated;
    private DateTime dateCreatedDateTime;
    private String taskDueToDate;
    private DateTime taskDueToDateDateTime;
    private Boolean flagValid;
    private Boolean flagInvalid;
    private String note;
    private String userId;
    // only for id = null
    // if unownedPhotoId = null && id = null then contains all unowned photos
    private Long unownedPhotoId;
    // only not sent (primary priority)
    private boolean notSentPhotos = false;
    // only sent (secondary priority)
    private boolean onlySentPhotos = false;

    private boolean isUploadStatus = false;
    private boolean isPhotoSync = false;

    @Ignore
    private PhotoRealIdsCollector photoRealIdsCollector = new PhotoRealIdsCollector();

    // indikace chyby při posledním odesílání na server
    // přidává mezistav pro nové odeslání tasku bez možnosti změn
    // pro fotografie v tasku není speciálně ošetřeno (nutný refaktor celé synchronizace)
    @ColumnInfo(defaultValue = "0")
    private boolean isLastSendFailed = false;

    public static int numberOfTasks(AppDatabase appDatabase, STATUS status, String userId) {
        return appDatabase.taskDao().countOfTasksByStatus(status.DB_VAL, userId);
    }

    public static Task createFromResponse(JSONObject jsonObject, String userId) throws JSONException {
        return new Task(jsonObject, userId, null);
    }

    public static Task createFromAppDatabase(String taskId, AppDatabase appDatabase) {
        return appDatabase.taskDao().selectTaskById(taskId);
    }

    public static Task createFromAppDatabaseSpecialUnownedPhoto(AppDatabase appDatabase, Long id, String userId) throws JSONException {
        return new Task(null, userId, id);
    }

    public static Task createFromAppDatabaseSpecialUnownedPhotos(AppDatabase appDatabase, String userId) throws JSONException {
        return new Task(null, userId, null);
    }

    private Task(JSONObject jsonObject, String userId, Long unownedPhotoId) throws JSONException {
        if (jsonObject != null) {
            id = jsonObject.getString("id");
            status = Util.JSONgetStringNullable(jsonObject, "status");
            if (status != null) {
                statusOrder = STATUS.createFromDBVal(status).ordinal();
            }
            name = Util.JSONgetStringNullable(jsonObject, "name");
            text = Util.JSONgetStringNullable(jsonObject, "text");
            textReturned = Util.JSONgetStringNullable(jsonObject, "text_returned");
            dateCreated = Util.JSONgetStringNullable(jsonObject, "date_created");
            if (dateCreated != null) {
                dateCreatedDateTime = DateTime.parse(dateCreated, DateTimeFormat.forPattern(DATETIME_FORMAT));
            }
            taskDueToDate = Util.JSONgetStringNullable(jsonObject, "task_due_date");
            if (taskDueToDate != null) {
                taskDueToDateDateTime = DateTime.parse(taskDueToDate, DateTimeFormat.forPattern(DATETIME_FORMAT));
            }
            note = Util.JSONgetStringNullable(jsonObject, "note");
            flagValid = "1".equals(Util.JSONgetStringNullable(jsonObject, "flag_valid"));
            flagInvalid = "1".equals(Util.JSONgetStringNullable(jsonObject, "flag_invalid"));
            JSONArray photoIdsJsonArray = jsonObject.getJSONArray("photos_ids");
            List<Long> realIds = new ArrayList<>();
            for (int i = 0; i < photoIdsJsonArray.length(); ++i) {
                realIds.add(photoIdsJsonArray.getLong(i));
            }
            photoRealIdsCollector.setPhotoRealIds(realIds);
        } else {
            this.unownedPhotoId = unownedPhotoId;
        }
        this.userId = userId;
    }

    public Task() {
    }

    public void saveToDB(AppDatabase appDatabase) {
        if (id != null) {
            appDatabase.taskDao().updateTask(this);
        }
    }

    public void updateStatus(final AppDatabase appDatabase, Context context, final UpdateTaskReceiver receiver, Requestor requestor) {
        requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_update.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status");
                    if (!status.equals("ok")) {
                        String errMsg = jsonObject.getString("error_msg");
                        receiver.failedExec(errMsg);
                        return;
                    }
                    isUploadStatus = false;
                    receiver.successExec(appDatabase, Task.this);
                } catch (JSONException e) {
                    receiver.failedExec(e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                receiver.failedExec(error.getMessage());
            }
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                if (id != null) {
                    params.put("task_id", id);
                    params.put("user_id", LoggedUser.createFromAppDatabase(appDatabase).getId());
                    params.put("status", status);
                }
                return params;
            }
        });
    }

    /**
     * užití při odeslání tasku a jeho fotografií v jednom requestu
     * (zastaralé)
     * (může překročit limit dat při velkém množství fotek v jednom tasku)
     */
    @Deprecated
    public void updateCompleteInSingleRequest(final AppDatabase appDatabase, Context context, final UpdateTaskReceiver receiver, Requestor requestor) {
        requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_update.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status");
                    if (!status.equals("ok")) {
                        String errMsg = jsonObject.getString("error_msg");
                        receiver.failedExec(errMsg);
                        return;
                    }
                    receiver.successExec(appDatabase, Task.this);
                } catch (JSONException e) {
                    receiver.failedExec(e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                receiver.failedExec(error.getMessage());
            }
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                if (id != null) {
                    params.put("task_id", id);
                    params.put("status", status);
                    params.put("note", note);
                }
                params.put("user_id", userId);
                PhotoList photoList = createPhotoList(appDatabase, context);
                JSONArray jsonArray;
                try {
                    jsonArray = photoList.toJSONArray();
                    params.put("photos", jsonArray.toString());
                    return params;
                } catch (Exception e) {
                    receiver.failedExec(e.getMessage());
                    return null;
                }
            }
        });
    }


    /**
     * užití při odeslání tasku v několika requestech
     * (nové)
     * (z důvodu odstranění limitu na data v jednom requestu)
     * sync(async(reqPhoto0 ... req_photoN), reqStatus)
     *
     * @param finalReceiver success větev je aplikována na úplném konci, po úspěchu všech reqeustů, v requestu pro update statusu
     *                      failed větev je aplikována na konci odeslání všech fotek (v případě chyby), nebo pak na konci requestu pro update statusu (v případě chyby)
     */
    public void updateCompleteInMultipleRequest(final AppDatabase appDatabase, Context context, final UpdateTaskReceiver finalReceiver, Requestor requestor, PhotoList photoList) {

        // <photoIndex, errors>
        Map<Integer, String> errors = new HashMap<>();
        //PhotoList photoList = createPhotoList(appDatabase, context);
        // <photoIndex, success>
        BlockingQueue<Pair<Integer, Boolean>> photoReqResults = new ArrayBlockingQueue<>(photoList.getPhotos().size());
        Function<VolleyError, String> volleyErrorConsumer = volleyError -> {
            String statusCode = "none";
            if (volleyError.networkResponse != null) {
                statusCode = String.valueOf(volleyError.networkResponse.statusCode);
            }
            return "Status Code " + statusCode + "\nError: " + volleyError.getMessage();
        };
        BiConsumer<Photo, String> failedPhotoExec = (photo, error) -> {
            errors.put(photo.getIndx(), error);
            photoReqResults.add(new Pair(photo.getIndx(), false));
        };
        Consumer<String> failedFinalExec = (error) -> {
            String errorResult = "";
            for (Map.Entry<Integer, String> entry : errors.entrySet()) {
                errorResult += "Failed request photo (index " + entry.getKey() + "):\n" + entry.getValue() + "\n\n";
            }
            if (error != null) {
                errorResult += "Failed request status:\n" + error;
            }
            finalReceiver.failedExec(errorResult);
        };

        // region photos request definition
        Function<Photo, Response.Listener<String>> photoListener = photo -> (Response.Listener<String>) response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String status = jsonObject.getString("status");
                /* DEBUGCOM
                if (photo.getNote().equals("C")) {
                    status = "chyba";
                    jsonObject.put("error_msg", status);
                }
                /**/
                if (!status.equals("ok")) {
                    String errMsg = jsonObject.getString("error_msg");
                    failedPhotoExec.accept(photo, errMsg);
                    return;
                }
                photo.setRealId(jsonObject.getLong("photo_id"));
                photo.refreshToDB(appDatabase);
                photoReqResults.add(new Pair<>(photo.getIndx(), true));
            } catch (JSONException e) {
                failedPhotoExec.accept(photo, e.getMessage());
            }
        };
        Function<Photo, Response.ErrorListener> photoErrorListener = photo -> {
            return new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    failedPhotoExec.accept(photo, volleyErrorConsumer.apply(error));
                }
            };
        };
        Function<Photo, Requestor.Req> photoRequestor = photo -> new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                if (id != null) {
                    params.put("task_id", id);
                }
                params.put("user_id", userId);
                JSONObject jsonObject;
                try {
                    jsonObject = photo.toJSONObject();
                    params.put("photo", jsonObject.toString());
                    return params;
                } catch (Exception e) {
                    failedPhotoExec.accept(photo, e.getMessage());
                    return null;
                }
            }
        };
        // endregion

        for (Photo photo : photoList.getPhotos()) {
            requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_photo.php",
                    photoListener.apply(photo),
                    photoErrorListener.apply(photo),
                    photoRequestor.apply(photo));
        }

        Handler joinHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                if (photoReqResults.size() < photoList.getPhotos().size()) {
                    continue;
                } else {
                    break;
                }
            }
            boolean result = true;
            for (Pair<Integer, Boolean> pair : photoReqResults) {
                if (!pair.second) {
                    result = false;
                    break;
                }
            }
            boolean finalResult = result;
            joinHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (finalResult) {
                        if (id == null) {
                            finalReceiver.successExec(appDatabase, Task.this);
                        } else {
                            // region status request definition
                            Response.Listener<String> statusListener = response -> {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    String status = jsonObject.getString("status");
                                    if (!status.equals("ok")) {
                                        String errMsg = jsonObject.getString("error_msg");
                                        failedFinalExec.accept(errMsg);
                                        return;
                                    }
                                    finalReceiver.successExec(appDatabase, Task.this);
                                } catch (JSONException e) {
                                    failedFinalExec.accept(e.getMessage());
                                }
                            };
                            Response.ErrorListener statusErrorListener = new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    failedFinalExec.accept(volleyErrorConsumer.apply(error));
                                    CountDownLatch d = new CountDownLatch(5);
                                    Future f = new FutureTask(new Callable() {
                                        @Override
                                        public Object call() throws Exception {
                                            return null;
                                        }
                                    });
                                }
                            };
                            Requestor.Req statusRequestor = new Requestor.Req() {
                                @Override
                                public Map<String, String> getParams() {
                                    Map<String, String> params = new HashMap<>();
                                    if (id != null) {
                                        params.put("task_id", id);
                                        params.put("status", status);
                                        params.put("note", note);
                                    }
                                    params.put("user_id", userId);
                                    return params;
                                }
                            };
                            // endregion
                            requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_status.php",
                                    statusListener,
                                    statusErrorListener,
                                    statusRequestor);
                        }
                    } else {
                        failedFinalExec.accept(null);
                    }

                }
            });
        });
    }

    // (zastaralé)
    @Deprecated
    public void updateCompleteDivided(final AppDatabase appDatabase, Context context, final UpdateTaskReceiver receiver, final UpdateTaskReceiver finalReceiver, Requestor requestor) throws Exception {
        PhotoList photoList = createPhotoList(appDatabase, context);
        JSONArray photoJsonArray = photoList.toJSONArray();
        SyncQueue syncQueue = new SyncQueue();
        receiver.setSyncQueue(syncQueue);
        finalReceiver.setSyncQueue(syncQueue);
        syncQueue.addAsyncExecutor(new SyncQueue.AsyncExecutor() {
            @Override
            protected void run() {
                requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_update.php", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String status = jsonObject.getString("status");
                            if (!status.equals("ok")) {
                                String errMsg = jsonObject.getString("error_msg");
                                receiver.failedExec(errMsg);
                                return;
                            }

                            for (int i = 0; i < photoJsonArray.length(); ++i) {
                                UpdateTaskReceiver taskReceiver = receiver;
                                if (i == photoJsonArray.length() - 1) {
                                    taskReceiver = finalReceiver;
                                }
                                int finalI = i;
                                UpdateTaskReceiver finalTaskReceiver = taskReceiver;
                                syncQueue.addAsyncExecutor(new SyncQueue.AsyncExecutor() {
                                    @Override
                                    protected void run() {
                                        requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_update.php", new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                try {
                                                    JSONObject jsonObject = new JSONObject(response);
                                                    String status = jsonObject.getString("status");
                                                    if (!status.equals("ok")) {
                                                        String errMsg = jsonObject.getString("error_msg");
                                                        finalTaskReceiver.failedExec(errMsg);
                                                        return;
                                                    }
                                                    finalTaskReceiver.successExec(appDatabase, Task.this);
                                                } catch (JSONException e) {
                                                    finalTaskReceiver.failedExec(e.getMessage());
                                                } finally {
                                                }
                                            }
                                        }, new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                finalTaskReceiver.failedExec(error.getMessage());
                                            }
                                        }, new Requestor.Req() {
                                            @Override
                                            public Map<String, String> getParams() {
                                                Map<String, String> params = new HashMap<>();
                                                if (id != null) {
                                                    params.put("task_id", id);
                                                    params.put("user_id", userId);
                                                    params.put("status", status);
                                                    params.put("note", note);
                                                }
                                                try {
                                                    JSONArray jsonArray = new JSONArray();
                                                    jsonArray.put(jsonArray.get(finalI));
                                                    params.put("photos", jsonArray.toString());
                                                } catch (Exception e) {
                                                    finalTaskReceiver.failedExec(e.getMessage());
                                                    return null;
                                                }
                                                return params;
                                            }
                                        });
                                    }
                                });
                            }

                            receiver.successExec(appDatabase, Task.this);
                        } catch (JSONException e) {
                            receiver.failedExec(e.getMessage());
                        } finally {
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        receiver.failedExec(error.getMessage());
                    }
                }, new Requestor.Req() {
                    @Override
                    public Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<>();
                        if (id != null) {
                            params.put("task_id", id);
                            params.put("userId", userId);
                            params.put("status", status);
                            params.put("note", note);
                        }
                        JSONArray jsonArray = new JSONArray();
                        params.put("photos", jsonArray.toString());
                        return params;
                    }
                });
            }
        });
        syncQueue.executeQueue();
    }

    @Deprecated
    public void updatePhotos(final AppDatabase appDatabase, final Task.UpdateTaskReceiver receiver, Requestor requestor, final Context context) {
        if (isPhotoSync) {
            receiver.successExec(appDatabase, this);
            return;
        }
        requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_task_photos.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status");
                    if (!status.equals("ok")) {
                        String errMsg = jsonObject.getString("error_msg");
                        receiver.failedExec(errMsg);
                        return;
                    }
                    JSONArray jsonArray = jsonObject.getJSONArray("photos");
                    PhotoList photoL = PhotoList.createFromJSONArray(appDatabase, jsonArray, id, context);
                    Task.this.isPhotoSync = true;
                    photoL.deleteSameDigestPhotos();
                    photoL.recreateToDB();
                    receiver.successExec(appDatabase, Task.this);
                } catch (JSONException e) {
                    receiver.failedExec(e.getMessage());
                } catch (IOException e) {
                    receiver.failedExec(e.getMessage());
                } catch (Exception e) {
                    receiver.failedExec(e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                receiver.failedExec(error.getMessage());
            }
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                if (id != null) {
                    params.put("task_id", id);
                }
                params.put("user_id", userId);
                return params;
            }
        });
    }

    public void updatePhotosByRealIds(AppDatabase appDatabase, Context context, Requestor requestor, Photo.UpdatePhotoReceiver photoReceiver, Task.UpdateTaskReceiver taskReceiver) {
        if (photoRealIdsCollector.photoRealIds.size() == 0) {
            taskReceiver.successExec(appDatabase, this);
            return;
        }
        if (id == null) {
            appDatabase.photoDao().deleteByRealIds(photoRealIdsCollector.toRemovePhotoRealIds);
        }
        prepareRealIdsForProcessing(appDatabase);
        if (photoRealIdsCollector.toUpdatePhotoRealIds.size() == 0) {
            taskReceiver.successExec(appDatabase, this);
            return;
        }

        Phaser phaser = new Phaser(photoRealIdsCollector.toUpdatePhotoRealIds.size() + 1);
        String errMsgTitle = "Photo downlad failed (taskId = " + id + ")";
        AtomicBoolean isSuccess = new AtomicBoolean(false);
        Photo.UpdatePhotoReceiver innerReceiver = new Photo.UpdatePhotoReceiver(phaser) {
            @Override
            protected void success(AppDatabase appDatabase, Photo photo) {
            }

            @Override
            protected void success(AppDatabase appDatabase) {
            }

            @Override
            protected void failed(String error) {
            }

            @Override
            protected void finish(boolean success) {
                isSuccess.set(success);
            }
        };
        innerReceiver.setAutoSuccessDBSave(false);
        BiConsumer<AppDatabase, Photo> successConsumer = (appDatabase1, photo) -> {
            innerReceiver.successExec(appDatabase, photo);
            photoReceiver.successExec(appDatabase, photo);
        };
        Consumer<String> failedConsumer = errMsg -> {
            innerReceiver.failedExec(errMsgTitle + ": " + errMsg);
            photoReceiver.failedExec(errMsgTitle + ": " + errMsg);
        };
        for (Long realId : photoRealIdsCollector.toUpdatePhotoRealIds) {
            requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_get_photo.php", response -> {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status");
                    if (!status.equals("ok")) {
                        String errMsg = jsonObject.getString("error_msg");
                        failedConsumer.accept(errMsg);
                        return;
                    }
                    String taskId = Task.this.id;
                    Photo photo = Photo.createFromResponse(jsonObject.getJSONObject("photo"), realId, taskId, Photo.findNewIndx(taskId, appDatabase), context, appDatabase);
                    photo.refreshToDB(appDatabase);
                    successConsumer.accept(appDatabase, photo);
                } catch (JSONException | IOException e) {
                    failedConsumer.accept(e.getMessage());
                }
            }, error -> {
                failedConsumer.accept(Util.volleyErrorMsg(error));
            }, new Requestor.Req() {
                @Override
                public Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("photo_id", String.valueOf(realId));
                    return params;
                }
            });
        }

        Handler joinerHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().submit(() -> {
            phaser.awaitAdvance(phaser.arriveAndDeregister());
            joinerHandler.post(() -> {
                Task task = Task.this;
                if (isSuccess.get()) {
                    task.isPhotoSync = true;
                    task.saveToDB(appDatabase);
                    taskReceiver.successExec(appDatabase, task);
                } else {
                    task.isPhotoSync = false;
                    task.saveToDB(appDatabase);
                    taskReceiver.failedExec("Download photos of task (id = " + task.id + ") failed.");
                }
            });
        });
    }

    public void prepareRealIdsForProcessing(AppDatabase appDatabase) {
        photoRealIdsCollector.prepareRealIdsForProcessing(appDatabase);
    }

    // only for unassignedPhotos
    public void downloadUnassignedPhotoRealIds(AppDatabase appDatabase, Context context, Requestor requestor, UpdateTaskReceiver taskReceiver) {
        // region CHECKEXCEPTIONS
        if (id != null || unownedPhotoId != null) {
            throw new RuntimeException("Cannot download unassigned photos ids for real task.");
        }
        // endregion

        String errMsgTitle = "Download unassigned photo ids failed.";
        Consumer<String> failedConsumer = errMsg -> {
            taskReceiver.failedExec(errMsgTitle + ": " + errMsg);
        };
        requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_unassigned.php", response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String status = jsonObject.getString("status");
                if (!status.equals("ok")) {
                    String errMsg = jsonObject.getString("error_msg");
                    failedConsumer.accept(errMsg);
                    return;
                }
                JSONArray photoIds = jsonObject.getJSONArray("photos_ids");
                List<Long> realIds = new ArrayList<>();
                for (int i = 0; i < photoIds.length(); ++i) {
                    realIds.add(photoIds.getLong(i));
                }
                photoRealIdsCollector.setPhotoRealIds(realIds);
                taskReceiver.successExec(appDatabase, Task.this);
            } catch (JSONException e) {
                failedConsumer.accept(e.getMessage());
            }
        }, error -> {
            failedConsumer.accept(Util.volleyErrorMsg(error));
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", LoggedUser.createFromAppDatabase(appDatabase).getId());
                return params;
            }
        });
    }

    public int getPhotoCount(AppDatabase appDatabase) {
        return appDatabase.photoDao().selectTaskPhotos(id).size();
    }

    public DateTime getTaskDueToDateDatetime() {
        if (taskDueToDate != null) {
            return DateTime.parse(taskDueToDate, DateTimeFormat.forPattern(DATETIME_FORMAT));
        }
        return null;
    }

    private PhotoList createPhotoList(AppDatabase appDatabase, Context context) {
        PhotoList photoList;
        if (id == null && unownedPhotoId != null) {
            photoList = PhotoList.createFromAppDatabaseUnownedPhoto(appDatabase, unownedPhotoId, context);
        } else if (id == null && notSentPhotos) {
            photoList = PhotoList.createFromAppDatabaseUnownedPhotos(appDatabase, true, context);
        } else if (id != null && notSentPhotos) {
            photoList = PhotoList.createFromAppDatabaseNotSent(appDatabase, id, context);
        } else if (id == null && onlySentPhotos) {
            photoList = PhotoList.createFromAppDatabaseUnownedPhotosOnlySent(appDatabase, context);
        } else if (id != null && onlySentPhotos) {
            photoList = PhotoList.createFromAppDatabaseOnlySent(appDatabase, id, context);
        } else {
            photoList = PhotoList.createFromAppDatabaseByTaskGroup(appDatabase, id, context);
        }
        return photoList;
    }

    public void deletePhotos(AppDatabase appDatabase, Context context) {
        PhotoList photoList = createPhotoList(appDatabase, context);
        for (Photo photo : photoList.getPhotos()) {
            photo.delete(appDatabase);
        }
    }

    // (v uživatelském rámci)
    public boolean isEditable() {
        return (status.equals(Task.STATUS.OPEN.DB_VAL) || status.equals(Task.STATUS.RETURNED.DB_VAL)) && !isLastSendFailed;
    }

    // (v uživatelském rámci)
    // musí splňovat implikaci isEditable = true => isSendable = true
    public boolean isSendable() {
        return (status.equals(Task.STATUS.OPEN.DB_VAL) || status.equals(Task.STATUS.RETURNED.DB_VAL));
    }

    public List<Long> getToUpdatePhotoRealIds() {
        return new ArrayList<>(photoRealIdsCollector.toUpdatePhotoRealIds);
    }

    // region get, set

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusOrder() {
        return statusOrder;
    }

    public void setStatusOrder(Integer statusOrder) {
        this.statusOrder = statusOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
        if (dateCreated != null) {
            this.dateCreatedDateTime = DateTime.parse(dateCreated, DateTimeFormat.forPattern(DATETIME_FORMAT));
        } else {
            this.dateCreatedDateTime = null;
        }
    }

    public String getTaskDueToDate() {
        return taskDueToDate;
    }

    public void setTaskDueToDate(String taskDueToDate) {
        this.taskDueToDate = taskDueToDate;
        if (this.taskDueToDate != null) {
            taskDueToDateDateTime = DateTime.parse(taskDueToDate, DateTimeFormat.forPattern(DATETIME_FORMAT));
        } else {
            this.taskDueToDate = null;
        }
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isUploadStatus() {
        return isUploadStatus;
    }

    public void setUploadStatus(boolean uploadStatus) {
        isUploadStatus = uploadStatus;
    }

    public boolean isPhotoSync() {
        return isPhotoSync;
    }

    public void setPhotoSync(boolean photoSync) {
        isPhotoSync = photoSync;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public DateTime getTaskDueToDateDateTime() {
        return taskDueToDateDateTime;
    }

    public void setTaskDueToDateDateTime(DateTime taskDueToDateDateTime) {
        this.taskDueToDateDateTime = taskDueToDateDateTime;
        if (taskDueToDateDateTime != null) {
            this.taskDueToDate = taskDueToDateDateTime.toString(DateTimeFormat.forPattern(DATETIME_FORMAT));
        } else {
            this.taskDueToDate = null;
        }
    }

    public DateTime getDateCreatedDateTime() {
        return dateCreatedDateTime;
    }

    public void setDateCreatedDateTime(DateTime dateCreatedDateTime) {
        this.dateCreatedDateTime = dateCreatedDateTime;
        if (dateCreatedDateTime != null) {
            this.dateCreated = dateCreatedDateTime.toString(DateTimeFormat.forPattern(DATETIME_FORMAT));
        } else {
            this.dateCreated = null;
        }
    }

    public Long getUnownedPhotoId() {
        return unownedPhotoId;
    }

    public void setUnownedPhotoId(Long unownedPhotoId) {
        this.unownedPhotoId = unownedPhotoId;
    }

    public boolean isNotSentPhotos() {
        return notSentPhotos;
    }

    public void setNotSentPhotos(boolean notSentPhotos) {
        this.notSentPhotos = notSentPhotos;
    }

    public String getTextReturned() {
        return textReturned;
    }

    public void setTextReturned(String textReturned) {
        this.textReturned = textReturned;
    }

    public Boolean getFlagValid() {
        return flagValid;
    }

    public void setFlagValid(Boolean flagValid) {
        this.flagValid = flagValid;
    }

    public Boolean getFlagInvalid() {
        return flagInvalid;
    }

    public void setFlagInvalid(Boolean flagInvalid) {
        this.flagInvalid = flagInvalid;
    }

    public boolean isOnlySentPhotos() {
        return onlySentPhotos;
    }

    public void setOnlySentPhotos(boolean onlySentPhotos) {
        this.onlySentPhotos = onlySentPhotos;
    }

    public boolean isLastSendFailed() {
        return isLastSendFailed;
    }

    public void setLastSendFailed(boolean lastSendFailed) {
        isLastSendFailed = lastSendFailed;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */