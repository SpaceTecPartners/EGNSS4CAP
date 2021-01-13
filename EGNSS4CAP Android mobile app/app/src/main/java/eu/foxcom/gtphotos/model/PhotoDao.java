package eu.foxcom.gtphotos.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class PhotoDao {
    @Query("select * from Photo where taskId = :taskId")
    public abstract List<Photo> selectTaskPhotos(String taskId);

    @Query("select * from Photo where taskId = :taskId and isSent = 0")
    public abstract List<Photo> selectTaskPhotosNotSent(String taskId);

    @Query("select count(*) from Photo where taskId = :taskId and isSent = 0")
    public abstract int countTaskPhotosNotSent(String taskId);

    @Query("select * from Photo where taskId is null and userId = :userId order by created desc")
    public abstract List<Photo> selectUnownedPhotos(String userId);

    @Query("select * from Photo where taskId is null and userId = :userId and id = :photoId")
    public abstract List<Photo> selectUnownedPhoto(String userId, Long photoId);

    @Query("select * from Photo where taskId is null and userId = :userId and isSent = 0")
    public abstract List<Photo> selectUnownedPhotosNotSent(String userId);

    @Query("select * from Photo where taskId is null and userId = :userId and isSent = 1")
    public abstract List<Photo> selectUnownedPhotosOnlySent(String userId);

    @Query("select * from Photo where taskId = :taskId and isSent = 1")
    public abstract List<Photo> selectPhotosOnlySent(String taskId);

    @Query("select count(*) from Photo where taskId is null and userId = :userId and isSent = 0")
    public abstract int countUnownedPhotosNotSent(String userId);

    @Query("select p.* from Photo p where p.userId = :userId ")
    public abstract List<Photo> selectUserPhotos(String userId);

    @Query("select * from Photo p where p.id = :id")
    public abstract Photo selectPhotoById(long id);

    @Query("select * from Photo p where p.digest = :digest")
    public abstract Photo selectPhotoByDigest(String digest);

    @Query("select max(p.indx) as maxIndx from Photo p where p.taskId = :taskId")
    public abstract Integer selectMaxIndx(String taskId);

    @Query("select max(p.indx) as maxIndx from Photo p where p.taskId is null")
    public abstract Integer selectUnownedMaxIndx();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insertPhoto(Photo photo);

    @Delete
    public abstract void deletePhoto(Photo photo);
}
