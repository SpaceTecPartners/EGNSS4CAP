package eu.foxcom.gtphotos.model.ekf;

public interface EkfReceiver {
    // !!! performed in an external thread
    void receive(EkfData ekfData);
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
