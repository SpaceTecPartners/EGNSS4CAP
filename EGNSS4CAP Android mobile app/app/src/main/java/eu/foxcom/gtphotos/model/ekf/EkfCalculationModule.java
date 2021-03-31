package eu.foxcom.gtphotos.model.ekf;

import java.util.ArrayList;

import eu.foxcom.gnss_compare_core.CalculationModule;
import eu.foxcom.gnss_compare_core.Constellations.GalileoConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GalileoE5aConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GalileoIonoFreeConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GpsConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GpsIonoFreeConstellation;
import eu.foxcom.gnss_compare_core.Constellations.GpsL5Constellation;
import eu.foxcom.gnss_compare_core.Corrections.Correction;
import eu.foxcom.gnss_compare_core.Corrections.IonoCorrection;
import eu.foxcom.gnss_compare_core.Corrections.ShapiroCorrection;
import eu.foxcom.gnss_compare_core.Corrections.TropoCorrection;
import eu.foxcom.gnss_compare_core.FileLoggers.SimpleFileLogger;
import eu.foxcom.gnss_compare_core.PvtMethods.StaticExtendedKalmanFilter;

public class EkfCalculationModule {

    public enum DEFAULT_MODULE {
        GALILEO_E5A,
        GPS_IF,
        GALILEO_IF,
        GALILEO_E1,
        GPS_L5,
        GPS_L1
    }

    public static EkfCalculationModule createDefaultGalileoE5a() throws CalculationModule.NameAlreadyRegisteredException, CalculationModule.NumberOfSeriesExceededLimitException {
        return new EkfCalculationModule(DEFAULT_MODULE.GALILEO_E5A.name(), new CalculationModule(
                DEFAULT_MODULE.GALILEO_E5A.name(),
                GalileoE5aConstellation.class,
                new ArrayList<Class<? extends Correction>>() {{
                    add(ShapiroCorrection.class);
                    add(TropoCorrection.class);
                }},
                StaticExtendedKalmanFilter.class,
                SimpleFileLogger.class));
    }

    public static EkfCalculationModule createDefaultGpsIf() throws CalculationModule.NameAlreadyRegisteredException, CalculationModule.NumberOfSeriesExceededLimitException {
        return new EkfCalculationModule(DEFAULT_MODULE.GPS_IF.name(), new CalculationModule(
                DEFAULT_MODULE.GPS_IF.name(),
                GpsIonoFreeConstellation.class,
                new ArrayList<Class<? extends Correction>>() {{
                    add(ShapiroCorrection.class);
                    add(TropoCorrection.class);
                    add(IonoCorrection.class);
                }},
                StaticExtendedKalmanFilter.class,
                SimpleFileLogger.class));
    }

    public static EkfCalculationModule createDefaultGalileoIf() throws CalculationModule.NameAlreadyRegisteredException, CalculationModule.NumberOfSeriesExceededLimitException {
        return new EkfCalculationModule(DEFAULT_MODULE.GALILEO_IF.name(), new CalculationModule(
                DEFAULT_MODULE.GALILEO_IF.name(),
                GalileoIonoFreeConstellation.class,
                new ArrayList<Class<? extends Correction>>() {{
                    add(ShapiroCorrection.class);
                    add(TropoCorrection.class);
                }},
                StaticExtendedKalmanFilter.class,
                SimpleFileLogger.class));
    }

    public static EkfCalculationModule createDefaultGalileoE1() throws CalculationModule.NameAlreadyRegisteredException, CalculationModule.NumberOfSeriesExceededLimitException {
        return new EkfCalculationModule(DEFAULT_MODULE.GALILEO_E1.name(), new CalculationModule(
                DEFAULT_MODULE.GALILEO_E1.name(),
                GalileoConstellation.class,
                new ArrayList<Class<? extends Correction>>() {{
                    add(ShapiroCorrection.class);
                    add(TropoCorrection.class);
                }},
                StaticExtendedKalmanFilter.class,
                SimpleFileLogger.class));
    }

    public static EkfCalculationModule createDefaultGpsL5() throws CalculationModule.NameAlreadyRegisteredException, CalculationModule.NumberOfSeriesExceededLimitException {
        return new EkfCalculationModule(DEFAULT_MODULE.GPS_L5.name(), new CalculationModule(
                DEFAULT_MODULE.GPS_L5.name(),
                GpsL5Constellation.class,
                new ArrayList<Class<? extends Correction>>() {{
                    add(ShapiroCorrection.class);
                    add(TropoCorrection.class);
                    add(IonoCorrection.class);
                }},
                StaticExtendedKalmanFilter.class,
                SimpleFileLogger.class));
    }

    public static EkfCalculationModule createDefaultGpsL1() throws CalculationModule.NameAlreadyRegisteredException, CalculationModule.NumberOfSeriesExceededLimitException {
        return new EkfCalculationModule(DEFAULT_MODULE.GPS_L1.name(), new CalculationModule(
                DEFAULT_MODULE.GPS_L1.name(),
                GpsConstellation.class,
                new ArrayList<Class<? extends Correction>>() {{
                    add(ShapiroCorrection.class);
                    add(TropoCorrection.class);
                    add(IonoCorrection.class);
                }},
                StaticExtendedKalmanFilter.class,
                SimpleFileLogger.class));
    }

    CalculationModule calculationModule;
    String id;
    EkfReceiver ekfReceiver;
    EkfData ekfData;
    boolean isReleased = false;

    private EkfCalculationModule() {

    }

    private EkfCalculationModule(String id, CalculationModule calculationModule) {
        this.id = id;
        this.calculationModule = calculationModule;
    }

    void callReceiver() {
        if (ekfReceiver == null) {
            return;
        }
        ekfReceiver.receive(ekfData);
    }

    void release() {
        calculationModule.release();
        isReleased = true;
    }

    void bind() throws CalculationModule.NameAlreadyRegisteredException, CalculationModule.NumberOfSeriesExceededLimitException {
        calculationModule.bind(calculationModule.getName());
        isReleased = false;
    }

    // region get, set

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EkfReceiver getEkfReceiver() {
        return ekfReceiver;
    }

    public void setEkfReceiver(EkfReceiver ekfReceiver) {
        this.ekfReceiver = ekfReceiver;
    }

    public EkfData getEkfData() {
        return ekfData;
    }

    // endregion

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
