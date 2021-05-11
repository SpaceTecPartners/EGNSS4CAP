package eu.foxcom.stp.gsa.egnss4cap.model.ekf;

import org.joda.time.DateTime;

public class EkfData {
    Double latitude;
    Double longitude;
    Double altitude;
    DateTime referenceTime;
    DateTime computedTime;

    // region get, set

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public DateTime getReferenceTime() {
        return referenceTime;
    }

    public DateTime getComputedTime() {
        return computedTime;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
