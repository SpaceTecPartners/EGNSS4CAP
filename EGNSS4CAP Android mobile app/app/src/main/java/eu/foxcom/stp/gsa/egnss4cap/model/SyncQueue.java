package eu.foxcom.stp.gsa.egnss4cap.model;

import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class SyncQueue {

    public static final String TAG = SyncQueue.class.getName();

    public static abstract class AsyncExecutor {
        String name = "AsyncExecutor";

        public AsyncExecutor() {
        }

        public AsyncExecutor(String name) {
            this.name = name;
        }

        private void runExec() {
            run();
        }

        protected abstract void run();
    }

    private List<AsyncExecutor> execs = new ArrayList<>();
    private int indxToExec = 0;
    private boolean isFinished = false;
    private String name = "SyncQueue";

    public SyncQueue() {
    }

    public SyncQueue(String name) {
        this.name = name;
    }

    public void addAsyncExecutor(AsyncExecutor asyncExecutor) {
        execs.add(asyncExecutor);
    }

    public void addAsyncExecutorBehindCurrent(AsyncExecutor asyncExecutor, int offset) {
        try {
            execs.add(indxToExec + 1 + offset, asyncExecutor);
        } catch (IndexOutOfBoundsException e) {
            execs.add(asyncExecutor);
        }
    }

    public void executionFinish() {
        if (isFinished) {
            throw new RuntimeException("Call executionFinish on finished "+ SyncQueue.class.getSimpleName() + " " + name + ".");
        }
        debugExecutorEnd(execs.get(indxToExec), indxToExec);
        ++indxToExec;
        if (indxToExec == execs.size()) {
            finish();
            return;
        }
        debugExecutorStart(execs.get(indxToExec), indxToExec);
        execs.get(indxToExec).runExec();
    }

    public void executionFinishTotal() {
        if (isFinished) {
            throw new RuntimeException("Call executionFinish on finished "+ SyncQueue.class.getSimpleName() + " " + name + ".");
        }
        finish();
    }

    public void executeQueue() {
        debugStart();
        int size = execs.size();
        if (size == 0 || isFinished) {
            return;
        }
        debugExecutorStart(execs.get(0), 0);
        execs.get(0).runExec();
    }

    private void finish() {
        isFinished = true;
        debugEnd();
    }

    public boolean isLastRunning() {
        int size = execs.size();
        if (size == 0) {
            return false;
        }
        return indxToExec == size - 1;
    }

    private void debugStart() {
        Log.d(TAG, name + " started " + DateTime.now());
    }

    private void debugExecutorStart(AsyncExecutor asyncExecutor, int indx) {
        Log.d(TAG, asyncExecutor.name + "(" + indx + ")" + " of " + name + " started " + DateTime.now());
    }

    private void debugExecutorEnd(AsyncExecutor asyncExecutor, int indx) {
        Log.d(TAG, asyncExecutor.name + "(" + indx + ")" + " of " + name + " ended " + DateTime.now());
    }

    private void debugEnd() {
        Log.d(TAG, name + " ended " + DateTime.now());
    }

    // region get,set

    public List<AsyncExecutor> getExecs() {
        return execs;
    }

    public void setExecs(List<AsyncExecutor> execs) {
        this.execs = execs;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public String getName() {
        return name;
    }
}
// endregion

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */