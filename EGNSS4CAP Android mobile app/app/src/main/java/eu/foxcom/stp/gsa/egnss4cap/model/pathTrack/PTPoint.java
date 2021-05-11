package eu.foxcom.stp.gsa.egnss4cap.model.pathTrack;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import eu.foxcom.stp.gsa.egnss4cap.model.AppDatabase;

@Entity (foreignKeys = @ForeignKey(entity = PTPath.class, parentColumns = "autoId", childColumns = "pathId", onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE))
public class PTPoint {

    public static final String DATETIME_RECEIVED_FORMAT = "yyyy-MM-dd HH:mm:ss";

    static PTPoint createNew(Location location, Integer index) {
        PTPoint ptPoint = new PTPoint();
        ptPoint.index = index;
        ptPoint.created = new DateTime(location.getTime());
        ptPoint.latitude = location.getLatitude();
        ptPoint.longitude = location.getLongitude();
        ptPoint.altitude = location.getAltitude();
        ptPoint.accuracy = (double) location.getAccuracy();
        return ptPoint;
    }

    public static PTPoint createFromAppDatabase(AppDatabase appDatabase, Long autoId) {
        return appDatabase.PTDao().selectPTPointByAutoId(autoId);
    }

    public static PTPoint createFromJSON(JSONObject jsonObject, Integer index) throws JSONException {
        PTPoint ptPoint = new PTPoint();
        ptPoint.index = index;
        ptPoint.created = DateTime.parse(jsonObject.getString("created"), DateTimeFormat.forPattern(DATETIME_RECEIVED_FORMAT));
        ptPoint.latitude = jsonObject.getDouble("lat");
        ptPoint.longitude = jsonObject.getDouble("lng");
        try {
            ptPoint.altitude = jsonObject.getDouble("altitude");
        } catch (JSONException jsonException) {
            ptPoint.altitude = null;
        }
        try {
            ptPoint.accuracy = jsonObject.getDouble("accuracy");
        }catch (JSONException jsonException) {
            ptPoint.accuracy = null;
        }
        return ptPoint;
    }

    @PrimaryKey(autoGenerate = true)
    private Long autoId;
    @NonNull
    @ColumnInfo(index = true)
    private Long pathId;
    private Integer index;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double accuracy;
    private DateTime created;

    public void saveToDB(AppDatabase appDatabase) {
        appDatabase.PTDao().insertPTPoint(this);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lat", latitude);
        jsonObject.put("lng", longitude);
        jsonObject.put("altitude", altitude);
        jsonObject.put("accuracy", accuracy);
        jsonObject.put("created", created.toString(DATETIME_RECEIVED_FORMAT));
        return jsonObject;
    }

    // region get, set

    public Long getAutoId() {
        return autoId;
    }

    public void setAutoId(Long autoId) {
        this.autoId = autoId;
    }

    @NonNull
    public Long getPathId() {
        return pathId;
    }

    public void setPathId(@NonNull Long pathId) {
        this.pathId = pathId;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
