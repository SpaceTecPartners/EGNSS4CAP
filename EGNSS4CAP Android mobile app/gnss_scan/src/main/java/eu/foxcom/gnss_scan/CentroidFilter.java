package eu.foxcom.gnss_scan;

public interface CentroidFilter {
    boolean testSample(NMEAParser.GGAData ggaData);
}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */