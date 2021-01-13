package eu.foxcom.gtphotos.model;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.os.StrictMode;
import android.view.KeyEvent;

import com.android.volley.VolleyError;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Util {

    public static String JSONgetStringNullable(JSONObject jsonObject, String key) throws JSONException {
        return jsonObject.isNull(key) ? null : jsonObject.getString(key);
    }

    public static Boolean JSONgetBooleanNullable(JSONObject jsonObject, String key) throws JSONException {
        return jsonObject.isNull(key) ? null : jsonObject.getBoolean(key);
    }

    public static boolean JSONequalsString(JSONObject jsonObject, String key, String value) throws JSONException {
        return (jsonObject.has(key)
                && jsonObject.getString(key).equals(value));
    }

    public static <T> List<T> JSONArrayToList(JSONArray jsonArray) throws JSONException {
        List<T> list = new ArrayList<>();
        int lentgh = jsonArray.length();
        for (int i = 0; i < lentgh; ++i) {
            list.add((T) jsonArray.get(i));
        }
        return list;
    }

    public static String getPhoneManufacturer() {
        return Build.MANUFACTURER;
    }

    public static String getPhoneModel() {
        return Build.MODEL;
    }

    public static String getOSName() {
        return "Android";
    }

    public static String getOSVersion() {
        return Build.VERSION.RELEASE;
    }

    public static int dpToPixels(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    public static String getButtonName(int keyCode) {
        String name = "UNRECOGNIZE";
        Class<KeyEvent> c = KeyEvent.class;
        for (Field f : c.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod)) {
                try {
                    if(!f.getType().equals(int.class) || keyCode != f.getInt(null)) {
                        continue;
                    }
                    name = f.getName();
                    break;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return name;
    }

    public static boolean isInternetAvailable() {
        try {
            StrictMode.ThreadPolicy origThreadPolicy = StrictMode.getThreadPolicy();
            StrictMode.ThreadPolicy newThreadPolicy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
            StrictMode.setThreadPolicy(newThreadPolicy);
            InetAddress inetAddress = InetAddress.getByName("google.com");
            boolean ret = !inetAddress.equals("");
            StrictMode.setThreadPolicy(origThreadPolicy);
            return ret;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static DecimalFormat createLatLngDecimalFormat() {
        DecimalFormat decimalFormat = new DecimalFormat("#.#######");
        decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
        return decimalFormat;
    }

    public static DateTimeFormatter createPrettyDateFormat() {
        return DateTimeFormat.forPattern("yyyy-MM-dd");
    }

    public static DateTimeFormatter createPrettyTimeFormat() {
        return DateTimeFormat.forPattern("HH:mm:ss");
    }

    public static DateTimeFormatter createPrettyDateTimeFormat() {
        return DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    }

    public static DecimalFormat createPrettyCoordinateFormat() {
        DecimalFormat decimalFormat = new DecimalFormat("#.#######");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
        decimalFormat.setDecimalFormatSymbols(symbols);
        return decimalFormat;
    }

    public static boolean isLocationServiceEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
    }

    public static String volleyErrorMsg(VolleyError error) {
        String err = "Status code: " + (error.networkResponse != null ?  error.networkResponse.statusCode : "none") + "\n";
        err += error.getMessage();
        return err;
    }
}
