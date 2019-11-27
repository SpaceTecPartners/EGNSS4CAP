package com.erasmicoin.cordova.cell_info;

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
import android.os.Bundle;
import java.io.UnsupportedEncodingException;

import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

public class ErasmicoinNetworkInfo extends CordovaPlugin {

	private static final String ACTION_READ_CELL_INFO = "getNetworkInfo";

	private final String TAG = ErasmicoinNetworkInfo.class.getSimpleName();

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
		JSONObject arg_object = args.optJSONObject(0);

		if (ACTION_READ_CELL_INFO.equals(action)) {
			ErasmicoinNetworkInfo.this.getAllInfo(callbackContext);
			return true;
		}
		// the action doesn't exist
		return false;
	}

	private int ieee80211_frequency_to_channel(int freq)
	{
	    if (freq == 2484)
	        return 14;

	    if (freq < 2484)
	        return (freq - 2407) / 5;

	    return freq/5 - 1000;
	}

	/**
	 * Register callback for read data
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void getAllInfo(final CallbackContext callbackContext) {

		JSONObject returnObj = new JSONObject();
		JSONObject wifiObj = new JSONObject();

		String ssid = "";
		String bssid = "";
		int frequency = -1;
		int channel = -1;
		int signal = -1;
	    ConnectivityManager connManager = (ConnectivityManager) cordova.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    if (networkInfo.isConnected()) {
	      final WifiManager wifiManager = (WifiManager) cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
	      final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
	      if (connectionInfo != null) {
	        ssid = connectionInfo.getSSID();
	        bssid = connectionInfo.getBSSID();
	        frequency = connectionInfo.getFrequency();
	        channel = ieee80211_frequency_to_channel(frequency);
	        int numberOfLevels = 5;
	        signal = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), numberOfLevels);
	      }
	    }

	    addProperty(wifiObj, "ssid", ssid);
        addProperty(wifiObj, "bssid", bssid);
        addProperty(wifiObj, "frequency", frequency);
        addProperty(wifiObj, "channel", channel);
        addProperty(wifiObj, "signal", signal);
        addProperty(returnObj, "wifi", wifiObj);
        System.out.println(bssid);
        System.out.println(frequency);
        System.out.println(channel);
        System.out.println(signal);

		TelephonyManager tel = (TelephonyManager) cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
		for(CellInfo ci : tel.getAllCellInfo()){
			System.out.println(ci.toString());
		}

		String networkOperator = "";
		String simserial = "";
		try{
			networkOperator = tel.getNetworkOperator();
			simserial = tel.getSimSerialNumber();
		}catch(Exception e){
			e.printStackTrace();
		}
		//String networkOperator = tel.getNetworkOperator();

		//String imei = tel.getImei();

        //String simserial = tel.getSimSerialNumber();

        //addProperty(returnObj, "imei", imei);
        addProperty(returnObj, "simserial", simserial);

		if (networkOperator != null) {

			int mcc = 0;
			int mnc = 0;	

			if(networkOperator.length() > 3){
				mcc = Integer.parseInt(networkOperator.substring(0, 3));
				mnc = Integer.parseInt(networkOperator.substring(3));
			}			

			System.out.println("NETWORKOPERATOR: "+networkOperator);
			System.out.println("MCC: "+mcc);
			System.out.println("MNC: "+mnc);

			addProperty(returnObj, "mcc", mcc);
			addProperty(returnObj, "mnc", mnc);
			addProperty(returnObj, "netOp", networkOperator);
			//addProperty(returnObj, "ssid", ssid);
		}

		if (tel.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
			final GsmCellLocation location = (GsmCellLocation) tel.getCellLocation();
			if (location != null) {
				System.out.println("LAC: " + location.getLac() + " CID: " + location.getCid());
				addProperty(returnObj, "lac", location.getLac());
				addProperty(returnObj, "cid", location.getCid());
			}
		}else{
			System.out.println("NO GSM PHONE");
		}

		int networkTypeInt = -1;
		String networkTypeStr = "unknown";
		try{
			networkTypeInt = tel.getDataNetworkType();
			switch(networkTypeInt){
				case 0:
					networkTypeStr = "unknown";
					break;
				case 3:
					networkTypeStr = "umts";
					break;
				case 17:
					networkTypeStr = "td_scdma";
					break;
				case 20:
					networkTypeStr = "5g";
					break;
				case 13:
					networkTypeStr = "lte";
					break;
				case 18:
					networkTypeStr = "iwlan";
					break;
				case 11:
					networkTypeStr = "iDen";
					break;
				case 9:
					networkTypeStr = "hsupa";
					break;
				case 15:
					networkTypeStr = "hspa+";
					break;
				case 10:
					networkTypeStr = "hspa";
					break;
				case 8:
					networkTypeStr = "hdspa";
					break;
				case 16:
					networkTypeStr = "gsm";
					break;
				case 1:
					networkTypeStr = "gprs";
					break;
				case 12:
					networkTypeStr = "evdo_b";
					break;
				case 6:
					networkTypeStr = "evdo_a";
					break;
				case 5:
					networkTypeStr = "evdo_0";
					break;
				case 14:
					networkTypeStr = "ehrpd";
					break;
				case 2:
					networkTypeStr = "edge";
					break;
				case 4:
					networkTypeStr = "cdma";
					break;
				case 7:
					networkTypeStr = "rtt";
					break;
				default:
					networkTypeStr = "unknown";

			}
			System.out.println("FINE SWITCH, RADIO_INT: "+networkTypeInt);
		}catch(Exception e){
			e.printStackTrace();
		}

		addProperty(returnObj, "radio", networkTypeStr);
		addProperty(returnObj, "radio_int", ""+networkTypeInt);

//		addProperty(returnObj, "LOL", "ASD");
		// Keep the callback
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
		callbackContext.sendPluginResult(pluginResult);
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