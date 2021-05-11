package eu.foxcom.stp.gsa.egnss4cap.model.pathTrack;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.foxcom.stp.gsa.egnss4cap.model.AppDatabase;
import eu.foxcom.stp.gsa.egnss4cap.model.LoggedUser;
import eu.foxcom.stp.gsa.egnss4cap.model.Requestor;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;

@Entity(indices = {@Index(value = {"realId"}, unique = true)})
public class PTPath {

    private class UploadExecutor {

        private AppDatabase appDatabase;
        private UploadReceiver uploadReceiver;

        private UploadExecutor(AppDatabase appDatabase, UploadReceiver uploadReceiver) {
            this.appDatabase = appDatabase;
            this.uploadReceiver = uploadReceiver;
        }

        private void success() {
            isLastSendFailed = false;
            if (uploadReceiver != null) {
                uploadReceiver.success();
            }
            complete();
        }

        private void failed(String errMsg) {
            isLastSendFailed = true;
            if (uploadReceiver != null) {
                uploadReceiver.failed(errMsg);
            }
            complete();
        }

        private void complete() {
            saveToDB(appDatabase);
            if (uploadReceiver != null) {
                uploadReceiver.complete();
            }
        }
    }

    public static abstract class UploadReceiver {
        protected abstract void success();

        protected abstract void failed(String errMsg);

        protected abstract void complete();
    }

    public static final String DATETIME_RECEIVED_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final double EARTH_RADIUS = 6378137;
    public static final DecimalFormat AREA_FORMAT = new DecimalFormat("#.##");

    public static int getUnsentCount(AppDatabase appDatabase) {
        return appDatabase.PTDao().selectPTPathsUnsentCount(LoggedUser.createFromAppDatabase(appDatabase).getId());
    }

    public static int getUploadingOpenedCount(AppDatabase appDatabase) {
        return appDatabase.PTDao().selectPTPathsUploadingOpenedCount(LoggedUser.createFromAppDatabase(appDatabase).getId());
    }

    public static PTPath createNew(AppDatabase appDatabase) {
        PTPath ptPath = new PTPath(LoggedUser.createFromAppDatabase(appDatabase).getId());
        return ptPath;
    }

    public static PTPath createFromAppDatabase(AppDatabase appDatabase, Long autoId) {
        PTPath ptPath = appDatabase.PTDao().selectPTPathByAutoId(autoId);
        if (ptPath != null) {
            ptPath.loadPoints(appDatabase);
            lazyCreateSettings(appDatabase, Arrays.asList(ptPath));
        }
        return ptPath;
    }

    public static PTPath createFromJSON(AppDatabase appDatabase, JSONObject jsonObject) throws JSONException {
        PTPath ptPath = new PTPath(LoggedUser.createFromAppDatabase(appDatabase).getId());
        ptPath.realId = jsonObject.getLong("id");
        ptPath.name = jsonObject.getString("name");
        ptPath.startT = DateTime.parse(jsonObject.getString("start"), DateTimeFormat.forPattern(DATETIME_RECEIVED_FORMAT));
        ptPath.endT = DateTime.parse(jsonObject.getString("end"), DateTimeFormat.forPattern(DATETIME_RECEIVED_FORMAT));
        if (jsonObject.has("area")) {
            ptPath.area = jsonObject.getDouble("area");
        }
        JSONArray jsonPoints = jsonObject.getJSONArray("points");
        for (int i = 0; i < jsonPoints.length(); ++i) {
            ptPath.points.add(PTPoint.createFromJSON(jsonPoints.getJSONObject(i), i));
        }
        lazyCreateSettings(appDatabase, Arrays.asList(ptPath));
        return ptPath;

    }

    public static List<PTPath> createListFromAppDatabase(AppDatabase appDatabase, boolean isLoadPoints) {
        List<PTPath> ptPaths = appDatabase.PTDao().selectAllPTPathsByUserId(LoggedUser.createFromAppDatabase(appDatabase).getId());
        if (isLoadPoints) {
            loadPoints(appDatabase, ptPaths);
        }
        lazyCreateSettings(appDatabase, ptPaths);
        return ptPaths;
    }

    public static List<PTPath> createListFromAppDatabaseUnsent(AppDatabase appDatabase) {
        List<PTPath> ptPaths = appDatabase.PTDao().selectPTPathsUnsent(LoggedUser.createFromAppDatabase(appDatabase).getId());
        loadPoints(appDatabase, ptPaths);
        lazyCreateSettings(appDatabase, ptPaths);
        return ptPaths;
    }

    private static void lazyCreateSettings(AppDatabase appDatabase, List<PTPath> paths) {
        for (PTPath ptPath : paths) {
            if (ptPath.area == null) {
                ptPath.calculateArea();
                ptPath.saveToDB(appDatabase);
            }
        }
    }

    private static void loadPoints(AppDatabase appDatabase, List<PTPath> ptPaths) {
        for (PTPath ptPath : ptPaths) {
            ptPath.loadPoints(appDatabase);
        }
    }

    static {
        AREA_FORMAT.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
    }

    @Ignore
    private List<PTPoint> points = new ArrayList<>();
    @Ignore
    private boolean isPointsLoaded = false;

    @PrimaryKey(autoGenerate = true)
    private Long autoId;
    // nullovost značí stav neodeslání (= i neúspěšné odeslání); ošetření duplicit je na straně serveru
    private Long realId;
    // = true - označuje stav nepotvrzeného odeslání (mezistav)
    private boolean isUploadingOpened = false;
    private boolean isLastSendFailed = false;
    @NonNull
    private String userId;
    private String name;
    private DateTime startT;
    private DateTime endT;
    private boolean byCentroids = false;
    private Double area;
    private String deviceManufacture;
    private String deviceModel;
    private String devicePlatform;
    private String deviceVersion;

    public PTPath(String userId) {
        this.userId = userId;
    }

    public void loadPoints(AppDatabase appDatabase) {
        points = appDatabase.PTDao().selectPTPointsByPathId(autoId);
        isPointsLoaded = true;
    }

    public void saveToDB(AppDatabase appDatabase) {
        autoId = appDatabase.PTDao().insertPTPath(this);
        for (PTPoint ptPoint : points) {
            ptPoint.setPathId(autoId);
            ptPoint.saveToDB(appDatabase);
        }
    }

