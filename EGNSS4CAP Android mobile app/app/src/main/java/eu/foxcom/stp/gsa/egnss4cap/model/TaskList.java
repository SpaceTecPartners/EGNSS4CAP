package eu.foxcom.stp.gsa.egnss4cap.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;


public class TaskList {

    public static class TaskListFilter {

        public enum STATUS {
            NEW,
            OPEN,
            SENT,
            PROVIDED,
            RETURNED,
            ACCEPTED,
            DECLINED;
        }

        public enum SORT {
            STATUS,
            DUE_DATE,
            NAME;
        }

        private enum PERZ_DATA {
            FILTER_STATUSES_COUNT,
            // musí být indexován
            FILTER_STATUS,
            SORT_PASSED_AT_END,
            SORT,
            SORT_DESC,
            NAME,
            USER_ID,
            ;
        }

        private static final String SHARED_PREFERENCES_ID = PersistData.SHARED_PREFERENCES_TASK_FILTER_ID;

        // vrací null pokud nebylo nastavení inicializováno
        public static TaskListFilter createFromPerzData(Context context, String userId) {
            TaskListFilter taskListFilter = new TaskListFilter();
            taskListFilter.userId = userId;
            if (!taskListFilter.loadFromPerzData(context)) {
                return null;
            }
            return taskListFilter;
        }

        private List<STATUS> filterStatuses = new ArrayList<>();
        private boolean sortPassedAtEnd = true;
        private SORT sort;
        private boolean sortDesc = true;
        private String userId;
        private String name;

        public void saveToPersistData(Context context) {
            SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE).edit();
            int filterStatusesCount = filterStatuses.size();
            editor.putInt(uniqKey(PERZ_DATA.FILTER_STATUSES_COUNT), filterStatuses.size());
            for (int i = 0; i < filterStatusesCount; ++i) {
                editor.putString(uniqKey(PERZ_DATA.FILTER_STATUS) + i, filterStatuses.get(i).name());
            }
            editor.putBoolean(uniqKey(PERZ_DATA.SORT_PASSED_AT_END), sortPassedAtEnd);
            editor.putString(uniqKey(PERZ_DATA.SORT), sort.name());
            editor.putBoolean(uniqKey(PERZ_DATA.SORT_DESC), sortDesc);
            editor.putString(uniqKey(PERZ_DATA.NAME), name);
            editor.putString(uniqKey(PERZ_DATA.USER_ID), userId);
            editor.apply();
        }

