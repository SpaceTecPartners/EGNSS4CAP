package eu.foxcom.gtphotos.model.ekf;

import eu.foxcom.gnss_compare_core.CalculationModule;

public class EKFStartExeception extends EKFException {
    public EKFStartExeception(String message) {
        super(message);
    }

    public EKFStartExeception(CalculationModule.NameAlreadyRegisteredException e) {
        super(e);
    }

    public EKFStartExeception(CalculationModule.NumberOfSeriesExceededLimitException e) {
        super(e);
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
