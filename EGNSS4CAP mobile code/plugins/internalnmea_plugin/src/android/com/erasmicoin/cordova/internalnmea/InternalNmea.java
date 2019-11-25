package com.erasmicoin.cordova.internalnmea;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import java.io.UnsupportedEncodingException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationCallback;
import android.os.Looper;

public class InternalNmea extends CordovaPlugin  implements OnNmeaMessageListener {

	private static final String ACTION_READ_CALLBACK = "registerReadCallback";
	private static final String ACTION_STOP_NMEA = "stopNmea";
	// callback that will be used to send back data to the cordova app
	private CallbackContext readCallback;
	private LocationManager lm = null;
	private LocationListener locationListenerGPS;
	private final String TAG = InternalNmea.class.getSimpleName();
	private LocationRequest mLocationRequest;
	private LocationCallback mLocationCallback;
	private FusedLocationProviderClient mFusedLocationProviderClient;
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

		if(ACTION_STOP_NMEA.equals(action)){
			InternalNmea.this.stopNmea();
			return true;
		}
//		if(lm == null){
//			InternalNmea.this.startNmea();
//		}
	
		// Register read callback
		if (ACTION_READ_CALLBACK.equals(action)) {
			InternalNmea.this.startNmea(args);
			InternalNmea.this.startLocationUpdates();
			registerReadCallback(callbackContext);
			return true;
		}
		// the action doesn't exist
		return false;
	}

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

	// Trigger new location updates at interval
	private void startLocationUpdates() {

	    // Create the location request to start receiving updates
	    mLocationRequest = new LocationRequest();
	    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	    mLocationRequest.setInterval(UPDATE_INTERVAL);
	    mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

	    // Create LocationSettingsRequest object using location request
	    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
	    builder.addLocationRequest(mLocationRequest);
	    LocationSettingsRequest locationSettingsRequest = builder.build();

	    // Check whether location settings are satisfied
	    // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
	    SettingsClient settingsClient = LocationServices.getSettingsClient(cordova.getActivity());
	    settingsClient.checkLocationSettings(locationSettingsRequest);

	    // new Google API SDK v11 uses getFusedLocationProviderClient(this)

	    mLocationCallback = new LocationCallback() {
						      @Override
						      public void onLocationResult(LocationResult locationResult) {
						         // do work here
						      	 System.out.println("ARRIVATA LOCATION DA FUSED!");
						         InternalNmea.this.onLocationChanged(locationResult.getLastLocation());
						      }
						    };

	    mFusedLocationProviderClient =LocationServices.getFusedLocationProviderClient(cordova.getActivity());
	    mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
	}

	private void onLocationChanged(Location location) {
	    // New location has now been determined
	    try{
			JSONObject returnObj = new JSONObject();
			addProperty(returnObj, "altitude", location.getAltitude());
			addProperty(returnObj, "latitude", location.getLatitude());
			addProperty(returnObj, "longitude", location.getLongitude());
			addProperty(returnObj, "timestamp", location.getTime());
			addProperty(returnObj, "provider", "LocationManager");
			String toSend = returnObj.toString()+"\r";
			System.out.println("INVIO DATI FUSED! ");
			InternalNmea.this.updateReceivedData(toSend.getBytes("UTF-8"));
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
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

	private void startNmea(JSONArray args) {

        lm = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
        //lm_net = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
		int timeout = 0;
		try{
			JSONObject parameters = args.getJSONObject(0);
			 timeout = parameters.getInt("timeout");
			 Log.d(TAG, "parametro timeout "+timeout);
		}catch(JSONException error){
				Log.d(TAG, "parametro timeout non impostato");
		}
        locationListenerGPS = new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
            	/*if(location.isFromMockProvider()){
            		try{
	            		JSONObject returnObj = new JSONObject();
						addProperty(returnObj, "message", "from_mock");
						addProperty(returnObj, "provider", "ErrorProvider");
						String toSend = returnObj.toString()+"\r";
						InternalNmea.this.updateReceivedData(toSend.getBytes("UTF-8"));
					}catch(UnsupportedEncodingException e){
						e.printStackTrace();
					}
            	}*/
            	if (location.getExtras() == null) {
            		try{
	            		JSONObject returnObj = new JSONObject();
						addProperty(returnObj, "message", "no_extras");
						addProperty(returnObj, "provider", "ErrorProvider");
						String toSend = returnObj.toString()+"\r";
						InternalNmea.this.updateReceivedData(toSend.getBytes("UTF-8"));
					}catch(UnsupportedEncodingException e){
						e.printStackTrace();
					}
            	}
            	else{
            		System.out.println("LOCATION HAS EXTRAS");
            		try{
	            		JSONObject returnObj = new JSONObject();
						addProperty(returnObj, "satellites", location.getExtras().getInt("satellites"));
						addProperty(returnObj, "provider", "ExtraProvider");
						String toSend = returnObj.toString()+"\r";
						InternalNmea.this.updateReceivedData(toSend.getBytes("UTF-8"));
					}catch(UnsupportedEncodingException e){
						e.printStackTrace();
					}
				}
				try{
					JSONObject returnObj = new JSONObject();
					addProperty(returnObj, "altitude", location.getAltitude());
					addProperty(returnObj, "latitude", location.getLatitude());
					addProperty(returnObj, "longitude", location.getLongitude());
					addProperty(returnObj, "timestamp", location.getTime());
					addProperty(returnObj, "provider", "LocationManager");
					String toSend = returnObj.toString()+"\r";
					InternalNmea.this.updateReceivedData(toSend.getBytes("UTF-8"));
				}catch(UnsupportedEncodingException e){
					e.printStackTrace();
				}
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
        
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, timeout, 0, locationListenerGPS);
        //lm_net.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timeout, 0, locationListenerGPS);

        lm.addNmeaListener(this);
    }

    private void stopNmea(){
		if(lm != null){
			lm.removeNmeaListener(this);
			lm.removeUpdates(locationListenerGPS);
		}
		if(mFusedLocationProviderClient != null){
			 mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
		}
	}

    @Override
    public void onNmeaMessage(String message, long timestamp) {
    	try{
//        	InternalNmea.this.updateReceivedData(message.getBytes("UTF-8"));
			JSONObject returnObj = new JSONObject();
			addProperty(returnObj, "nmea_message", message);
			addProperty(returnObj, "provider", "Nmea");
			String toSend = returnObj.toString()+"\r";
			InternalNmea.this.updateReceivedData(toSend.getBytes("UTF-8"));
    	}catch(UnsupportedEncodingException e){
    		e.printStackTrace();
    	}
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
}