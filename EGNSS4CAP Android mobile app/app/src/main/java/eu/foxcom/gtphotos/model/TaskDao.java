package eu.foxcom.gtphotos.model;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

@Dao
public abstract class TaskDao {

    @Query("select * from Task")
    public abstract List<Task> selectAllTasks();

    @Query("select * from Task where userId = :userId")
    public abstract List<Task> selectUserTasks(String userId);

    @Query("select * from Task where id = :id")
    public abstract Task selectTaskById(String id);

    @Query("delete from Task")
    protected abstract void deleteAllTasks();

    @Query("delete from Task where userId = :userId")
    protected abstract void deleteUserTasks(String userId);

    @Query("select count(*) from Task where userId = :userId")
    protected abstract int countOfAllTasks(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void insertTasks(List<Task> tasks);

    @Update
    public abstract void updateTask(Task task);

    @Transaction
    public void recreateTasks(List<Task> tasks, String userId) {
        deleteUserTasks(userId);
        insertTasks(tasks);
    }

    @RawQuery
    public abstract List<Task> selectFilteredTasks(SupportSQLiteQuery query);
}
