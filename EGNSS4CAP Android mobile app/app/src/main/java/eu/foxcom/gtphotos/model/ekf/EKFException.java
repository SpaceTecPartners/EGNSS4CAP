package eu.foxcom.gtphotos.model.ekf;

import eu.foxcom.gnss_compare_core.CalculationModule;

class EKFException extends Exception {
    CalculationModule.NameAlreadyRegisteredException nameAlreadyRegisteredException;
    CalculationModule.NumberOfSeriesExceededLimitException numberOfSeriesExceededLimitException;

    public EKFException(String message) {
        super(message);
    }

    public EKFException(CalculationModule.NameAlreadyRegisteredException e) {
        super(e);
        this.nameAlreadyRegisteredException = e;
    }

    public EKFException(CalculationModule.NumberOfSeriesExceededLimitException e) {
        super(e);
        this.numberOfSeriesExceededLimitException = e;
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
