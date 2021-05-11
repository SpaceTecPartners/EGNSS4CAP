package eu.foxcom.stp.gsa.egnss4cap;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

public class ServiceController {

    public static abstract class Task {
        public abstract void run();
    }

    private Context context;
    private Class serviceClass;
    private ServiceInit serviceInit;
    private Service service;
    private ServiceConnection serviceConnection;
    private boolean isServiceBound = false;
    private boolean isServiceInitialized = false;
    private List<Task> queueAfterInitialized = new ArrayList<>();
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;

    public ServiceController(Context context, final ServiceGetter serviceGetter, Class serviceClass) {
        this.context = context;
        this.serviceClass = serviceClass;
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ServiceController.this.service = serviceGetter.getService(service);
                isServiceBound = true;
                if (serviceInit != null) {
                    serviceInit.serviceInit();
                }
                isServiceInitialized = true;
                executeAfterInitializedQueue();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isServiceBound = false;
            }


        };
    }

    public void startService() {
        Intent intent = new Intent(context, serviceClass);
        context.startService(intent);
        bindService();
    }

    public void stopService() {
        unbindService();
        Intent intent = new Intent(context, serviceClass);
        context.stopService(intent);
    }

    public void bindService() {
        if (isServiceBound) {
            return;
        }
        Intent intent = new Intent(context, serviceClass);
        context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    public void unbindService() {
        if (!isServiceBound) {
            return;
        }
        context.unbindService(serviceConnection);
        isServiceBound = false;
    }

    public void registerBroadcastReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter) {
        this.broadcastReceiver = broadcastReceiver;
        this.intentFilter = intentFilter;
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
    }

    public void unregisterBroadcastReceiver() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
    }

    public void addAfterInitializedTask(Task task) {
        if (isServiceInitialized) {
            task.run();
        } else {
            queueAfterInitialized.add(task);
        }
    }

    private void executeAfterInitializedQueue() {
        for(Task task : queueAfterInitialized) {
            task.run();
        }
        queueAfterInitialized.clear();
    }

    // region get, set

    public Service getService() {
        return service;
    }

    public boolean isServiceBound() {
        return isServiceBound;
    }

    public void setServiceInit(ServiceInit serviceInit) {
        this.serviceInit = serviceInit;
    }

    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    public IntentFilter getIntentFilter() {
        return intentFilter;
    }

    public boolean isServiceInitialized() {
        return isServiceInitialized;
    }
// endregion
}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
