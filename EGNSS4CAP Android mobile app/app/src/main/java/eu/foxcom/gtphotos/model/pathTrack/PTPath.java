package eu.foxcom.gtphotos.model.pathTrack;

import android.location.Location;

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
import java.util.List;
import java.util.Locale;

import eu.foxcom.gtphotos.model.AppDatabase;
import eu.foxcom.gtphotos.model.LoggedUser;

@Entity(indices = {@Index(value = {"realId"}, unique = true)})
public class PTPath {

    public static final String DATETIME_RECEIVED_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final double EARTH_RADIUS = 6378137;
    public static final DecimalFormat AREA_FORMAT = new DecimalFormat("#.##");

    public static PTPath createNew(AppDatabase appDatabase) {
        PTPath ptPath = new PTPath(LoggedUser.createFromAppDatabase(appDatabase).getId());
        return ptPath;
    }

    public static PTPath createFromAppDatabase(AppDatabase appDatabase, Long autoId) {
        PTPath ptPath = appDatabase.PTDao().selectPTPathByAutoId(autoId);
        if (ptPath != null) {
            ptPath.loadPoints(appDatabase);
        }
        lazyCreateSettings(appDatabase, Arrays.asList(ptPath));
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

    public static List<PTPath> createListFromAppDatabase(AppDatabase appDatabase) {
        List<PTPath> ptPaths = appDatabase.PTDao().selectAllPTPathsByUserId(LoggedUser.createFromAppDatabase(appDatabase).getId());
        loadPoints(appDatabase, ptPaths);
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
        for (PTPath ptPath: paths) {
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

    @PrimaryKey(autoGenerate = true)
    private Long autoId;
    // nullovost značí stav neodeslání (= i neúspěšné odeslání); ošetření duplicit je na straně serveru
    private Long realId;
    @NonNull
    private String userId;
    private String name;
    private DateTime startT;
    private DateTime endT;
    private boolean byCentroids = false;
    private Double area;

    public PTPath(String userId) {
        this.userId = userId;
    }

    private void loadPoints(AppDatabase appDatabase) {
        points = appDatabase.PTDao().selectPTPointsByPathId(autoId);
    }

    public void saveToDB(AppDatabase appDatabase) {
        autoId = appDatabase.PTDao().insertPTPath(this);
        for (PTPoint ptPoint : points) {
            ptPoint.setPathId(autoId);
            ptPoint.saveToDB(appDatabase);
        }
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

    // endregion


}
