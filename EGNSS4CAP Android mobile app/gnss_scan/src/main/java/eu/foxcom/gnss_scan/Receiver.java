package eu.foxcom.gnss_scan;

abstract class Receiver {

    protected String getCategory() {
        return null;
    }

    protected abstract void receiveVirtual(Holder holder);

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
