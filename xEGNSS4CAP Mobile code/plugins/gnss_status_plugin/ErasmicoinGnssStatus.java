package com.erasmicoin.gnss_status;

import java.util.Arrays;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import android.location.LocationListener;
import android.os.Bundle;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;

public class ErasmicoinGnssStatus extends CordovaPlugin implements LocationListener {

    private static final String ACTION_READ_CALLBACK = "registerReadCallback";
    private static final String ACTION_STOP_UPDATE = "stopGnssStatus";
    // callback that will be used to send back data to the cordova app
    private CallbackContext readCallback;
    private LocationManager lm = null;
    private LocationListener locationListenerGPS;
    private final String TAG = ErasmicoinGnssStatus.class.getSimpleName();
    private long UPDATE_INTERVAL = 3 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    /**
     * Overridden execute method
     * @param action the string representation of the action to execute
     * @param args
     * @param callbackContext the cordova {@link CallbackContext}
     * @return true if the action exists, false otherwise
     * @throws JSONException if the args parsing fails
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Action: " + action);
        Log.d(TAG, "Args: " + args);
        JSONObject arg_object = args.optJSONObject(0);
//		if(action.equals('registerReadCallback')){
//
//		}

        if (ACTION_STOP_UPDATE.equals(action)) {

            return true;
        }
//		if(lm == null){
//			ErasmicoinGnssStatus.this.startNmea();
//		}

        // Register read callback
        if (ACTION_READ_CALLBACK.equals(action)) {
            ErasmicoinGnssStatus.this.startGnssStatus(args);
            registerReadCallback(callbackContext);
            return true;
        }
        // the action doesn't exist
        return false;
    }

    private void startGnssStatus(JSONArray args) {

        lm = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
        //lm_net = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
        int timeout = 0;
        try {
            JSONObject parameters = args.getJSONObject(0);
            timeout = parameters.getInt("timeout");
            Log.d(TAG, "parametro timeout " + timeout);
        } catch (JSONException error) {
            Log.d(TAG, "parametro timeout non impostato");
        }


        if (ActivityCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timeout, 0, ErasmicoinGnssStatus.this);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, timeout, 0, ErasmicoinGnssStatus.this);

        lm.registerGnssStatusCallback(gnssStatusListener);
        lm.registerGnssNavigationMessageCallback(gnssNavigationMessageListener);
        lm.registerGnssMeasurementsCallback(gnssMeasurementsEventListener);

    }

    private int satelliteCount;
    private float[] azimuths;
    private float[] cn0DHzs;
    private Integer[] constellationTypes;
    private float[] elevations;
    private int[] svids;
    private final GnssStatus.Callback gnssStatusListener =
            new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    super.onStarted();
                    System.out.println("STARTED STATUS LISTENER");
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    System.out.println("STOPPED STATUS LISTENER");
                }

                @Override
                public void onFirstFix(int ttffMillis) {
                    super.onFirstFix(ttffMillis);
                    System.out.println("FIRST FIX");
                    System.out.println(ttffMillis);
                }

                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    super.onSatelliteStatusChanged(status);
                    satelliteCount = status.getSatelliteCount();

                    JSONObject obj = new JSONObject();

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

                    Set<Integer> targetSet = new HashSet<Integer>(Arrays.asList(constellationTypes));

                    ErasmicoinGnssStatus.this.addProperty(obj, "constellations", targetSet);
                    ErasmicoinGnssStatus.this.addProperty(obj, "type", "gnss_status");

                    String toSend = obj.toString()+"\r";
                    try{
                        ErasmicoinGnssStatus.this.updateReceivedData(toSend.getBytes("UTF-8"));
                    }catch(UnsupportedEncodingException e){
                        e.printStackTrace();
                    }

                    System.out.println("NEW ROUND OF MESSAGES");
                    System.out.println(Arrays.toString(azimuths));
                    System.out.println(Arrays.toString(cn0DHzs));
                    System.out.println(Arrays.toString(constellationTypes));
                    System.out.println(Arrays.toString(elevations));
                    System.out.println(Arrays.toString(svids));
                }
            };

    private final GnssNavigationMessage.Callback gnssNavigationMessageListener =
            new GnssNavigationMessage.Callback() {
                @Override
                public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
//                    super.onGnssNavigationMessageReceived(event);
                    System.out.println("STAMPO SAT ID Modif:");
                    System.out.println(event.getSvid());
                    System.out.println(event.getType());
                    if(event.getType() == GnssNavigationMessage.TYPE_GAL_I){
                        JSONObject obj = new JSONObject();
                        ErasmicoinGnssStatus.this.addProperty(obj, "value", true);
                        ErasmicoinGnssStatus.this.addProperty(obj, "type", "osnma");
                        String toSend = obj.toString()+"\r";
                        try{
                            ErasmicoinGnssStatus.this.updateReceivedData(toSend.getBytes("UTF-8"));
                        }catch(UnsupportedEncodingException e){
                            e.printStackTrace();
                        }
                    }
                    if(event.getType() == GnssNavigationMessage.TYPE_GPS_L5CNAV){
                        JSONObject obj = new JSONObject();
                        ErasmicoinGnssStatus.this.addProperty(obj, "value", true);
                        ErasmicoinGnssStatus.this.addProperty(obj, "type", "dual_freq");
                        String toSend = obj.toString()+"\r";
                        try{
                            ErasmicoinGnssStatus.this.updateReceivedData(toSend.getBytes("UTF-8"));
                        }catch(UnsupportedEncodingException e){
                            e.printStackTrace();
                        }
                    }
                    System.out.println(event.toString());
                }

                @Override
                public void onStatusChanged(int status) {
                    System.out.println("NEW NAVIGATION STATUS "+status);
                }
            };

    private final GnssMeasurementsEvent.Callback gnssMeasurementsEventListener =
            new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                    System.out.println("MEASUR. RECEIVED, CFHZ:");
                    for (GnssMeasurement measurement : event.getMeasurements()) {
                        if(measurement.hasCarrierFrequencyHz()){
                            if(measurement.getCarrierFrequencyHz() == 1176450048){
                                System.out.println(measurement.getCarrierFrequencyHz());
                                JSONObject obj_df = new JSONObject();
                                ErasmicoinGnssStatus.this.addProperty(obj_df, "value", true);
                                ErasmicoinGnssStatus.this.addProperty(obj_df, "type", "dual_freq");
                                String toSend = obj_df.toString()+"\r";
                                try{
                                    ErasmicoinGnssStatus.this.updateReceivedData(toSend.getBytes("UTF-8"));
                                }catch(UnsupportedEncodingException e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                @Override
                public void onStatusChanged(int status) {
                    System.out.println("NEW MEASUREM. STATUS "+status);
                }
            };

    /**
     * Dispatch read data to javascript
     * @param data the array of bytes to dispatch
     */
    private void updateReceivedData(byte[] data) {
        if( readCallback != null ) {
            System.out.println("MANDO FUORI: ");
            System.out.println(new String(data));
            PluginResult result = new PluginResult(PluginResult.Status.OK, data);
            result.setKeepCallback(true);
            readCallback.sendPluginResult(result);
        }
    }

    /**
     * Register callback for read data
     * @param callbackContext the cordova {@link CallbackContext}
     */
    private void registerReadCallback(final CallbackContext callbackContext) {
        Log.d(TAG, "Registering callback");
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Log.d(TAG, "Registering Read Callback");
                readCallback = callbackContext;
                JSONObject returnObj = new JSONObject();
                addProperty(returnObj, "registerReadCallback", "true");
                // Keep the callback
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
    }

    /**
     * Utility method to add some properties to a {@link JSONObject}
     * @param obj the json object where to add the new property
     * @param key property key
     * @param value value of the property
     */
    private void addProperty(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value);
        }
        catch (JSONException e){}
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}