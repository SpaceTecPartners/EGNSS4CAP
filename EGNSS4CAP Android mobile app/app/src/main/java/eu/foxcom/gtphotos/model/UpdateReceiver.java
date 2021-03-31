package eu.foxcom.gtphotos.model;

import java.util.concurrent.Phaser;

public abstract class UpdateReceiver {

    protected SyncQueue syncQueue;
    protected Phaser phaser;

    public UpdateReceiver() {

    }

    public UpdateReceiver(SyncQueue syncQueue) {
        this.syncQueue = syncQueue;
    }

    public UpdateReceiver(Phaser phaser) {
        this.phaser = phaser;
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
        if (phaser != null) {
            phaser.arriveAndDeregister();
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

    public Phaser getPhaser() {
        return phaser;
    }

    public void setPhaser(Phaser phaser) {
        this.phaser = phaser;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */