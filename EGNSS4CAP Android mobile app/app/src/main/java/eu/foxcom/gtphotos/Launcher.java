package eu.foxcom.gtphotos;

import android.app.Application;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTimeZone;

public class Launcher extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        /*
        ServiceController serviceController = new ServiceController(this, new ServiceGetter(){
            @Override
            public Service getService(IBinder binder) {
                return ((MainService.LocalBinder) binder).getService();
            }
        }, MainService.class);
        serviceController.startService();
        */

        JodaTimeAndroid.init(this);
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }
}
