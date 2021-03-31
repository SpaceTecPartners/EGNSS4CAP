/*
 * Copyright 2018 TFI Systems

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package eu.foxcom.gnss_compare_core.PvtMethods;

import android.location.Location;

import com.galfins.gogpsextracts.Coordinates;

import java.util.HashMap;
import java.util.Set;

import eu.foxcom.gnss_compare_core.Constellations.Constellation;


public abstract class PvtMethod {
    /**
     * Calculates pose based on given parameters
     * @param constellation satellite constellation object for which the calculations are to
     *                      be performed
     * @return calculated pose of the receiver
     */
    public abstract Coordinates calculatePose(
            Constellation constellation
    );

    private static HashMap<String, Class<? extends PvtMethod>> registeredObjects = new HashMap<>();

    protected static void register(String constellationName, Class<?extends PvtMethod> objectClass) {
//        if(registeredObjects.containsKey(constellationName))
//            throw new IllegalArgumentException("This key is already registered! Select a different name!");
        if(!registeredObjects.containsKey(constellationName))
            registeredObjects.put(constellationName, objectClass);
    }

    public static Set<String> getRegistered(){
        return registeredObjects.keySet();
    }

    /**
     * @return Name of the PVT method, which is to be displayed in the UI
     */
    public abstract String getName();

    public static Class<? extends PvtMethod> getClassByName(String name) {
        return registeredObjects.get(name);
    }

    public abstract double getClockBias();

    private static boolean initialized = false;

    public static void initialize() {
        if(!initialized) {
            WeightedLeastSquares.registerClass();
            eu.foxcom.gnss_compare_core.PvtMethods.StaticExtendedKalmanFilter.registerClass();
            eu.foxcom.gnss_compare_core.PvtMethods.DynamicExtendedKalmanFilter.registerClass();
            PedestrianStaticExtendedKalmanFilter.registerClass();
            initialized = true;
        }
    }

    public void startLog(String name) { }

    public void stopLog() { }

    public void logError(double latError, double lonError) { }

    public void logFineLocation(Location location) {}
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */