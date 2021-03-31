package eu.foxcom.gnss_compare_core;

import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;

import eu.foxcom.gnss_compare_core.Constellations.Constellation;
import eu.foxcom.gnss_compare_core.Corrections.Correction;
import eu.foxcom.gnss_compare_core.FileLoggers.FileLogger;
import eu.foxcom.gnss_compare_core.PvtMethods.PvtMethod;

public class Core {



    public static final String TAG = Core.class.getName();

    public static void notifyCore(String text, int duration, String id) {
        Log.i(TAG, "notify: " + id + ": " + text);
    }

    private static void initModuleClasses() {
        Constellation.initialize();
        Correction.initialize();
        PvtMethod.initialize();
        FileLogger.initialize();
    }

    private CalculationModulesArrayList calculationModules = new CalculationModulesArrayList();

    public Core() {
       initModuleClasses();
    }

    public void registerGnssUpdate (FusedLocationProviderClient fusedLocationProviderClient, LocationManager locationManager, CalculationModulesArrayList.PoseUpdatedListener poseUpdatedListener) {
        calculationModules.assignPoseUpdatedListener(poseUpdatedListener);
        calculationModules.registerForGnssUpdates(fusedLocationProviderClient, locationManager);

        calculationModules.get(0).getPose();
    }

    public void unregisterGnssUpdate() {
        calculationModules.unregisterFromGnssUpdates();
    }

    public void removeAllCalculationModule() {
        calculationModules.clear();
    }

    // region get, set

    public CalculationModulesArrayList getCalculationModules() {
        return calculationModules;
    }

    public void setCalculationModules(CalculationModulesArrayList calculationModules) {
        this.calculationModules = calculationModules;
    }

    // endregion


}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
