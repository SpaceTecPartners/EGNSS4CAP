package eu.foxcom.gtphotos.model.convexHullUtil;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import eu.foxcom.gnss_scan.NMEAParser;
import eu.foxcom.gnss_scan.NMEAScanner;
import eu.foxcom.gtphotos.model.gnss.NMEAParserApp;

@RequiresApi(api = Build.VERSION_CODES.N)
public class CHService extends Service {

    public class LocalBinder extends Binder {
        public CHService getService() {
            return CHService.this;
        }
    }

    public class Centroid {
        public double latitude;
        public double longitude;

        public Centroid(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private NMEAScanner nmeaScanner;
    private NMEAParser nmeaParser;
    private NMEAParser.CentroidComputedReceiver centroidComputedReceiver;
    private NMEAParser.CentroidSampleAddReceiver centroidSampleAddReceiver;

    private MutableLiveData<Centroid> lastCentroid = new MutableLiveData<>(null);
    private MutableLiveData<Integer> sampleCount = new MutableLiveData<>(0);

    @Override
    public void onCreate() {
        super.onCreate();

        nmeaScanner = new NMEAScanner(this);
        nmeaParser = new NMEAParserApp(this);
        centroidComputedReceiver = new NMEAParser.CentroidComputedReceiver() {
            @Override
            public void receive(double latitude, double longitude) {
                lastCentroid.setValue(new Centroid(latitude, longitude));
            }
        };
        centroidSampleAddReceiver = new NMEAParser.CentroidSampleAddReceiver() {
            @Override
            public void receive(int count) {
                sampleCount.setValue(count);
            }
        };

        nmeaScanner.registerReceiver(nmeaParser);
        nmeaParser.setCentroidComputedReceiver(centroidComputedReceiver);
        nmeaParser.setCentroidSampleAddReceiver(centroidSampleAddReceiver);

        start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void start() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        nmeaScanner.startScan();
    }

    private void stop() {
        nmeaScanner.stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    // region get, set

    public MutableLiveData<Centroid> getLastCentroid() {
        return lastCentroid;
    }

    public MutableLiveData<Integer> getSampleCount() {
        return sampleCount;
    }


    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
