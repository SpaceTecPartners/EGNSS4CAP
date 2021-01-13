package eu.foxcom.gnss_scan;

public interface CentroidFinisher {
    boolean finish(NMEAParser.GGAData ggaData, int sampleProcessedNumber);
}
