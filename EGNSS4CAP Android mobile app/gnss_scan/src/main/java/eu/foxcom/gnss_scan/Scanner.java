package eu.foxcom.gnss_scan;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

abstract class Scanner {

    private Object receiverMutex = new Object();
    private List<Receiver> receivers = new ArrayList<>();
    protected Context context;

    protected Scanner(Context context) {
        this.context = context;
    }

    protected final void registerReceiverVirtual(Receiver receiver) {
        synchronized (receiverMutex) {
            receivers.add(receiver);
        }
    }

    protected final void unregisterReceiverVirtual(Receiver receiver) {
        synchronized (receiverMutex) {
            receivers.remove(receiver);
        }
    }

    public void unregisterAllReceivers() {
        synchronized (receiverMutex) {
            receivers.clear();
        }
    }

    protected void updateReceivers(Holder holder, String category) {
        synchronized (receiverMutex) {
            // allow re-entrance to modify receivers (async mutex)
            List<Receiver> receiversCopy = new ArrayList<>(receivers);
            for(Receiver receiver : receiversCopy) {
                if (receiver.getCategory() == null || category == null || receiver.getCategory().equals(category)) {
                    receiver.receiveVirtual(holder);
                }
            }
        }
    }

    protected void updateReceivers(Holder holder) {
        updateReceivers(holder, null);
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
