package eu.foxcom.stp.gsa.egnss4cap.model.ekf;

import android.content.Context;
import android.location.LocationManager;

import com.galfins.gogpsextracts.Coordinates;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.joda.time.DateTime;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import eu.foxcom.gnss_compare_core.CalculationModule;
import eu.foxcom.gnss_compare_core.CalculationModulesArrayList;
import eu.foxcom.gnss_compare_core.Core;

public class EkfController {

    private Context appContext;

    private Core ekfCore;

    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private Map<String, EkfCalculationModule> ekfCalculationModuleMap = new LinkedHashMap<String, EkfCalculationModule>();

    public EkfController(Context context) {
        this.appContext = context.getApplicationContext();

        ekfCore = new Core();

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void release() {
        stop();
        releaseAllModules();
    }

    private void bindAllModules() throws CalculationModule.NameAlreadyRegisteredException, CalculationModule.NumberOfSeriesExceededLimitException {
        for (String id : ekfCalculationModuleMap.keySet()) {
            EkfCalculationModule ekfCalculationModule = ekfCalculationModuleMap.get(id);
            if (ekfCalculationModule.isReleased) {
                ekfCalculationModule.bind();
                ekfCore.getCalculationModules().add(ekfCalculationModule.calculationModule);
            }
        }
    }

    public void addDefaultModules() throws EkfCreateException {
        try {
            addModule(EkfCalculationModule.createDefaultGalileoE1());
            addModule(EkfCalculationModule.createDefaultGalileoE5a());
            addModule(EkfCalculationModule.createDefaultGalileoIf());
            addModule(EkfCalculationModule.createDefaultGpsIf());
            addModule(EkfCalculationModule.createDefaultGpsL1());
            addModule(EkfCalculationModule.createDefaultGpsL5());
        } catch (CalculationModule.NameAlreadyRegisteredException e) {
            throw new EkfCreateException(e);
        } catch (CalculationModule.NumberOfSeriesExceededLimitException e) {
            throw new EkfCreateException(e);
        }
    }

    public void addModule(EkfCalculationModule ekfCalculationModule) {
        ekfCore.getCalculationModules().add(ekfCalculationModule.calculationModule);
        ekfCalculationModuleMap.put(ekfCalculationModule.id, ekfCalculationModule);
    }

    public void removeModule(String id) {
        releaseModule(id);
        EkfCalculationModule ekfCalculationModule = ekfCalculationModuleMap.get(id);
        ekfCalculationModuleMap.remove(ekfCalculationModule.id);
    }

    public void removeModule(EkfCalculationModule.DEFAULT_MODULE defaultModule) {
        removeModule(defaultModule.name());
    }

    public void releaseModule(String id) {
        EkfCalculationModule ekfCalculationModule = ekfCalculationModuleMap.get(id);
        CalculationModule calculationModule = ekfCalculationModule.calculationModule;
        ekfCore.getCalculationModules().removeAll(Collections.singleton(calculationModule));
        ekfCalculationModule.release();
    }

    public void releaseModule(EkfCalculationModule.DEFAULT_MODULE defaultModule) {
        releaseModule(defaultModule.name());
    }

    public EkfCalculationModule getModule(String id) {
        return ekfCalculationModuleMap.get(id);
    }

    public void removeAllModules() {
        Map<String, EkfCalculationModule> ekfCalModCopy = new HashMap<>(ekfCalculationModuleMap);
        for (String id : ekfCalModCopy.keySet()) {
            removeModule(id);
        }
    }

    public void releaseAllModules() {
        for (String id : ekfCalculationModuleMap.keySet()) {
            releaseModule(id);
        }
    }

    public EkfCalculationModule getModule(EkfCalculationModule.DEFAULT_MODULE defaultModule) {
        return getModule(defaultModule.name());
    }

    public void start() throws EKFStartExeception {
        try {
            bindAllModules();
        } catch (CalculationModule.NameAlreadyRegisteredException e) {
            throw new EKFStartExeception(e);
        } catch (CalculationModule.NumberOfSeriesExceededLimitException e) {
            throw new EKFStartExeception(e);
        }
        CalculationModulesArrayList.PoseUpdatedListener poseUpdatedListener = null;
        if (ekfCalculationModuleMap.size() > 0) {
            poseUpdatedListener = new CalculationModulesArrayList.PoseUpdatedListener() {
                @Override
                public void onPoseUpdated() {
                    updateMeasurement();
                }
            };
        }
        ekfCore.registerGnssUpdate(fusedLocationProviderClient, locationManager, poseUpdatedListener);
    }

    public void stop() {
        ekfCore.unregisterGnssUpdate();
    }

    private void updateMeasurement() {
        for (String id : ekfCalculationModuleMap.keySet()) {
            EkfCalculationModule ekfCalculationModule = ekfCalculationModuleMap.get(id);
            EkfData ekfData = new EkfData();
            Coordinates coordinates = ekfCalculationModule.calculationModule.getPose();
            ekfData.latitude = coordinates.getGeodeticLatitude();
            ekfData.longitude = coordinates.getGeodeticLongitude();
            ekfData.altitude = coordinates.getGeodeticHeight();
            if (coordinates.getRefTime() != null) {
                ekfData.referenceTime = new DateTime(coordinates.getRefTime().getMsec());
            } else if (ekfCalculationModule.ekfData != null && ekfCalculationModule.ekfData.referenceTime != null) {
                ekfData.referenceTime = ekfCalculationModule.ekfData.referenceTime;
            }
            ekfData.computedTime = DateTime.now();
            ekfCalculationModule.ekfData = ekfData;
            ekfCalculationModule.callReceiver();
        }
    }

    // region get, set

    // endregion

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
