package eu.foxcom.gtphotos.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhotoList {

    private Context context;
    private AppDatabase appDatabase;
    private List<Photo> photos = new ArrayList<>();
    private String taskId;

    public static PhotoList createFromJSONArray(AppDatabase appDatabase, JSONArray jsonArray, String taskId, Context context) throws JSONException, IOException {
        List<Photo> photos = new ArrayList<>();
        Integer indx = 0;
        if (taskId == null) {
            indx = appDatabase.photoDao().selectUnownedMaxIndx();
            if (indx == null) {
                indx = 0;
            } else {
                ++indx;
            }
        }
        for (int i = 0; i < jsonArray.length(); ++i) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            photos.add(Photo.createFromResponse(jsonObject, taskId, indx, context, appDatabase));
            ++indx;
        }
        return new PhotoList(appDatabase, photos, taskId, context);
    }

    public static PhotoList createFromAppDatabase(AppDatabase appDatabase, String taskId, Context context) {
        List<Photo> photos = new ArrayList<>();
        if (taskId != null) {
            photos = appDatabase.photoDao().selectTaskPhotos(taskId);
        } else {
            photos = appDatabase.photoDao().selectUnownedPhotos(LoggedUser.createFromAppDatabase(appDatabase).getId());
        }
        return new PhotoList(appDatabase, photos, taskId, context);
    }

    public static PhotoList createFromAppDatabaseNotSent(AppDatabase appDatabase, String taskId, Context context) {
        List<Photo> photos;
        if (taskId != null) {
            photos = appDatabase.photoDao().selectTaskPhotosNotSent(taskId);
        } else {
            photos = appDatabase.photoDao().selectUnownedPhotosNotSent(LoggedUser.createFromAppDatabase(appDatabase).getId());
        }
        return new PhotoList(appDatabase, photos, taskId, context);
    }

    public static PhotoList createFromAppDatabaseUnownedPhotos(AppDatabase appDatabase, boolean notSent, Context context) {
        if (notSent) {
            List<Photo> photos = appDatabase.photoDao().selectUnownedPhotosNotSent(LoggedUser.createFromAppDatabase(appDatabase).getId());
            return new PhotoList(appDatabase, photos, null, context);
        } else {
            return createFromAppDatabase(appDatabase, null, context);
        }
    }

    public static PhotoList createFromAppDatabaseUnownedPhotosOnlySent(AppDatabase appDatabase, Context context) {
        List<Photo> photos = appDatabase.photoDao().selectUnownedPhotosOnlySent(LoggedUser.createFromAppDatabase(appDatabase).getId());
        return new PhotoList(appDatabase, photos, null, context);
    }

    public static PhotoList createFromAppDatabaseOnlySent(AppDatabase appDatabase, String taskId, Context context) {
        List<Photo> photos = appDatabase.photoDao().selectPhotosOnlySent(taskId);
        return new PhotoList(appDatabase, photos, taskId, context);
    }

    public static PhotoList createFromAppDatabaseUnownedPhoto(AppDatabase appDatabase, Long photoId, Context context) {
        List<Photo> photos = appDatabase.photoDao().selectUnownedPhoto(LoggedUser.createFromAppDatabase(appDatabase).getId(), photoId);
        return new PhotoList(appDatabase, photos, null, context);
    }

    public static Map<Long, Photo> createFromAppDatabaseByUser(AppDatabase appDatabase) {
        LoggedUser loggedUser = LoggedUser.createFromAppDatabase(appDatabase);
        List<Photo> photos = appDatabase.photoDao().selectUserPhotos(loggedUser.getId());
        Map<Long, Photo> photoMap = new HashMap<>();
        for (Photo photo : photos) {
            photoMap.put(photo.getId(), photo);
        }
        return photoMap;
    }

    public static int countUnownedPhotosNotSent(AppDatabase appDatabase) {
        return appDatabase.photoDao().countUnownedPhotosNotSent(LoggedUser.createFromAppDatabase(appDatabase).getId());
    }

    public static int countTaskPhotosNotSent(AppDatabase appDatabase, String taskId) {
        return appDatabase.photoDao().countTaskPhotosNotSent(taskId);
    }

    private PhotoList(AppDatabase appDatabase, List<Photo> photos, String taskId, Context context) {
        this.appDatabase = appDatabase;
        this.photos = photos;
        this.taskId = taskId;
        this.context = context;
        for (Photo photo : photos) {
            photo.setContext(context);
        }
    }

    public void recreateToDB() {
        for (Photo photo : photos) {
            photo.refreshToDB(appDatabase);
        }
    }

    public void addPhoto(Photo photo) {
        photo.refreshToDB(appDatabase);
        photos.add(photo);
    }

    public void setSent(boolean isSent) {
        for (Photo photo : photos) {
            photo.setSent(isSent);
        }
    }

    public void setLastSendFailed(boolean failed) {
        for (Photo photo :photos) {
            photo.setLastSendFailed(failed);
        }
    }

    public void removePhotoAt(int indx) {
        appDatabase.photoDao().deletePhoto(photos.get(indx));
        photos.remove(indx);
    }

    /*
     * odstraní z databáze všechny fotografie, který mají shodné digesty fotografií v tomto seznamu
     * */
    public void deleteSameDigestPhotos() {
        for (Photo photo : photos) {
            String digest = photo.getDigest();
            if (digest == null) {
                continue;
            }
            Photo duplicatePhoto = appDatabase.photoDao().selectPhotoByDigest(digest);
            if (duplicatePhoto == null) {
                continue;
            }
            duplicatePhoto.delete(appDatabase);
        }
    }

    public JSONArray toJSONArray() throws Exception {
        JSONArray jsonArray = new JSONArray();
        for (Photo photo : photos) {
            jsonArray.put(photo.toJSONObject());
        }
        return jsonArray;
    }

    // region get, set

    public List<Photo> getPhotos() {
        return photos;
    }

    public String getTaskId() {
        return taskId;
    }

    // endregion
}