    public void upload(AppDatabase appDatabase, Requestor requestor, UploadReceiver uploadReceiver) {
        isUploadingOpened = true;
        saveToDB(appDatabase);
        if (!isPointsLoaded) {
            loadPoints(appDatabase);
        }
        UploadExecutor uploadExecutor = this.new UploadExecutor(appDatabase, uploadReceiver);
        String errMsgTitle = "uploadPath failed (autoId = " + autoId + "; realId = " + realId + "; name = " + name + ")";
        requestor.requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_path.php", response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.has("status")) {
                    isUploadingOpened = false;
                    saveToDB(appDatabase);
                }
                String status = jsonObject.getString("status").trim();
                /* DEBUGCOM
                if (autoId == 474) {
                    status = "error";
                    jsonObject.put("error_msg", "pokusná chyba");
                }
                /**/
                if (!status.equals("ok")) {
                    String errMsg = jsonObject.getString("error_msg");
                    uploadExecutor.failed(errMsgTitle + ", bad status\n" + errMsg);
                    return;
                }
                setRealId(jsonObject.getLong("path_id"));
                uploadExecutor.success();
            } catch (JSONException jsonException) {
                uploadExecutor.failed(errMsgTitle + ", response json error\n" + jsonException.getMessage());
            } finally {
                Log.d("UPLOAD PATH", "ptPath: " + getAutoId() + "upload end");
            }
        }, error -> {
            uploadExecutor.failed(errMsgTitle + ", network error\n" + Util.volleyErrorMsg(error));
            Log.d("UPLOAD PATH", "ptPath: " + getAutoId() + "uploaded");
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                try {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", LoggedUser.createFromAppDatabase(appDatabase).getId());
                    params.put("name", getName());
                    params.put("start", getStartT().toString(DATETIME_RECEIVED_FORMAT));
                    params.put("end", getEndT().toString(DATETIME_RECEIVED_FORMAT));
                    params.put("area", getArea() == null ? "0" : AREA_FORMAT.format(getArea()));
                    params.put("deviceManufacture", getDeviceManufacture());
                    params.put("deviceModel", getDeviceModel());
                    params.put("devicePlatform", getDevicePlatform());
                    params.put("deviceVersion", getDeviceVersion());
                    params.put("points", pointsToJSONArray().toString());
                    return params;
                } catch (JSONException jsonException) {
                    this.forceCancel = true;
                    uploadExecutor.failed(errMsgTitle + ", params json error\n" + jsonException.getMessage());
                    Log.d("UPLOAD PATH", "ptPath: " + getAutoId() + "uploaded");
                    return null;
                }
            }
        });
    }

    public boolean isLocked() {
        return isLastSendFailed;
    }

    public void addPoint(Location location) {
        PTPoint ptPoint = PTPoint.createNew(location, points.size());
        points.add(ptPoint);
    }

    public void addPoints(List<Location> locations) {
        for (Location location : locations) {
            addPoint(location);
        }
    }

    public void delete(AppDatabase appDatabase) {
        appDatabase.PTDao().deletePTPath(this);
    }

    public boolean isSent() {
        return realId != null;
    }

    public JSONArray pointsToJSONArray() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (PTPoint ptPoint : points) {
            jsonArray.put(ptPoint.toJSON());
        }
        return jsonArray;
    }

    public void calculateArea() {
        if (points.size() < 3) {
            return;
        }

        double area = 0;
        for (int i = 0; i < points.size(); ++i) {
            PTPoint p1 = points.get(i > 0 ? i - 1 : points.size() - 1);
            PTPoint p2 = points.get(i);
            area += Math.toRadians(p2.getLongitude() - p1.getLongitude()) * (2 + Math.sin(Math.toRadians(p1.getLatitude())) + Math.sin(Math.toRadians(p2.getLatitude())));
        }
        area = -(area * EARTH_RADIUS * EARTH_RADIUS / 2);
        this.area = Math.max(area, -area);
    }

    public void setDeviceInfos() {
        deviceManufacture = Util.getPhoneManufacturer();
        deviceModel = Util.getPhoneModel();
        devicePlatform = Util.getOSName();
        deviceVersion = Util.getOSVersion();
    }

    // region get, set
    public List<PTPoint> getPoints() {
        return points;
    }

    public Long getAutoId() {
        return autoId;
    }

    public void setAutoId(Long autoId) {
        this.autoId = autoId;
    }

    public Long getRealId() {
        return realId;
    }

    public void setRealId(Long realId) {
        this.realId = realId;
    }

    public boolean isUploadingOpened() {
        return isUploadingOpened;
    }

    public void setUploadingOpened(boolean uploadingOpened) {
        isUploadingOpened = uploadingOpened;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DateTime getStartT() {
        return startT;
    }

    public void setStartT(DateTime startT) {
        this.startT = startT;
    }

    public DateTime getEndT() {
        return endT;
    }

    public void setEndT(DateTime endT) {
        this.endT = endT;
    }

    public boolean isByCentroids() {
        return byCentroids;
    }

    public void setByCentroids(boolean byCentroids) {
        this.byCentroids = byCentroids;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public String getDeviceManufacture() {
        return deviceManufacture;
    }

    public void setDeviceManufacture(String deviceManufacture) {
        this.deviceManufacture = deviceManufacture;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDevicePlatform() {
        return devicePlatform;
    }

    public void setDevicePlatform(String devicePlatform) {
        this.devicePlatform = devicePlatform;
    }

    public String getDeviceVersion() {
        return deviceVersion;
    }

    public void setDeviceVersion(String deviceVersion) {
        this.deviceVersion = deviceVersion;
    }

    public boolean isLastSendFailed() {
        return isLastSendFailed;
    }

    public void setLastSendFailed(boolean lastSendFailed) {
        isLastSendFailed = lastSendFailed;
    }

    public boolean isPointsLoaded() {
        return isPointsLoaded;
    }

    // endregion


}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
