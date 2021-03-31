package eu.foxcom.gtphotos.model.ekf;

import eu.foxcom.gnss_compare_core.CalculationModule;

public class EkfCreateException extends EKFException {

    public EkfCreateException(String message) {
        super(message);
    }

    public EkfCreateException(CalculationModule.NameAlreadyRegisteredException e) {
        super(e);
    }

    public EkfCreateException(CalculationModule.NumberOfSeriesExceededLimitException e) {
        super(e);
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
