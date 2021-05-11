package eu.foxcom.stp.gsa.egnss4cap.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Phaser;

import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfCalculationModule;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfController;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfData;

@Entity(indices = {@Index(value = {"realId"}, unique = true)})
public class Photo {

    public static abstract class UpdatePhotoReceiver extends UpdateReceiver {

        private boolean isAutoSuccessDBSave = true;

        public UpdatePhotoReceiver() {
            super();
        }

        public UpdatePhotoReceiver(SyncQueue syncQueue) {
            super(syncQueue);
        }

        public UpdatePhotoReceiver(Phaser phaser) {
            super(phaser);
        }

        protected final void successExec(AppDatabase appDatabase, Photo photo) {
            if (isAutoSuccessDBSave) {
                photo.refreshToDB(appDatabase);
            }
            success(appDatabase, photo);
            finishExec(true);
        }

        protected abstract void success(AppDatabase appDatabase, Photo photo);

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

    // otherwise to database
    public static final boolean TO_FILE = true;

    public static final String TAG = Photo.class.getSimpleName();
    public static final String DATETIME_RECEIVED_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final int MAX_WIDTH = 1080;
    public static final int MAX_HEIGHT = 1080;
    public static final int COMPRESS_QUALITY = 100;

    private static final String KEY = "bfb576892e43b763731a1596c428987893b2e76ce1be10f733";

    @Ignore
    private Context context;

    /* interní id, neodpovídá serverové databázi*/
    @PrimaryKey(autoGenerate = true)
    private Long id;
    private Long realId;
    private String taskId;
    private String userId;
    private boolean isSent = false;
    private Double lat;
    private Double lng;
    private DateTime created;
    private int indx;
    private String photoPath;

    private Double accuracy;
    private Double altitude;
    private Double bearing;
    private Double azimMagneticField;
    private Double photoHeading;
    private Double pitch;
    private Double roll;
    private Double tilt;
    private Integer orientation;
    private Double horizontalViewAngle;
    private Double verticalViewAngle;
    private String deviceManufacture;
    private String deviceModel;
    private String devicePlatform;
    private String deviceVersion;
    private JSONArray satsInfo;
    private Integer extraSatCount;
    private String NMEAMessage;
    private JSONObject networkInfo;
    private Double centroidLat;
    private Double centroidLng;
    /* EKF data */
    private Double efkLatGpsL1;
    private Double efkLatGpsL5;
    private Double efkLatGpsIf;
    private Double efkLatGalE1;
    private Double efkLatGalE5;
    private Double efkLatGalIf;

    private Double efkLngGpsL1;
    private Double efkLngGpsL5;
    private Double efkLngGpsIf;
    private Double efkLngGalE1;
    private Double efkLngGalE5;
    private Double efkLngGalIf;

    private Double efkAltGpsL1;
    private Double efkAltGpsL5;
    private Double efkAltGpsIf;
    private Double efkAltGalE1;
    private Double efkAltGalE5;
    private Double efkAtlGalIf;

    private DateTime efkTimeGpsL1;
    private DateTime efkTimeGpsL5;
    private DateTime efkTimeGpsIf;
    private DateTime efkTimeGalE1;
    private DateTime efkTimeGalE5;
    private DateTime efkTimeGalIf;
    /**/
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    private byte[] photoBytes;
    private String digest;
    /* použit jen pro bezprizorní fotografie */
    private String note;
    // indikace chyby při posledním odesílání na server
    // přidává mezistav pro nové odeslání samostatné fotografie bez možnosti změn
    // pro fotografie v tasku není využíváno (nutný refaktor celé synchronizace)
    @ColumnInfo(defaultValue = "0")
    private boolean isLastSendFailed = false;

    public static Photo createFromAppDatabase(long photoId, AppDatabase appDatabase, Context context) {
        Photo photo = appDatabase.photoDao().selectPhotoById(photoId);
        photo.context = context;
        return photo;
    }

    public static Photo createFromResponse(JSONObject jsonObject, Long realId, String taskId, int indx, Context context, AppDatabase appDatabase) throws JSONException, IOException {
        Double lat = jsonObject.isNull("lat") ? null : jsonObject.getDouble("lat");
        Double lng = jsonObject.isNull("lng") ? null : jsonObject.getDouble("lng");
        DateTime created = jsonObject.isNull("created") ? null : DateTime.parse(jsonObject.getString("created"), DateTimeFormat.forPattern(DATETIME_RECEIVED_FORMAT));
        String base64 = jsonObject.getString("photo");
        String userId = LoggedUser.createFromAppDatabase(appDatabase).getId();
        String path = null;
        byte[] photoBytes = null;
        ByteArrayInputStream stream = null;
        File file = null;
        ExifInterface exifInterface;
        if (TO_FILE) {
            path = savePhotoFileFromBase64(base64, createPhotoName(taskId, indx, userId), context);
            exifInterface = new ExifInterface(path);
        } else {
            photoBytes = Base64.decode(base64, Base64.DEFAULT);
            stream = new ByteArrayInputStream(photoBytes);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                exifInterface = new ExifInterface(stream);
            } else {
                file = createPhotoFile(context, taskId, indx, photoBytes);
                exifInterface = new ExifInterface(file.getAbsolutePath());
            }
        }
        String altitudeS = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
        String azimS = exifInterface.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION);
        Double altitude = null;
        Double azim = null;
        if (altitudeS != null) {
            altitude = ExifUtil.fromRational64u(altitudeS);
        }
        if (azimS != null) {
            azim = ExifUtil.fromRational64u(azimS);
        }
        Double photoHeading = null;
        if (jsonObject.has("photo_heading") && !jsonObject.isNull("photo_heading")) {
            photoHeading = jsonObject.getDouble("photo_heading");
        }
        Photo photo = new Photo(realId, taskId, userId, lat, lng, altitude, azim, photoHeading, created, path, photoBytes, indx, context);
        photo.isSent = true;
        String note = null;
        if (jsonObject.has("note") && !jsonObject.isNull("note")) {
            note = jsonObject.getString("note");
        }
        photo.note = note;
        if (stream != null) {
            stream.close();
        }
        if (file != null) {
            file.delete();
        }
        photo.digest = jsonObject.getString("digest");
        return createBaseFactory(photo);
    }

    public static Photo createFromPhotoDataController(String taskId, PhotoDataController photoDataController, DateTime createdDateTime, String photoPathTemp, Context context, AppDatabase appDatabase) throws IOException, JSONException {
        Photo photo = new Photo();
        Location location = photoDataController.getLocation();
        photo.taskId = taskId;
        String userId = LoggedUser.createFromAppDatabase(appDatabase).getId();
        photo.userId = userId;
        photo.context = context;
        photo.lat = location.getLatitude();
        photo.lng = location.getLongitude();
        photo.created = createdDateTime;
        int indx = findNewIndx(taskId, appDatabase);
        if (TO_FILE) {
            photo.photoPath = savePhotoFileFromPath(photoPathTemp, createPhotoName(taskId, indx, userId), context);
        } else {
            photo.photoBytes = createBytesPhotoFromPath(photoPathTemp);
        }
        photo.indx = indx;
        photo.isSent = false;

        // pouze horizontální
        photo.accuracy = Double.valueOf(location.getAccuracy());
        photo.altitude = location.getAltitude();
        photo.bearing = Double.valueOf(location.getBearing());
        photo.azimMagneticField = photoDataController.getPositionSensorController().getAzimuthDegreesAverage();
        photo.photoHeading = photoDataController.computePhotoHeading(photoDataController.getLastScreenRotation(), photoDataController.getLastTilt(), photo.azimMagneticField);
        photo.pitch = photoDataController.getPositionSensorController().getPitchDegreesAverage();
        photo.roll = photoDataController.getPositionSensorController().getRollDegreesAverage();
        int orientation = ExifUtil.toExifOrientation(
                photoDataController.getPositionSensorController().getPitchDegreesAverage(),
                photoDataController.getPositionSensorController().getRollDegreesAverage());
        // exif orientation (zrcadlení nebude podporováno)
        /* -90° CameraX bug -> oprava  s garancí pro samsung a MI 9 */
        int rotation = ExifUtil.getExifOrientationRotation(orientation);
        rotation = (rotation + 90) % 360;
        orientation = ExifUtil.toExifOrientation(rotation);
        /**/
        photo.orientation = orientation;
        photo.horizontalViewAngle = Math.toDegrees(photoDataController.getCameraController().calculateFOV()[0]);
        photo.verticalViewAngle = Math.toDegrees(photoDataController.getCameraController().calculateFOV()[1]);
        photo.deviceManufacture = Util.getPhoneManufacturer();
        photo.deviceModel = Util.getPhoneModel();
        photo.devicePlatform = Util.getOSName();
        photo.deviceVersion = Util.getOSVersion();
        photo.networkInfo = photoDataController.getNetworkInfoData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            photo.NMEAMessage = photoDataController.getNmeaParser().getNmeaTotalMessage();
            photo.extraSatCount = photoDataController.getExtraSatNumber();
            photo.satsInfo = photoDataController.getNmeaParser().getSNRSatellites().toCurrSatsInfo();
        }
        photo.centroidLat = photoDataController.getCentroidLatitude();
        photo.centroidLng = photoDataController.getCentroidLongitude();
        photo.tilt = photoDataController.computeTilt(photoDataController.getLastScreenRotation());

        /* ekf data */
        EkfController ekfController = photoDataController.getEkfController();
        EkfData gpsL1 = ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GPS_L1).getEkfData();
        EkfData gpsL5 = ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GPS_L5).getEkfData();
        EkfData gpsIf = ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GPS_IF).getEkfData();
        EkfData galileoE1 = ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GALILEO_E1).getEkfData();
        EkfData galileoE5 = ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GALILEO_E5A).getEkfData();
        EkfData galileoIf = ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GALILEO_IF).getEkfData();
        if (gpsL1 != null) {
            photo.efkLatGpsL1 = gpsL1.getLatitude();
            photo.efkLngGpsL1 = gpsL1.getLongitude();
            photo.efkAltGpsL1 = gpsL1.getAltitude();
            photo.efkTimeGpsL1 = gpsL1.getReferenceTime();
        }
        if (gpsL5 != null) {
            photo.efkLatGpsL5 = gpsL5.getLatitude();
            photo.efkLngGpsL5 = gpsL5.getLongitude();
            photo.efkAltGpsL5 = gpsL5.getAltitude();
            photo.efkTimeGpsL5 = gpsL5.getReferenceTime();
        }
        if (gpsIf != null) {
            photo.efkLatGpsIf = gpsIf.getLatitude();
            photo.efkLngGpsIf = gpsIf.getLongitude();
            photo.efkAltGpsIf = gpsIf.getAltitude();
            photo.efkTimeGpsIf = gpsIf.getReferenceTime();
        }
        if (galileoE1 != null) {
            photo.efkLatGalE1 = galileoE1.getLatitude();
            photo.efkLngGalE1 = galileoE1.getLongitude();
            photo.efkAltGalE1 = galileoE1.getAltitude();
            photo.efkTimeGalE1 = galileoE1.getReferenceTime();
        }

        if (galileoE5 != null) {
            photo.efkLatGalE5 = galileoE5.getLatitude();
            photo.efkLngGalE5 = galileoE5.getLongitude();
            photo.efkAltGalE5 = galileoE5.getAltitude();
            photo.efkTimeGalE5 = galileoE5.getReferenceTime();
        }
        if (galileoIf != null) {
            photo.efkLatGalIf = galileoIf.getLatitude();
            photo.efkLngGalIf = galileoIf.getLongitude();
            photo.efkAtlGalIf = galileoIf.getAltitude();
            photo.efkTimeGalIf = galileoIf.getReferenceTime();
        }
        /**/

        DateTime original = photo.created;
        photo.saveToExif(original);

        photo.burnDigest(appDatabase);

        return createBaseFactory(photo);
    }

    public static int findNewIndx(String taskId, AppDatabase appDatabase) {
        Integer maxIndx;
        if (taskId != null) {
            maxIndx = appDatabase.photoDao().selectMaxIndx(taskId);
        } else {
            maxIndx = appDatabase.photoDao().selectUnownedMaxIndx();
        }
        if (maxIndx == null) {
            return 0;
        } else {
            return maxIndx + 1;
        }
    }

    private static Photo createBaseFactory(Photo photo) throws IOException {
        return photo;
    }

    public static String createPhotoName(String taskId, int indx, String userId) {
        if (taskId != null) {
            return "photo_" + taskId + "_" + indx + ".data";
        } else {
            return "photo_unowned_" + userId + "_" + indx + ".data";
        }
    }

    private static String createPhotoNameTemp(String taskId, int indx) {
        if (taskId != null) {
            return "photo_temp_" + taskId + "_" + indx + ".data";
        } else {
            return "photo_unowned_temp_" + taskId + "_" + indx + ".data";
        }
    }

    private static String savePhotoFileFromBase64(String base64, String photoName, Context context) throws IOException {
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(
                storageDir,
                photoName
        );
        String absPath = image.getAbsolutePath();
        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
        FileOutputStream fileOutputStream = new FileOutputStream(image);
        fileOutputStream.write(decodedString);
        fileOutputStream.flush();
        fileOutputStream.close();
        return absPath;
    }

    private static String savePhotoFileFromPath(String photoPathTemp, String photoName, Context context) throws IOException {
        byte[] bytes = createBytesPhotoFromPath(photoPathTemp);
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, photoName);
        FileOutputStream fileOutputStream = new FileOutputStream(image);
        fileOutputStream.write(bytes);
        fileOutputStream.flush();
        fileOutputStream.close();
        return image.getAbsolutePath();
    }

    private static byte[] createBytesPhotoFromPath(String photoPathTemp) {
        Bitmap bitmap = BitmapFactory.decodeFile(photoPathTemp);
        float scale = Math.min(((float) MAX_HEIGHT / bitmap.getWidth()), ((float) MAX_WIDTH / bitmap.getHeight()));
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, stream);
        byte[] bytes = stream.toByteArray();
        return bytes;
    }

    private static File createPhotoFile(Context context, String taskId, int indx, byte[] photoBytes) throws IOException {
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, createPhotoNameTemp(taskId, indx));
        FileOutputStream fileOutputStream = new FileOutputStream(image);
        fileOutputStream.write(photoBytes);
        fileOutputStream.flush();
        fileOutputStream.close();
        return image;
    }

    public static int numberOfPhotos(AppDatabase appDatabase, String userId) {
        return appDatabase.photoDao().selectCountOfAllPhotos(userId);
    }

    private Photo(Long realId, String taskId, String userId, Double lat, Double lng, Double altitude, Double azimuth, Double photoHeading, DateTime created, String photoPath, byte[] photoBytes, int indx, Context context) throws IOException {
        this.realId = realId;
        this.taskId = taskId;
        this.userId = userId;
        this.lat = lat;
        this.lng = lng;
        this.altitude = altitude;
        this.azimMagneticField = azimuth;
        this.photoHeading = photoHeading;
        this.created = created;
        this.photoPath = photoPath;
        this.photoBytes = photoBytes;
        this.indx = indx;
        this.context = context;
    }

    public Photo() {
    }

    public void refreshToDB(AppDatabase appDatabase) {
        id = appDatabase.photoDao().insertPhoto(this);
    }

    public Bitmap getBitmap() {
        if (TO_FILE) {
            File imgFile = new File(photoPath);
            if (!imgFile.exists()) {
                return null;
            }
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        } else {
            return BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
        }
    }

    private File createPhotoFile() throws IOException {
        return createPhotoFile(context, taskId, indx, photoBytes);
    }

    private void removePhotoFile(File file) {
        file.delete();
    }

    private void refreshBytesFromFile(File file) throws IOException {
        photoBytes = FileUtils.readFileToByteArray(file);
    }

    public Bitmap getRotatedBitmap() throws IOException {
        Bitmap bitmap = getBitmap();
        if (bitmap == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        int orientation = 0;
        ExifInterface exifInterface;
        ByteArrayInputStream stream = null;
        File file = null;
        if (TO_FILE) {
            exifInterface = new ExifInterface(photoPath);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stream = new ByteArrayInputStream(photoBytes);
                exifInterface = new ExifInterface(stream);
            } else {
                file = createPhotoFile();
                exifInterface = new ExifInterface(file.getAbsolutePath());
            }
        }
        String orientationS = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
        if (orientationS != null) {
            orientation = Integer.valueOf(orientationS);
        }
        int rotation = ExifUtil.getExifOrientationRotation(orientation);
        matrix.postRotate(rotation);
        if (stream != null) {
            stream.close();
        }
        if (file != null) {
            removePhotoFile(file);
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void saveToExif(DateTime original) throws IOException {
        ExifInterface exifInterface;
        File file = null;
        if (TO_FILE) {
            exifInterface = new ExifInterface(photoPath);
        } else {
            file = createPhotoFile();
            exifInterface = new ExifInterface(file.getAbsolutePath());
        }
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, ExifUtil.toGPSFormat(lat));
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, ExifUtil.toLatitudeRef(lat));
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, ExifUtil.toGPSFormat(lng));
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, ExifUtil.toLongitudeRef(lng));
        exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, ExifUtil.toRational64u(altitude));
        exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, ExifUtil.toAltitudeRef(altitude));
        exifInterface.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, ExifUtil.toRational64u(azimMagneticField));
        exifInterface.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
        exifInterface.setAttribute(ExifInterface.TAG_GPS_DOP, ExifUtil.toRational64u(accuracy));
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, ExifUtil.toDateTime(original));
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, ExifUtil.toDateTime(original));
        exifInterface.setAttribute(ExifInterface.TAG_DATETIME, ExifUtil.toDateTime(created));
        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, String.valueOf(deviceManufacture));
        exifInterface.setAttribute(ExifInterface.TAG_MODEL, String.valueOf(deviceModel));
        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, "TASK_" + taskId + "_DATETIME_" + created);
        exifInterface.saveAttributes();
        if (file != null) {
            refreshBytesFromFile(file);
            removePhotoFile(file);
        }
    }

    public void readExif() throws IOException {
        ExifInterface exifInterface;
        File file = null;
        ByteArrayInputStream stream = null;
        if (TO_FILE) {
            exifInterface = new ExifInterface(photoPath);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stream = new ByteArrayInputStream(photoBytes);
                exifInterface = new ExifInterface(new ByteArrayInputStream(photoBytes));
            } else {
                file = createPhotoFile();
                exifInterface = new ExifInterface(file.getAbsolutePath());
            }
        }
        String lat = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String latRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        String lng = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        String lngRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
        String alt = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
        String altRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF);
        String dir = exifInterface.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION);
        String dirRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF);
        String dop = exifInterface.getAttribute(ExifInterface.TAG_GPS_DOP);
        String orig = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
        String digi = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
        String dt = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
        String com = exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT);
        String ori = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
        if (stream != null) {
            stream.close();
        }
        if (file != null) {
            removePhotoFile(file);
        }
    }

    public String getBASE64Photo() throws Exception {
        if (TO_FILE) {
            File image = new File(photoPath);
            return Base64.encodeToString(org.apache.commons.io.FileUtils.readFileToByteArray(image), Base64.DEFAULT);
        } else {
            return Base64.encodeToString(photoBytes, Base64.DEFAULT);
        }
    }


    public JSONObject toJSONObject() throws JSONException, Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lat", lat);
        jsonObject.put("lng", lng);
        jsonObject.put("centroidLat", centroidLat);
        jsonObject.put("centroidLng", centroidLng);
        jsonObject.put("altitude", altitude);
        jsonObject.put("bearing", bearing);
        jsonObject.put("magnetic_azimuth", azimMagneticField);
        jsonObject.put("photo_heading", photoHeading);
        jsonObject.put("pitch", pitch);
        jsonObject.put("roll", roll);
        jsonObject.put("photo_angle", tilt);
        jsonObject.put("orientation", orientation);
        jsonObject.put("horizontalViewAngle", horizontalViewAngle);
        jsonObject.put("verticalViewAngle", verticalViewAngle);
        jsonObject.put("accuracy", accuracy);
        jsonObject.put("created", created.toString());
        jsonObject.put("deviceManufacture", deviceManufacture);
        jsonObject.put("deviceModel", deviceModel);
        jsonObject.put("devicePlatform", devicePlatform);
        jsonObject.put("deviceVersion", deviceVersion);
        jsonObject.put("satsInfo", satsInfo);
        jsonObject.put("extraSatCount", extraSatCount);
        jsonObject.put("NMEAMessage", NMEAMessage);
        jsonObject.put("networkInfo", networkInfo);
        jsonObject.put("photo", getBASE64Photo());
        jsonObject.put("digest", digest);
        jsonObject.put("note", note);
        /* ekf data */
        jsonObject.put("efkLatGpsL1", efkLatGpsL1);
        jsonObject.put("efkLatGpsL5", efkLatGpsL5);
        jsonObject.put("efkLatGpsIf", efkLatGpsIf);
        jsonObject.put("efkLatGalE1", efkLatGalE1);
        jsonObject.put("efkLatGalE5", efkLatGalE5);
        jsonObject.put("efkLatGalIf", efkLatGalIf);

        jsonObject.put("efkLngGpsL1", efkLngGpsL1);
        jsonObject.put("efkLngGpsL5", efkLngGpsL5);
        jsonObject.put("efkLngGpsIf", efkLngGpsIf);
        jsonObject.put("efkLngGalE1", efkLngGalE1);
        jsonObject.put("efkLngGalE5", efkLngGalE5);
        jsonObject.put("efkLngGalIf", efkLngGalIf);

        jsonObject.put("efkAltGpsL1", efkAltGpsL1);
        jsonObject.put("efkAltGpsL5", efkAltGpsL5);
        jsonObject.put("efkAltGpsIf", efkAltGpsIf);
        jsonObject.put("efkAltGalE1", efkAltGalE1);
        jsonObject.put("efkAltGalE5", efkAltGalE5);
        jsonObject.put("efkAtlGalIf", efkAtlGalIf);

        jsonObject.put("efkTimeGpsL1", efkTimeGpsL1 == null ? null : efkTimeGpsL1.toString());
        jsonObject.put("efkTimeGpsL1", efkTimeGpsL5 == null ? null : efkTimeGpsL5.toString());
        jsonObject.put("efkTimeGpsIf", efkTimeGpsIf == null ? null : efkTimeGpsIf.toString());
        jsonObject.put("efkTimeGalE1", efkTimeGalE1 == null ? null : efkTimeGalE1.toString());
        jsonObject.put("efkTimeGalE5", efkTimeGalE5 == null ? null : efkTimeGalE5.toString());
        jsonObject.put("efkTimeGalIf", efkTimeGalIf == null ? null : efkTimeGalIf.toString());
        /**/
        return jsonObject;
    }

    public void delete(AppDatabase appDatabase) {
        appDatabase.photoDao().deletePhoto(this);
    }

    private void burnDigest(AppDatabase appDatabase) {
        if (digest != null) {
            throw new RuntimeException("An attempt was made to change the digest of a photo.");
        }
        try {
            File file = new File(photoPath);
            String fileHash = Digest.hashFileStream(file);
            String createdS = created == null ? "0" : created.toString();
            String userId = LoggedUser.createFromAppDatabase(appDatabase).getId();
            String toHash = KEY + "_" + fileHash + "_" + createdS + "_" + userId;
            digest = Digest.hashStringToHexString(toHash);
        } catch (NoSuchAlgorithmException | IOException e) {
            PrintWriter printWriter = new PrintWriter(new StringWriter());
            e.printStackTrace(printWriter);
            digest = "Unexpected error: \n" + printWriter.toString();
        }
    }

    // (v uživatelském rámci)
    public boolean isEditable() {
        return !isSent && !isLastSendFailed;
    }

    // (v uživatelském rámci)
    // musí splňovat implikaci isEditable = true => isSendable = true
    public boolean isSendable() {
        return !isSent;
    }

    // region get, set

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public int getIndx() {
        return indx;
    }

    public void setIndx(int indx) {
        this.indx = indx;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getBearing() {
        return bearing;
    }

    public void setBearing(Double bearing) {
        this.bearing = bearing;
    }

    public Double getAzimMagneticField() {
        return azimMagneticField;
    }

    public void setAzimMagneticField(Double azimMagneticField) {
        this.azimMagneticField = azimMagneticField;
    }

    public Double getPitch() {
        return pitch;
    }

    public void setPitch(Double pitch) {
        this.pitch = pitch;
    }

    public Double getRoll() {
        return roll;
    }

    public void setRoll(Double roll) {
        this.roll = roll;
    }

    public Integer getOrientation() {
        return orientation;
    }

    public void setOrientation(Integer orientation) {
        this.orientation = orientation;
    }

    public Double getHorizontalViewAngle() {
        return horizontalViewAngle;
    }

    public void setHorizontalViewAngle(Double horizontalViewAngle) {
        this.horizontalViewAngle = horizontalViewAngle;
    }

    public Double getVerticalViewAngle() {
        return verticalViewAngle;
    }

    public void setVerticalViewAngle(Double verticalViewAngle) {
        this.verticalViewAngle = verticalViewAngle;
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

    public JSONArray getSatsInfo() {
        return satsInfo;
    }

    public void setSatsInfo(JSONArray satsInfo) {
        this.satsInfo = satsInfo;
    }

    public Integer getExtraSatCount() {
        return extraSatCount;
    }

    public void setExtraSatCount(Integer extraSatCount) {
        this.extraSatCount = extraSatCount;
    }

    public String getNMEAMessage() {
        return NMEAMessage;
    }

    public void setNMEAMessage(String NMEAMessage) {
        this.NMEAMessage = NMEAMessage;
    }

    public JSONObject getNetworkInfo() {
        return networkInfo;
    }

    public void setNetworkInfo(JSONObject networkInfo) {
        this.networkInfo = networkInfo;
    }

    public Double getCentroidLat() {
        return centroidLat;
    }

    public void setCentroidLat(Double centroidLat) {
        this.centroidLat = centroidLat;
    }

    public Double getCentroidLng() {
        return centroidLng;
    }

    public void setCentroidLng(Double centroidLng) {
        this.centroidLng = centroidLng;
    }

    public Double getPhotoHeading() {
        return photoHeading;
    }

    public void setPhotoHeading(Double photoHeading) {
        this.photoHeading = photoHeading;
    }

    public byte[] getPhotoBytes() {
        return photoBytes;
    }

    public void setPhotoBytes(byte[] photoBytes) {
        this.photoBytes = photoBytes;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isLastSendFailed() {
        return isLastSendFailed;
    }

    public void setLastSendFailed(boolean lastSendFailed) {
        isLastSendFailed = lastSendFailed;
    }

    public Double getTilt() {
        return tilt;
    }

    public void setTilt(Double tilt) {
        this.tilt = tilt;
    }

    public Double getEfkLatGpsL1() {
        return efkLatGpsL1;
    }

    public void setEfkLatGpsL1(Double efkLatGpsL1) {
        this.efkLatGpsL1 = efkLatGpsL1;
    }

    public Double getEfkLatGpsL5() {
        return efkLatGpsL5;
    }

    public void setEfkLatGpsL5(Double efkLatGpsL5) {
        this.efkLatGpsL5 = efkLatGpsL5;
    }

    public Double getEfkLatGpsIf() {
        return efkLatGpsIf;
    }

    public void setEfkLatGpsIf(Double efkLatGpsIf) {
        this.efkLatGpsIf = efkLatGpsIf;
    }

    public Double getEfkLatGalE1() {
        return efkLatGalE1;
    }

    public void setEfkLatGalE1(Double efkLatGalE1) {
        this.efkLatGalE1 = efkLatGalE1;
    }

    public Double getEfkLatGalE5() {
        return efkLatGalE5;
    }

    public void setEfkLatGalE5(Double efkLatGalE5) {
        this.efkLatGalE5 = efkLatGalE5;
    }

    public Double getEfkLatGalIf() {
        return efkLatGalIf;
    }

    public void setEfkLatGalIf(Double efkLatGalIf) {
        this.efkLatGalIf = efkLatGalIf;
    }

    public Double getEfkLngGpsL1() {
        return efkLngGpsL1;
    }

    public void setEfkLngGpsL1(Double efkLngGpsL1) {
        this.efkLngGpsL1 = efkLngGpsL1;
    }

    public Double getEfkLngGpsL5() {
        return efkLngGpsL5;
    }

    public void setEfkLngGpsL5(Double efkLngGpsL5) {
        this.efkLngGpsL5 = efkLngGpsL5;
    }

    public Double getEfkLngGpsIf() {
        return efkLngGpsIf;
    }

    public void setEfkLngGpsIf(Double efkLngGpsIf) {
        this.efkLngGpsIf = efkLngGpsIf;
    }

    public Double getEfkLngGalE1() {
        return efkLngGalE1;
    }

    public void setEfkLngGalE1(Double efkLngGalE1) {
        this.efkLngGalE1 = efkLngGalE1;
    }

    public Double getEfkLngGalE5() {
        return efkLngGalE5;
    }

    public void setEfkLngGalE5(Double efkLngGalE5) {
        this.efkLngGalE5 = efkLngGalE5;
    }

    public Double getEfkLngGalIf() {
        return efkLngGalIf;
    }

    public void setEfkLngGalIf(Double efkLngGalIf) {
        this.efkLngGalIf = efkLngGalIf;
    }

    public Double getEfkAltGpsL1() {
        return efkAltGpsL1;
    }

    public void setEfkAltGpsL1(Double efkAltGpsL1) {
        this.efkAltGpsL1 = efkAltGpsL1;
    }

    public Double getEfkAltGpsL5() {
        return efkAltGpsL5;
    }

    public void setEfkAltGpsL5(Double efkAltGpsL5) {
        this.efkAltGpsL5 = efkAltGpsL5;
    }

    public Double getEfkAltGpsIf() {
        return efkAltGpsIf;
    }

    public void setEfkAltGpsIf(Double efkAltGpsIf) {
        this.efkAltGpsIf = efkAltGpsIf;
    }

    public Double getEfkAltGalE1() {
        return efkAltGalE1;
    }

    public void setEfkAltGalE1(Double efkAltGalE1) {
        this.efkAltGalE1 = efkAltGalE1;
    }

    public Double getEfkAltGalE5() {
        return efkAltGalE5;
    }

    public void setEfkAltGalE5(Double efkAltGalE5) {
        this.efkAltGalE5 = efkAltGalE5;
    }

    public Double getEfkAtlGalIf() {
        return efkAtlGalIf;
    }

    public void setEfkAtlGalIf(Double efkAtlGalIf) {
        this.efkAtlGalIf = efkAtlGalIf;
    }

    public DateTime getEfkTimeGpsL1() {
        return efkTimeGpsL1;
    }

    public void setEfkTimeGpsL1(DateTime efkTimeGpsL1) {
        this.efkTimeGpsL1 = efkTimeGpsL1;
    }

    public DateTime getEfkTimeGpsL5() {
        return efkTimeGpsL5;
    }

    public void setEfkTimeGpsL5(DateTime efkTimeGpsL5) {
        this.efkTimeGpsL5 = efkTimeGpsL5;
    }

    public DateTime getEfkTimeGpsIf() {
        return efkTimeGpsIf;
    }

    public void setEfkTimeGpsIf(DateTime efkTimeGpsIf) {
        this.efkTimeGpsIf = efkTimeGpsIf;
    }

    public DateTime getEfkTimeGalE1() {
        return efkTimeGalE1;
    }

    public void setEfkTimeGalE1(DateTime efkTimeGalE1) {
        this.efkTimeGalE1 = efkTimeGalE1;
    }

    public DateTime getEfkTimeGalE5() {
        return efkTimeGalE5;
    }

    public void setEfkTimeGalE5(DateTime efkTimeGalE5) {
        this.efkTimeGalE5 = efkTimeGalE5;
    }

    public DateTime getEfkTimeGalIf() {
        return efkTimeGalIf;
    }

    public void setEfkTimeGalIf(DateTime efkTimeGalIf) {
        this.efkTimeGalIf = efkTimeGalIf;
    }

    public Long getRealId() {
        return realId;
    }

    public void setRealId(Long realId) {
        this.realId = realId;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