        private boolean loadFromPerzData(Context context) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE);
            if (!sharedPreferences.contains(uniqKey(PERZ_DATA.USER_ID))) {
                // nastavení nebylo inicializováno
                return false;
            }
            int filterStatusesCount = sharedPreferences.getInt(uniqKey(PERZ_DATA.FILTER_STATUSES_COUNT), 0);
            System.out.println("");
            for (int i = 0; i < filterStatusesCount; ++i) {
                filterStatuses.add(STATUS.valueOf(sharedPreferences.getString(uniqKey(PERZ_DATA.FILTER_STATUS) + i, null)));
            }
            sortPassedAtEnd = sharedPreferences.getBoolean(uniqKey(PERZ_DATA.SORT_PASSED_AT_END), true);
            sort = SORT.valueOf(sharedPreferences.getString(uniqKey(PERZ_DATA.SORT), null));
            sortDesc = sharedPreferences.getBoolean(uniqKey(PERZ_DATA.SORT_DESC), true);
            name = sharedPreferences.getString(uniqKey(PERZ_DATA.NAME), null);
            return true;
        }

        private String uniqKey(PERZ_DATA perzData) {
            return perzData.name() + userId;
        }

        // region get, set

        public boolean isSortPassedAtEnd() {
            return sortPassedAtEnd;
        }

        public void setSortPassedAtEnd(boolean sortPassedAtEnd) {
            this.sortPassedAtEnd = sortPassedAtEnd;
        }

        public SORT getSort() {
            return sort;
        }

        public void setSort(SORT sort) {
            this.sort = sort;
        }

        public List<STATUS> getFilterStatuses() {
            return filterStatuses;
        }

        public boolean isSortDesc() {
            return sortDesc;
        }

        public void setSortDesc(boolean sortDesc) {
            this.sortDesc = sortDesc;
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

        // endregion
    }

    private AppDatabase appDatabase;
    private List<Task> tasks = new ArrayList<>();

    public static int countOfAllTask(AppDatabase appDatabase) {
        return appDatabase.taskDao().countOfAllTasks(LoggedUser.createFromAppDatabase(appDatabase).getId());
    }

    public static TaskList createFromJSONArray(AppDatabase appDatabase, JSONArray jsonArray, String userId) throws JSONException {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); ++i) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Task task = Task.createFromResponse(jsonObject, userId);
            tasks.add(task);
        }
        return new TaskList(appDatabase, tasks);
    }

    public static TaskList createFromAppDatabase(AppDatabase appDatabase) {
        List<Task> tasks = appDatabase.taskDao().selectUserTasks(LoggedUser.createFromAppDatabase(appDatabase).getId());
        return new TaskList(appDatabase, tasks);
    }

    public static TaskList createFromAppDatabaseFilter(AppDatabase appDatabase, TaskListFilter taskListFilter) {
        List<Task> tasks = appDatabase.taskDao().selectFilteredTasks(buildFilteredTaskQuery(taskListFilter, false));
        if (taskListFilter.isSortPassedAtEnd()) {
            List<Task> passedTasks = appDatabase.taskDao().selectFilteredTasks(buildFilteredTaskQuery(taskListFilter, true));
            tasks.addAll(passedTasks);
        }
        return new TaskList(appDatabase, tasks);
    }

    private static SupportSQLiteQuery buildFilteredTaskQuery(TaskListFilter taskListFilter, boolean isDueDatePassed) {
        String queryString = "";
        List<Object> args = new ArrayList<>();

        // user id
        queryString += "select * from Task where userId = ?";
        args.add(taskListFilter.getUserId());

        // name
        if (taskListFilter.getName() != null && !taskListFilter.getName().isEmpty()) {
            queryString += " and name like ?";
            args.add("%" + taskListFilter.getName() + "%");
        }

        // status
        int statusFilterSize = taskListFilter.getFilterStatuses().size();
        queryString += " and ( 1 != 1";
        for (int i = 0; i < statusFilterSize; ++i) {
            TaskListFilter.STATUS filterStatus = taskListFilter.getFilterStatuses().get(i);
            queryString += " or";
            switch (filterStatus) {
                case NEW:
                    queryString += " status = ?";
                    args.add(Task.STATUS.NEW.DB_VAL);
                    break;
                case OPEN:
                    queryString += " status = ?";
                    args.add(Task.STATUS.OPEN.DB_VAL);
                    break;
                case SENT:
                    queryString += " (status != ? and status != ?)";
                    args.add(Task.STATUS.NEW.DB_VAL);
                    args.add(Task.STATUS.OPEN.DB_VAL);
                    break;
                case PROVIDED:
                    queryString += " status = ?";
                    args.add(Task.STATUS.DATA_PROVIDED.DB_VAL);
                    break;
                case RETURNED:
                    queryString += " status = ?";
                    args.add(Task.STATUS.RETURNED.DB_VAL);
                    break;
                case ACCEPTED:
                    queryString += " (status = ? and flagValid = 1)";
                    args.add(Task.STATUS.DATA_CHECKED.DB_VAL);
                    break;
                case DECLINED:
                    queryString += " (status = ? and flagInvalid = 1)";
                    args.add(Task.STATUS.DATA_CHECKED.DB_VAL);
                    break;
            }
        }
        queryString += ")";

        // passed due date
        if (taskListFilter.isSortPassedAtEnd()) {
            if (isDueDatePassed) {
                queryString += " and taskDueToDateDateTime < ?";
            } else {
                queryString += " and taskDueToDateDateTime >= ?";
            }
            args.add(DateTime.now().toDate().getTime());
        }

        // sort
        if (taskListFilter.getSort().equals(TaskListFilter.SORT.STATUS)) {
            queryString += " order by statusOrder";
        } else if (taskListFilter.getSort().equals(TaskListFilter.SORT.DUE_DATE)) {
            queryString += " order by taskDueToDateDateTime";
        } else if (taskListFilter.getSort().equals(TaskListFilter.SORT.NAME)) {
            queryString += " order by name";
        } else {
            throw new RuntimeException("Unknown TaskListFilter.ORDER value");
        }
        queryString += " collate localized";
        if (taskListFilter.isSortDesc()) {
            queryString += " desc";
        } else {
            queryString += " asc";
        }

        return new SimpleSQLiteQuery(queryString, args.toArray());
    }

    private TaskList(AppDatabase appDatabase, List<Task> tasks) {
        this.appDatabase = appDatabase;
        this.tasks = tasks;
    }

    @Deprecated
    public void updatePhotos(SyncQueue syncQueue, final Task.UpdateTaskReceiver receiver, final Requestor requestor, final Context context) {
        for (final Task task : tasks) {
            syncQueue.addAsyncExecutor(new SyncQueue.AsyncExecutor("updatePhotoExecutor") {
                @Override
                protected void run() {
                    task.updatePhotos(appDatabase, receiver, requestor, context);
                }
            });
        }
    }

    public void updatePhotosByRealIds(Context context, Requestor requestor, Photo.UpdatePhotoReceiver photoReceiver, Task.UpdateTaskReceiver taskReceiver, Task.UpdateTaskReceiver finalTasksReceiver) {
        Phaser phaser = new Phaser(tasks.size() + 1);
        AtomicBoolean isSuccess = new AtomicBoolean(false);
        Task.UpdateTaskReceiver innerTaskReceiver = new Task.UpdateTaskReceiver(phaser) {
            @Override
            protected void success(AppDatabase appDatabase, Task task) {
                taskReceiver.successExec(appDatabase, task);
            }

            @Override
            protected void success(AppDatabase appDatabase) {
            }

            @Override
            protected void failed(String error) {
                taskReceiver.failedExec(error);
            }

            @Override
            protected void finish(boolean success) {
                isSuccess.set(success);
            }
        };
        innerTaskReceiver.setAutoSuccessDBSave(false);
        for(Task task : tasks) {
            task.updatePhotosByRealIds(appDatabase, context, requestor, photoReceiver, innerTaskReceiver);
        }

        Handler joinHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().submit(() -> {
            phaser.awaitAdvance(phaser.arriveAndDeregister());
            joinHandler.post(() -> {
                if (isSuccess.get()) {
                    finalTasksReceiver.successExec(appDatabase);
                } else {
                    finalTasksReceiver.failedExec("Failed to download photos for tasks.");
                }
            });
        });
    }

    public void recreateToDB() {
        appDatabase.taskDao().recreateTasks(tasks, LoggedUser.createFromAppDatabase(appDatabase).getId());
    }

    public void uploadStatus(final AppDatabase appDatabase, final Context context, final Task.UpdateTaskReceiver receiver, final Requestor requestor) {
        SyncQueue syncQueue = receiver.getSyncQueue();
        for (final Task task : tasks) {
            if (!task.isUploadStatus()) {
                continue;
            }
            syncQueue.addAsyncExecutor(new SyncQueue.AsyncExecutor("uploadExecutor") {
                @Override
                protected void run() {
                    task.updateStatus(appDatabase, context, receiver, requestor);
                }
            });
        }
    }

    // atribute note pro open task je zachován
    // isPhotoSync je zachován
    public void refreshToDB() {
        TaskList oldTaskList = TaskList.createFromAppDatabase(appDatabase);
        for (Task newTask : tasks) {
            String note = null;
            for (Task oldTask : oldTaskList.getTasks()) {
                if (!oldTask.getStatus().equals(Task.STATUS.OPEN.DB_VAL)) {
                    continue;
                }
                if (oldTask.getId().equals(newTask.getId())) {
                    note = oldTask.getNote();
                    break;
                }
            }
            if (note == null) {
                note = newTask.getNote();
            }
            newTask.setNote(note);
        }

        for (Task newTask : tasks) {
            for (Task oldTask : oldTaskList.getTasks()) {
                if (oldTask.getId().equals(newTask.getId())) {
                    newTask.setPhotoSync(oldTask.isPhotoSync());
                }
            }
        }

        recreateToDB();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void prepareRealIdsForProcessing(AppDatabase appDatabase) {
        for (Task task : tasks) {
            task.prepareRealIdsForProcessing(appDatabase);
        }
    }

    public int getCountToUpdatePhotoRealId() {
        int count = 0;
        for (Task task : tasks) {
            count += task.getToUpdatePhotoRealIds().size();
        }
        return count;
    }

    // region get, set

    public List<Task> getTasks() {
        return tasks;
    }

    // endregion

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */