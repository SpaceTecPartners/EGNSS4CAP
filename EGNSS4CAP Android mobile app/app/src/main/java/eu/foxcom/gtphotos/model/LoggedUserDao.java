package eu.foxcom.gtphotos.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
@Dao
public abstract class LoggedUserDao {
    @Query("select * from LoggedUser limit 1")
    public abstract LoggedUser loadLoggedUser();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void insertLoggedUser(LoggedUser loggedUser);

    @Query("delete from LoggedUser")
    protected abstract void deleteLoggedUser();

    @Transaction
    public void refreshLoggedUser(LoggedUser loggedUser) {
        deleteLoggedUser();
        insertLoggedUser(loggedUser);
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */