package eu.foxcom.gtphotos.model;

public abstract class UpdateReceiver {

    protected SyncQueue syncQueue;

    public UpdateReceiver() {

    }

    public UpdateReceiver(SyncQueue syncQueue) {
        this.syncQueue = syncQueue;
    }

    protected void successExec(AppDatabase appDatabase) {
        success(appDatabase);
        finishExec(true);
    }

    protected abstract void success(AppDatabase appDatabase);

    protected void failedExec(String error) {
        failed(error);
        finishExec(false);
    }

    protected abstract void failed(String error);

    protected void finishExec(boolean success) {
        finish(success);
        if (syncQueue != null) {
            syncQueue.executionFinish();
        }
    }

    protected abstract void finish(boolean success);

    // region get, set

    public void setSyncQueue(SyncQueue syncQueue) {
        this.syncQueue = syncQueue;
    }

    public SyncQueue getSyncQueue() {
        return syncQueue;
    }

    // endregion
}
