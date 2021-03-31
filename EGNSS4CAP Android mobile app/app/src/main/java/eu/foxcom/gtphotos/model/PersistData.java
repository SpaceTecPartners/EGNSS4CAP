package eu.foxcom.gtphotos.model;

import android.content.Context;
import android.content.SharedPreferences;

import eu.foxcom.gtphotos.SettingFilterActivity;
import eu.foxcom.gtphotos.SettingsActivity;

public class PersistData {

    public enum PREFERENCES {
        // basic info
        GALILEO_SIGNAL_CHECK("galileoSignalCheck"),
        DUAL_FREQUENCY_SIGNAL_CHECK("dualFrequncySignalCheck"),
        EGNOS_SIGNAL_CHECK("egnosSignalCheck"),
        GALILEO_NAVIGATION_MESSAGE_SIGNAL_CHECK("galileoNavigationMessageSignalCheck"),
        // settings
        CENTROID_FILTER_ACTIVE("centroidFilterActive"),
        PHOTO_WITH_CENTROID_FILTER("photoWithCentroiFilter"),
        SAMPLING_NUMBER("samplingNumber"),
        MIN_NUMBER_SATS("minNumberSats"),
        MIN_HDOP("minHDOP"),
        MIN_SNR("minSNR"),
        MIN_FIX("minFix"),
        BUTTON_SNAPSHOT_ACTIVE("buttonSnapshotActive"),
        BUTTON_SNAPSHOT_KEY_CODE("buttonSnapshotKeyCode"),
        EXPOSURE_CORRECTION("exposureCorrection"),
        MANUAL_BRIGHTNESS_ACTIVE("manualBrightnessActive"),
        AUTO_PAN_ACTIVE("AUTO_PAN_ACTIVE"),
        BEEP_PATH_POINT_ACTIVE("BEEP_PATH_POINT_ACTIVE");

        public final String ID;

        PREFERENCES(String id) {
            ID = id;
        }
    }

    public enum MANUAL_BRIGHTNESS_ACTIVE {
        DEFAULT,
        TRUE,
        FALSE;
    }

