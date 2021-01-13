package eu.foxcom.gnss_scan;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import org.json.JSONException;
import org.json.JSONObject;

@RequiresApi(Build.VERSION_CODES.N)
public class NMEAScanner extends Scanner {

    enum RECEIVER_CATEGORY {
        LOCATION,
        NMEA;
    }

    enum PROVIDER {
        FUSED("fused"),
        GNSS("gnss");

        public final String JSON_NAME;

        PROVIDER(String jsonName) {
            JSON_NAME = jsonName;
        }
    }

    public abstract static class NMEAReceiver extends Receiver {
        @Override
        protected String getCategory() {
            return RECEIVER_CATEGORY.NMEA.name();
        }

        @Override
        protected void receiveVirtual(Holder holder) {
            receive((NMEAHolder) holder);
        }

        public abstract void receive(NMEAHolder nmeaHolder);
    }

    public abstract static class NMEALocationReceiver extends Receiver {
        @Override
        protected String getCategory() {
            return RECEIVER_CATEGORY.LOCATION.name();
        }

        @Override
        protected void receiveVirtual(Holder holder) {
            receive((NMEALocationHolder) holder);
        }

        public abstract void receive(NMEALocationHolder nmeaLocationHolder);
    }

    public static class NMEAHolder extends Holder {
        private String nmeaMessage;
        private long timestamp;

        @Override
        public JSONObject toJSONObject() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("nmeaMessage", nmeaMessage);
            jsonObject.put("timestamp", timestamp);
            return jsonObject;
        }

        // region get, set

        public String getNmeaMessage() {
            return nmeaMessage;
        }

        public long getTimestamp() {
            return timestamp;
        }

        // endregion
    }

    public static class NMEALocationHolder extends Holder {
        private PROVIDER provider;
        private double altitude;
        private double latitude;
        private double longitude;
        private long timestamp;
        private Integer satelliteCount;

        @Override
        public JSONObject toJSONObject() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("provider", provider.JSON_NAME);
            jsonObject.put("altitude", altitude);
            jsonObject.put("latitude", latitude);
            jsonObject.put("longitude", longitude);
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("satelliteCount", satelliteCount);
            return jsonObject;
        }

        // region get, set


        public PROVIDER getProvider() {
            return provider;
        }

        public Integer getSatelliteCount() {
            return satelliteCount;
        }

        public double getAltitude() {
            return altitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public long getTimestamp() {
            return timestamp;
        }

        // endregion
    }

    private final long UPDATE_INTERVAL = 500;
    private final long FASTEST_INTERVAL = 500;
    private final int TIMEOUT = 0;

    private LocationManager lm = null;
    private LocationListener locationListenerGPS;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private OnNmeaMessageListener onNmeaMessageListener;


    public NMEAScanner(Context context) {
        super(context);
        onNmeaMessageListener = new OnNmeaMessageListener() {
            @Override
            public void onNmeaMessage(String message, long timestamp) {
                NMEAHolder nmeaHolder = new NMEAHolder();
                nmeaHolder.nmeaMessage = message;
                nmeaHolder.timestamp = timestamp;
                updateReceivers(nmeaHolder, RECEIVER_CATEGORY.NMEA.name());
            }
        };
    }

    public void registerReceiver(NMEAReceiver nmeaReceiver) {
        registerReceiverVirtual(nmeaReceiver);
    }

    public void registerReceiver(NMEALocationReceiver nmeaLocationReceiver) {
        registerReceiverVirtual(nmeaLocationReceiver);
    }

    public void unregisterReceiver(NMEAReceiver nmeaReceiver) {
        unregisterReceiverVirtual(nmeaReceiver);
    }

    public void unregisterReceiver(NMEALocationReceiver nmeaLocationReceiver) {
        unregisterReceiverVirtual(nmeaLocationReceiver);
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    public void startScan() {
        startNmea();
        startFusedLocationUpdates();
    }

    public void stopScan() {
        stopNmea();
        stopFusedLocationUpdates();
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void startFusedLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(context);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                NMEAScanner.this.onLocationChanged(locationResult.getLastLocation());
            }
        };

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private void onLocationChanged(Location location) {
        updateNMEALocationReceiver(PROVIDER.FUSED, location);
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    private void startNmea() {
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListenerGPS = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateNMEALocationReceiver(PROVIDER.GNSS, location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIMEOUT, 0, locationListenerGPS, Looper.getMainLooper());
        lm.addNmeaListener(onNmeaMessageListener);
    }

    private void updateNMEALocationReceiver(PROVIDER provider, Location location) {
        NMEALocationHolder nmeaLocationHolder = new NMEALocationHolder();
        nmeaLocationHolder.provider = provider;
        nmeaLocationHolder.altitude = location.getAltitude();
        nmeaLocationHolder.latitude = location.getLatitude();
        nmeaLocationHolder.longitude = location.getLongitude();
        nmeaLocationHolder.timestamp = location.getTime();
        if (location.getExtras() != null) {
            nmeaLocationHolder.satelliteCount = location.getExtras().getInt("satellites");
        }
        updateReceivers(nmeaLocationHolder, RECEIVER_CATEGORY.LOCATION.name());
    }

    private void stopNmea() {
        if (lm != null) {
            lm.removeNmeaListener(onNmeaMessageListener);
            lm.removeUpdates(locationListenerGPS);
        }
    }

    private void stopFusedLocationUpdates() {
        if (mFusedLocationProviderClient != null) {
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }
    }

    // region get, set
    // endregion
}
