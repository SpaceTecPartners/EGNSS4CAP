package eu.foxcom.gnss_scan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class NetworkInfoScanner extends Scanner {

    public enum CELL {
        GSM("GSM"),
        LTE("LTE"),
        CDMA("CDMA"),
        WCDMA("WCDMA");

        public final String JSON_NAME;

        CELL(String jsonName) {
            JSON_NAME = jsonName;
        }
    }

    public enum RADIO {
        UNKNOWN(0, "unknown"),
        UMTS(3, "umts"),
        TD_SCDMA(17, "td_scdma"),
        N_5G(20, "5g"),
        LTE(13, "lte"),
        IWLAN(18, "iwlan"),
        IDEN(11, "iDen"),
        HSUPA(9, "hsupa"),
        HSPAPLUS(15, "hspa+"),
        HSPA(10, "hspa"),
        HDSPA(8, "hdspa"),
        GSM(16, "gsm"),
        GPRS(1, "gprs"),
        EVDO_B(12, "evdo_b"),
        EVDO_A(6, "evdo_a"),
        EVDO_0(5, "evdo_0"),
        EHRPD(14, "ehrpd"),
        EDGE(2, "edge"),
        CDMA(4, "cdma"),
        RTT(7, "rtt");

        private static RADIO createFromCode(int code) {
            for (RADIO RADIO : RADIO.values()) {
                if (RADIO.CODE == code) {
                    return RADIO;
                }
            }
            return UNKNOWN;
        }

        private final int CODE;
        public final String JSON_NAME;

        RADIO(int code, String jsonName) {
            CODE = code;
            JSON_NAME = jsonName;
        }
    }

    public abstract static class NetworkInfoReceiver extends Receiver {

        @Override
        protected void receiveVirtual(Holder holder) {
            receive((NetworkInfoHolder) holder);
        }

        public abstract void receive(NetworkInfoHolder networkInfoHolder);
    }

    public static class NetworkInfoHolder extends Holder {

        private String mnc;
        private String mcc;
        private String networkOperator;
        private WifiHolder wifiHolder;
        private List<CellHolder> cells;
        private RADIO radio;

        @Override
        public JSONObject toJSONObject() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("mnc", mnc);
            jsonObject.put("mcc", mcc);
            jsonObject.put("networkOperator", networkOperator);
            jsonObject.put("wifi", wifiHolder == null ? null : wifiHolder.toJSONObject());
            JSONArray cellArray = new JSONArray();
            for (CellHolder cellHolder : cells) {
                cellArray.put(cellHolder.toJSONObject());
            }
            jsonObject.put("cells", cellArray);
            jsonObject.put("radio", radio.JSON_NAME);
            return jsonObject;
        }

        // region get, set

        public String getMnc() {
            return mnc;
        }

        public String getMcc() {
            return mcc;
        }

        public String getNetworkOperator() {
            return networkOperator;
        }

        public WifiHolder getWifiHolder() {
            return wifiHolder;
        }

        public List<CellHolder> getCells() {
            return cells;
        }

        public RADIO getRadio() {
            return radio;
        }

        // endregion
    }

    public static class WifiHolder extends Holder {
        private String ssid;
        private String bsid;
        private Integer frequency;
        private Integer channel;
        private Integer signal;

        @Override
        public JSONObject toJSONObject() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ssid", ssid);
            jsonObject.put("bsid", bsid);
            jsonObject.put("frequency", frequency);
            jsonObject.put("channel", channel);
            jsonObject.put("signal", signal);
            return jsonObject;
        }

        // region get, set

        public String getSsid() {
            return ssid;
        }

        public String getBsid() {
            return bsid;
        }

        public Integer getFrequency() {
            return frequency;
        }

        public Integer getChannel() {
            return channel;
        }

        public Integer getSignal() {
            return signal;
        }

        // endregion
    }

    public static class CellHolder extends Holder {
        private String type;
        private String mnc;
        private String mcc;
        private String lac;
        private String cid;
        private String ci;
        private String tac;
        private String networkId;
        private String baseStationId;
        private String latitude;
        private String longitude;

        @Override
        public JSONObject toJSONObject() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type);
            jsonObject.put("mnc", mnc);
            jsonObject.put("mcc", mcc);
            jsonObject.put("lac", lac);
            jsonObject.put("cid", cid);
            jsonObject.put("ci", ci);
            jsonObject.put("tac", tac);
            jsonObject.put("networkId", networkId);
            jsonObject.put("baseStationId", baseStationId);
            jsonObject.put("latitude", latitude);
            jsonObject.put("longitude", longitude);
            return jsonObject;
        }

        // region get, set

        public String getType() {
            return type;
        }

        public String getMnc() {
            return mnc;
        }

        public String getMcc() {
            return mcc;
        }

        public String getLac() {
            return lac;
        }

        public String getCid() {
            return cid;
        }

        public String getCi() {
            return ci;
        }

        public String getTac() {
            return tac;
        }

        public String getNetworkId() {
            return networkId;
        }

        public String getBaseStationId() {
            return baseStationId;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        // endregion

    }

    public NetworkInfoScanner(Context context) {
        super(context);
    }

    public void registerReceiver(NetworkInfoReceiver networkInfoReceiver) {
        registerReceiverVirtual(networkInfoReceiver);
    }

    public void unregisterReceiver(NetworkInfoReceiver networkInfoReceiver) {
        unregisterReceiverVirtual(networkInfoReceiver);
    }

    private int ieee80211FrequencyToChannel(int freq) {
        if (freq == 2484)
            return 14;

        if (freq < 2484)
            return (freq - 2407) / 5;

        return freq / 5 - 1000;
    }

    @RequiresPermission(allOf = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.READ_PHONE_STATE
    })
    public void scan() {

        NetworkInfoHolder returnObj = new NetworkInfoHolder();
        WifiHolder wifiObj = new WifiHolder();

        String ssid = "";
        String bssid = "";
        int frequency = -1;
        int channel = -1;
        int signal = -1;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities networkCapabilities = connManager.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                        if (connectionInfo != null) {
                            ssid = connectionInfo.getSSID();
                            bssid = connectionInfo.getBSSID();
                            frequency = connectionInfo.getFrequency();
                            channel = ieee80211FrequencyToChannel(frequency);
                            int numberOfLevels = 5;
                            signal = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), numberOfLevels);
                        }
                    }
                }
            }
        }

        wifiObj.ssid = ssid;
        wifiObj.bsid = bssid;
        wifiObj.frequency = frequency;
        wifiObj.channel = channel;
        wifiObj.signal = signal;
        returnObj.wifiHolder = wifiObj;

        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = tel.getNetworkOperator();

        if (networkOperator != null) {
            String mcc = "";
            String mnc = "";
            if (networkOperator.length() > 3) {
                mcc = networkOperator.substring(0, 3);
                mnc = networkOperator.substring(3);
            }
            returnObj.mcc = mcc;
            returnObj.mnc = mnc;
            returnObj.networkOperator = networkOperator;
        }

        List<CellHolder> cells = new ArrayList<>();
        List<CellInfo> cellInfos = tel.getAllCellInfo();
        for (CellInfo info : cellInfos) {
            if (info instanceof CellInfoGsm) {
                CellIdentityGsm cellIdentityGsm = ((CellInfoGsm) info).getCellIdentity();
                CellHolder cellHolder = new CellHolder();
                cellHolder.type = CELL.GSM.name();
                cellHolder.mcc = availableInt(cellIdentityGsm.getMcc());
                cellHolder.mnc = availableInt(cellIdentityGsm.getMnc());
                cellHolder.lac = availableInt(cellIdentityGsm.getLac());
                cellHolder.cid = availableInt(cellIdentityGsm.getCid());
                cells.add(cellHolder);
            } else if (info instanceof CellInfoLte) {
                CellIdentityLte cellIdentityLte = ((CellInfoLte) info).getCellIdentity();
                CellHolder cellHolder = new CellHolder();
                cellHolder.type = CELL.LTE.name();
                cellHolder.mcc = availableInt(cellIdentityLte.getMcc());
                cellHolder.mnc = availableInt(cellIdentityLte.getMnc());
                cellHolder.ci = availableInt(cellIdentityLte.getCi());
                cellHolder.tac = availableInt(cellIdentityLte.getTac());
                cells.add(cellHolder);
            } else if (info instanceof CellInfoCdma) {
                CellIdentityCdma cellIdentityCdma = ((CellInfoCdma) info).getCellIdentity();
                CellHolder cellHolder = new CellHolder();
                cellHolder.type = CELL.CDMA.name();
                cellHolder.networkId = availableInt(cellIdentityCdma.getNetworkId());
                cellHolder.baseStationId = availableInt(cellIdentityCdma.getBasestationId());
                cellHolder.latitude = availableInt(cellIdentityCdma.getLatitude());
                cellHolder.longitude = availableInt(cellIdentityCdma.getLongitude());
                cells.add(cellHolder);
            } else if (info instanceof CellInfoWcdma) {
                CellIdentityWcdma cellIdentityWcdma = ((CellInfoWcdma) info).getCellIdentity();
                CellHolder cellHolder = new CellHolder();
                cellHolder.type = CELL.WCDMA.name();
                cellHolder.mcc = availableInt(cellIdentityWcdma.getMcc());
                cellHolder.mnc = availableInt(cellIdentityWcdma.getMnc());
                cellHolder.lac = availableInt(cellIdentityWcdma.getLac());
                cellHolder.cid = availableInt(cellIdentityWcdma.getCid());
                cells.add(cellHolder);
            }
        }
        returnObj.cells = cells;

        int networkTypeInt;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkTypeInt = tel.getDataNetworkType();
        } else {
            networkTypeInt = 0;
        }
        returnObj.radio = RADIO.createFromCode(networkTypeInt);
        ;

        updateReceivers(returnObj);
    }

    private String availableInt(int number) {
        if (number == CellInfo.UNAVAILABLE) {
            return "UNAVAILABLE";
        } else {
            return String.valueOf(number);
        }
    }
}
