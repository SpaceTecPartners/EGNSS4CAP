package eu.foxcom.gtphotos.model.gnss;

import android.content.Context;

import eu.foxcom.gtphotos.model.PersistData;
import eu.foxcom.gnss_scan.NMEAParser;

public class NMEAParserApp extends NMEAParser {
    public NMEAParserApp(Context context) {
        super(context);
        setCentroidFilter((ggaData) -> {
            NMEAParser nmeaExtractor = (NMEAParser) NMEAParserApp.this;
            Integer currMeanSnr = nmeaExtractor.getSNRSatellites().getMeanSnr();
            return  (!PersistData.getCentroidFilterActive(context) || (
                    ggaData.getSatelliteNumber() != null && ggaData.getSatelliteNumber() >= PersistData.getMinNumberSats(context)
                            && ggaData.getHdop() != null && ggaData.getHdop() >= PersistData.getMinHDOP(context)
                            && currMeanSnr != null && currMeanSnr >= PersistData.getMinSNR(context)
                            && nmeaExtractor.getLastFixValue() != null && nmeaExtractor.getLastFixValue() >= PersistData.getMinFix(context)
            ));
        });
        setSamplingNumber(PersistData.getSamplingNumber(context));
        // dynamic changes sampling Number
        setCentroidFinisher((ggaData, sampleProcessedNumber) -> sampleProcessedNumber >= PersistData.getSamplingNumber(context));
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
