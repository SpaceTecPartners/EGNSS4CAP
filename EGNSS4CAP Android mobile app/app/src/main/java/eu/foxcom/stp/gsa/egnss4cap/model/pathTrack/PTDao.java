package eu.foxcom.stp.gsa.egnss4cap.model.pathTrack;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class PTDao {

    @Query("select * from PTPath where userId = :userId order by startT desc")
    public abstract List<PTPath> selectAllPTPathsByUserId(String userId);

    @Query("select * from PTPath where userId = :userId and realId is null")
    public abstract List<PTPath> selectPTPathsUnsent(String userId);

    @Query("select count(*) from PTPath where userId = :userId and realId is null")
    public abstract int selectPTPathsUnsentCount(String userId);

    @Query("select count(*) from PTPath where userId = :userId and realId is null and isUploadingOpened = 1")
    public abstract int selectPTPathsUploadingOpenedCount(String userId);

    @Query("select * from PTPath where autoId = :autoId")
    public abstract PTPath selectPTPathByAutoId(Long autoId);

    @Query("select * from PTPoint where autoId = :autoId")
    public abstract PTPoint selectPTPointByAutoId(Long autoId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Long insertPTPath(PTPath ptPath);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Long insertPTPoint(PTPoint ptPoint);

    @Query("select * from PTPoint where pathId = :pathId")
    public abstract List<PTPoint> selectPTPointsByPathId(Long pathId);

    @Delete
    public abstract void deletePTPath(PTPath ptPath);

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
