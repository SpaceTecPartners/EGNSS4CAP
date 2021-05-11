package eu.foxcom.stp.gsa.egnss4cap.model.mock;

import java.util.ArrayList;
import java.util.List;

public class Gnss {

    public void GSVTest() {
        String[] splitted = "$GAGSV,1,1,01,09,16,206,20,1*4C".split(",");

        String freq = "E1";
        splitted[splitted.length - 1] = splitted[splitted.length - 1].split("\\*")[0];

        if (true) {
            if ("1".equals(splitted[splitted.length - 1])) {
                freq = "E5";
            }
        }

        class GSVData {
            private String prn;
            private String snr;
            private String elevation;
            private String azimuth;
            private String band;
        }

        List<GSVData> gsvDataList = new ArrayList<>();
        int numBlock = (splitted.length - 4) / 4;
        for (int i = 1; i < numBlock; ++i) {
            if (splitted.length >= (4 * i) + 3) {
                GSVData nmeagsvData = new GSVData();
                nmeagsvData.prn = splitted[4 * i];
                nmeagsvData.elevation = splitted[(4 * i) + 1];
                nmeagsvData.azimuth = splitted[(4 * i) + 2];
                nmeagsvData.snr = splitted[(4 * i) + 3];
                if (true) {
                    nmeagsvData.band = freq;
                    nmeagsvData.prn = nmeagsvData.prn + "_" + nmeagsvData.band;
                } else {
                    nmeagsvData.band = "n/d";
                }
                if (nmeagsvData.snr.trim().isEmpty()) {
                    continue;
                }
                gsvDataList.add(nmeagsvData);
            }
        }
    }
}