    private static final String SHARED_PREFERENCES_ID = "EGNSS4CAPPerzData";
    public static final String SHARED_PREFERENCES_TASK_FILTER_ID = "EGNSS4CAPPerzDataTaskFilter";

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE);
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_ID, Context.MODE_PRIVATE).edit();
    }

    public static boolean getGalileoSignalCheck(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.GALILEO_SIGNAL_CHECK.ID, false);
    }

    public static void saveGalileoSignalCheck(Context context, boolean checked) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.GALILEO_SIGNAL_CHECK.ID, checked);
        editor.apply();
    }

    public static boolean getDualFrequencySignalCheck(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.DUAL_FREQUENCY_SIGNAL_CHECK.ID, false);
    }

    public static void saveDualFrequencySignalCheck(Context context, boolean checked) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.DUAL_FREQUENCY_SIGNAL_CHECK.ID, checked);
        editor.apply();
    }

    public static boolean getEgnosSignalCheck(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.EGNOS_SIGNAL_CHECK.ID, false);
    }

    public static void saveEgnosSignalCheck(Context context, boolean checked) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.EGNOS_SIGNAL_CHECK.ID, checked);
        editor.apply();
    }

    public static boolean getGalileoNavigationMessageSignalCheck(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.GALILEO_NAVIGATION_MESSAGE_SIGNAL_CHECK.ID, false);
    }

    public static void saveGalileoNavigationMessageSignalCheck(Context context, boolean checked) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.GALILEO_NAVIGATION_MESSAGE_SIGNAL_CHECK.ID, checked);
        editor.apply();
    }

    public static void saveSamplingNumber(Context context, int number) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFERENCES.SAMPLING_NUMBER.ID, number);
        editor.apply();
    }

    public static int getSamplingNumber(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(PREFERENCES.SAMPLING_NUMBER.ID, SettingsActivity.DEFAULT_SAMPLING_NUMBER);
    }

    public static void saveMinNumberSats(Context context, int number) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFERENCES.MIN_NUMBER_SATS.ID, number);
        editor.apply();
    }

    public static int getMinNumberSats(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(PREFERENCES.MIN_NUMBER_SATS.ID, SettingFilterActivity.DEFAULT_MIN_SAT_NUMBER);
    }

    public static void saveMinHDOP(Context context, double number) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putLong(PREFERENCES.MIN_HDOP.ID, Double.doubleToRawLongBits(number));
        editor.apply();
    }

    public static double getMinHDOP(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return Double.longBitsToDouble(sharedPreferences.getLong(PREFERENCES.MIN_HDOP.ID, Double.doubleToRawLongBits(SettingFilterActivity.DEFAULT_MIN_HDOP)));
    }

    public static void saveMinSNR(Context context, double number) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putLong(PREFERENCES.MIN_SNR.ID, Double.doubleToRawLongBits(number));
        editor.apply();
    }

    public static double getMinSNR(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return Double.longBitsToDouble(sharedPreferences.getLong(PREFERENCES.MIN_SNR.ID, Double.doubleToRawLongBits(SettingFilterActivity.DEFAULT_MIN_MEAN_SNR)));
    }

    public static void saveMinFix(Context context, int number) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFERENCES.MIN_FIX.ID, number);
        editor.apply();
    }

    public static int getMinFix(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(PREFERENCES.MIN_FIX.ID, SettingFilterActivity.DEFAULT_MIN_FIX);
    }

    public static void saveCentroidFilterActive(Context context, boolean active) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.CENTROID_FILTER_ACTIVE.ID, active);
        editor.apply();
    }

    public static boolean getCentroidFilterActive(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.CENTROID_FILTER_ACTIVE.ID, false);
    }

    public static void savePhotoWithCentroiLocation(Context context, boolean active) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.PHOTO_WITH_CENTROID_FILTER.ID, active);
        editor.apply();
    }

    public static boolean getPhotoWithCentroiLocation(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.PHOTO_WITH_CENTROID_FILTER.ID, false);
    }

    public static void saveButtonSnapshotActive(Context context, boolean active) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.BUTTON_SNAPSHOT_ACTIVE.ID, active);
        editor.apply();
    }

    public static boolean getButtonSnapshotActive(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.BUTTON_SNAPSHOT_ACTIVE.ID, false);
    }

    public static void saveButtonSnapshotKeyCode(Context context, int keyCode) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFERENCES.BUTTON_SNAPSHOT_KEY_CODE.ID, keyCode);
        editor.apply();
    }

    public static int getButtonSnapshotKeyCode(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(PREFERENCES.BUTTON_SNAPSHOT_KEY_CODE.ID, -1);
    }

    public static int getExposureCorrection(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(PREFERENCES.EXPOSURE_CORRECTION.ID, 0);
    }

    public static void saveExposureCorrection(Context context, int correction) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFERENCES.EXPOSURE_CORRECTION.ID, correction);
        editor.apply();
    }

    public static MANUAL_BRIGHTNESS_ACTIVE getManualBrightnessCorrectionActive(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return  MANUAL_BRIGHTNESS_ACTIVE.valueOf(sharedPreferences.getString(PREFERENCES.MANUAL_BRIGHTNESS_ACTIVE.ID, MANUAL_BRIGHTNESS_ACTIVE.DEFAULT.name()));
    }

    public static void saveManualBrightnessCorrectionActive(Context context, MANUAL_BRIGHTNESS_ACTIVE value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(PREFERENCES.MANUAL_BRIGHTNESS_ACTIVE.ID, value.name());
        editor.apply();
    }

    public static boolean getAutoPan(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.AUTO_PAN_ACTIVE.ID, true);
    }

    public static void saveAutoPan(Context context, boolean active) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.AUTO_PAN_ACTIVE.ID, active);
        editor.apply();
    }

    public static boolean getBeepPathPoint(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(PREFERENCES.BEEP_PATH_POINT_ACTIVE.ID, true);
    }

    public static void saveBeepPathPoint(Context context, boolean active) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFERENCES.BEEP_PATH_POINT_ACTIVE.ID, active);
        editor.apply();
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */