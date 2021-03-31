package eu.foxcom.gnss_scan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;


public class GnssStatusScanner extends Scanner {

    public enum MESSAGE_TYPE {
        GNSS_STATUS("gnssStatus"),
        OSNMA("osnma"),
        DUAL_FREQ("dualFreq");

        public final String JSON_NAME;

        MESSAGE_TYPE (String jsonName) {
            JSON_NAME = jsonName;
        }
    }

    public enum CONSTELLATION {
        UNKNOWN(GnssStatus.CONSTELLATION_UNKNOWN, "unknown"),
        GPS(GnssStatus.CONSTELLATION_GPS, "gps"),
        SBAS(GnssStatus.CONSTELLATION_SBAS, "sbas"),
        GLONASS(GnssStatus.CONSTELLATION_GLONASS, "glonass"),
        QZSS(GnssStatus.CONSTELLATION_QZSS, "qzss"),
        BEIDOU(GnssStatus.CONSTELLATION_BEIDOU, "beidou"),
        GALILEO(GnssStatus.CONSTELLATION_GALILEO, "galileo"),
        IRNSS(GnssStatus.CONSTELLATION_IRNSS, "irnss");

        private final int CONST_NUMBER;
        public final String JSON_NAME;

        private static CONSTELLATION createfromConst(int number) {
            for(CONSTELLATION constellation : CONSTELLATION.values()) {
                if (number == constellation.CONST_NUMBER) {
                    return constellation;
                }
            }
            return null;
        }

        CONSTELLATION (int number, String jsonName) {
            CONST_NUMBER = number;
            JSON_NAME = jsonName;
        }
    }

    public abstract static class GnssStatusReceiver extends Receiver {

        @Override
        protected void receiveVirtual(Holder holder) {
            receive((GnssStatusHolder) holder);
        }

        public abstract void receive(GnssStatusHolder gnssStatusHolder);
    }

    public static class GnssStatusHolder extends Holder {

        private MESSAGE_TYPE messageType;
        private Set<CONSTELLATION> constellations = new HashSet<>();


        @Override
        public JSONObject toJSONObject() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("messageType", messageType.JSON_NAME);
            JSONArray constArray = new JSONArray();
            for (CONSTELLATION constellation : constellations) {
                constArray.put(constellation.JSON_NAME);
            }
            jsonObject.put("constellations", constArray);
            return jsonObject;
        }

        // region get, set

        public MESSAGE_TYPE getMessageType() {
            return messageType;
        }

        public Set<CONSTELLATION> getConstellations() {
            return constellations;
        }

        // endregion
    }

    private LocationManager lm = null;

    private int satelliteCount;
    private float[] azimuths;
    private float[] cn0DHzs;
    private Integer[] constellationTypes;
    private float[] elevations;
    private int[] svids;
    private GnssStatus.Callback gnssStatusListener;
    private GnssNavigationMessage.Callback gnssNavigationMessageListener;
    private GnssMeasurementsEvent.Callback gnssMeasurementsEventListener;
    private LocationListener locationListener;

    public GnssStatusScanner(Context context) {
        super(context);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssStatusListener =
                    new GnssStatus.Callback() {
                        @Override
                        public void onStarted() {
                            super.onStarted();
                        }

                        @Override
                        public void onStopped() {
                            super.onStopped();
                        }

                        @Override
                        public void onFirstFix(int ttffMillis) {
                            super.onFirstFix(ttffMillis);
                        }

                        @Override
                        public void onSatelliteStatusChanged(GnssStatus status) {
                            super.onSatelliteStatusChanged(status);
                            satelliteCount = status.getSatelliteCount();

                            azimuths = new float[satelliteCount];
                            cn0DHzs = new float[satelliteCount];
                            constellationTypes = new Integer[satelliteCount];
                            elevations = new float[satelliteCount];
                            svids = new int[satelliteCount];

                            for (int i = 0; i < satelliteCount; i++) {
                                azimuths[i] = status.getAzimuthDegrees(i);
                                cn0DHzs[i] = status.getCn0DbHz(i);
                                constellationTypes[i] = status.getConstellationType(i);
                                elevations[i] = status.getElevationDegrees(i);
                                svids[i] = status.getSvid(i);
                            }

                            Set<CONSTELLATION> constSet = new HashSet<>();
                            for (int i = 0; i < constellationTypes.length; ++i) {
                                constSet.add(CONSTELLATION.createfromConst(constellationTypes[i]));
                            }
                            GnssStatusHolder gnssStatusHolder = new GnssStatusHolder();
                            gnssStatusHolder.messageType = MESSAGE_TYPE.GNSS_STATUS;
                            gnssStatusHolder.constellations = constSet;
                            GnssStatusScanner.this.updateReceivers(gnssStatusHolder);
                        }
                    };

            gnssNavigationMessageListener =
                    new GnssNavigationMessage.Callback() {
                        @Override
                        public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                            if (event.getType() == GnssNavigationMessage.TYPE_GAL_I) {
                                GnssStatusHolder gnssStatusHolder = new GnssStatusHolder();
                                gnssStatusHolder.messageType = MESSAGE_TYPE.OSNMA;
                                GnssStatusScanner.this.updateReceivers(gnssStatusHolder);
                            }
                            if (event.getType() == GnssNavigationMessage.TYPE_GPS_L5CNAV) {
                                GnssStatusHolder gnssStatusHolder = new GnssStatusHolder();
                                gnssStatusHolder.messageType = MESSAGE_TYPE.DUAL_FREQ;
                                GnssStatusScanner.this.updateReceivers(gnssStatusHolder);
                            }
                        }

                        @Override
                        public void onStatusChanged(int status) {
                        }
                    };

            gnssMeasurementsEventListener =
                    new GnssMeasurementsEvent.Callback() {
                        @Override
                        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                            for (GnssMeasurement measurement : event.getMeasurements()) {
                                if (measurement.hasCarrierFrequencyHz()) {
                                    if (measurement.getCarrierFrequencyHz() == 1176450048) {
                                        GnssStatusHolder gnssStatusHolder = new GnssStatusHolder();
                                        gnssStatusHolder.messageType = MESSAGE_TYPE.DUAL_FREQ;
                                        GnssStatusScanner.this.updateReceivers(gnssStatusHolder);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onStatusChanged(int status) {
                        }
                    };
        }
    }

    public void registerReceiver(GnssStatusReceiver gnssStatusReceiver) {
        registerReceiverVirtual(gnssStatusReceiver);
    }

    public void unregisterReceiver(GnssStatusReceiver gnssStatusReceiver) {
        unregisterReceiverVirtual(gnssStatusReceiver);
    }

    public void stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            lm.unregisterGnssStatusCallback(gnssStatusListener);
            lm.unregisterGnssNavigationMessageCallback(gnssNavigationMessageListener);
            lm.unregisterGnssMeasurementsCallback(gnssMeasurementsEventListener);
        }
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    public void startScan(int timeout) {
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timeout, 0, locationListener);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, timeout, 0, locationListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            lm.registerGnssStatusCallback(gnssStatusListener);
            lm.registerGnssNavigationMessageCallback(gnssNavigationMessageListener);
            lm.registerGnssMeasurementsCallback(gnssMeasurementsEventListener);
        }
    }

    // region get, set
    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
