package eu.foxcom.gtphotos.model;

import android.util.Log;

import androidx.room.TypeConverter;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class Convertes {

    public static String TAG = Convertes.class.getSimpleName();

    @TypeConverter
    public static DateTime LongToDatetime(Long mils) {
        if (mils == null) {
            return null;
        }
        return new DateTime(new Date(mils));
    }

    @TypeConverter
    public static Long datetimeToLong(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toDate().getTime();
    }

    @TypeConverter
    public static JSONObject StringToJSON(String s) {
        if (s == null) {
            return null;
        }
        try {
            return new JSONObject(s);
        } catch (JSONException e) {
            Log.e(TAG, "Try convert malformed string to JSONObject.", e);
            return null;
        }
    }

    @TypeConverter
    public static String JSONToString(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        return jsonObject.toString();
    }

    @TypeConverter
    public static JSONArray stringToJSONArray(String s) {
        if (s == null) {
            return null;
        }
        try {
            return new JSONArray(s);
        } catch (JSONException e) {
            Log.e(TAG, "Try convert malformed string to JSONArray.", e);
            return null;
        }
    }

    @TypeConverter
    public static String JSONArrayToString(JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }
        return jsonArray.toString();
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
