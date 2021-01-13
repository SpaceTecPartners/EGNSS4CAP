package eu.foxcom.gnss_scan;

public interface CentroidFilter {
    boolean testSample(NMEAParser.GGAData ggaData);
}
