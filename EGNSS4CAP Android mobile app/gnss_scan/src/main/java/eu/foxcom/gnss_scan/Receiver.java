package eu.foxcom.gnss_scan;

abstract class Receiver {

    protected String getCategory() {
        return null;
    }

    protected abstract void receiveVirtual(Holder holder);

}
