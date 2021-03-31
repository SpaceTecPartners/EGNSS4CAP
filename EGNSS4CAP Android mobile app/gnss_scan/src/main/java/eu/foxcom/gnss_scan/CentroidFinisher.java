package eu.foxcom.gnss_scan;

public interface CentroidFinisher {
    boolean finish(NMEAParser.GGAData ggaData, int sampleProcessedNumber);
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */