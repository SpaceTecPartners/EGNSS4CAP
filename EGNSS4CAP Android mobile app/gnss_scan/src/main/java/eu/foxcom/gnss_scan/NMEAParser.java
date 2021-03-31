package eu.foxcom.gnss_scan;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import eu.foxcom.convex_hull.Cluster;
import eu.foxcom.convex_hull.Point;

public class NMEAParser extends NMEAScanner.NMEAReceiver {

    public enum SATELLITE_TYPE {
        GALILEO("ga"),
        GPS("gp"),
        GLONASS("gl"),
        BEIDOU("bd");

        public final String ID;

        SATELLITE_TYPE(String id) {
            ID = id;
        }
    }

    public static abstract class GGAReceiver {
        public abstract void receive(GGAData GGAData);
    }

    public static abstract class GSVReceiver {
        public abstract void receive(List<GSVData> nmeagsvDataList, SATELLITE_TYPE SATELLITETYPE);
    }

    public static abstract class RMCReceiver {
        public abstract void receive(RMCData RMCData);
    }

    public static abstract class VTGReceiver {
        public abstract void receive(VTGData vtgData);
    }

    public static abstract class GSAReceiver {
        public abstract void receive(GSAData gsaData);
    }

    public static abstract class PrecisionReceiver {
        public abstract void receive(double distance);
    }

    public static abstract class CentroidComputedReceiver {
        public abstract void receive(double latitude, double longitude);
    }

    public static abstract class CentroidSampleAddReceiver {
        public abstract void receive(int count);
    }

    public static class SNRSatellites {
        public static class Satellite {
            private String prn;
            private Integer snr;
            private Integer elevation;
            private Integer azimuth;
            private String band;
            private long timestamp;

            private Satellite(String prn, Integer snr, Integer elevation, Integer azimuth, String band) {
                this.prn = prn;
                this.snr = snr;
                this.elevation = elevation;
                this.azimuth = azimuth;
                this.band = band;
                this.timestamp = new Date().getTime();
            }

            // region get, set

            public String getPrn() {
                return prn;
            }

            public Integer getSnr() {
                return snr;
            }

            public Integer getElevation() {
                return elevation;
            }

            public Integer getAzimuth() {
                return azimuth;
            }

            public String getBand() {
                return band;
            }

            public long getTimestamp() {
                return timestamp;
            }

            // endregion
        }

        protected Map<String, Satellite> gp = new HashMap<>();
        protected Map<String, Satellite> gl = new HashMap<>();
        protected Map<String, Satellite> ga = new HashMap<>();
        protected Map<String, Satellite> bd = new HashMap<>();

        private SNRSatellites() {

        }

        protected void addSatellite(SATELLITE_TYPE SATELLITETYPE, Satellite satellite) {
            switch (SATELLITETYPE) {
                case GPS:
                    gp.put(satellite.prn, satellite);
                    break;
                case GLONASS:
                    gl.put(satellite.prn, satellite);
                    break;
                case GALILEO:
                    ga.put(satellite.prn, satellite);
                    break;
                case BEIDOU:
                    bd.put(satellite.prn, satellite);
                    break;
            }
        }

        public Map<String, Map<String, Satellite>> getListNets() {
            Map<String, Map<String, Satellite>> nets = new HashMap<String, Map<String, Satellite>>() {{
                put(SATELLITE_TYPE.GPS.ID, gp);
                put(SATELLITE_TYPE.GLONASS.ID, gl);
                put(SATELLITE_TYPE.GALILEO.ID, ga);
                put(SATELLITE_TYPE.BEIDOU.ID, bd);
            }};
            return nets;
        }

        public Integer getMeanSnr() {
            Map<String, Map<String, Satellite>> nets = getListNets();
            List<Integer> snrListTemp = new ArrayList<>();
            for (Map.Entry<String, Map<String, Satellite>> entry : nets.entrySet()) {
                for (Map.Entry<String, Satellite> entryNet : entry.getValue().entrySet()) {
                    Satellite satellite = entryNet.getValue();
                    if (satellite.snr != null) {
                        snrListTemp.add(satellite.snr);
                    }
                }
            }
            return computeMeanSnr(snrListTemp);
        }

        public JSONArray toCurrSatsInfo() throws JSONException {
            JSONArray jsonArray = new JSONArray();
            Map<String, Map<String, Satellite>> nets = getListNets();
            for (Map.Entry<String, Map<String, Satellite>> netEntry : nets.entrySet()) {
                Map<String, Satellite> net = netEntry.getValue();
                for (Map.Entry<String, Satellite> prnEntry : net.entrySet()) {
                    String prn = prnEntry.getKey();
                    Satellite satellite = prnEntry.getValue();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("prn", prn);
                    jsonObject.put("snr", satellite.getSnr());
                    jsonObject.put("elevation", satellite.getElevation());
                    jsonObject.put("azimuth", satellite.getAzimuth());
                    jsonObject.put("band", satellite.getBand());
                    jsonObject.put("timestamp", satellite.getTimestamp());
                    jsonArray.put(jsonObject);
                }
            }
            return jsonArray;
        }

        // region get, set

        public Map<String, Satellite> getGp() {
            return new HashMap<>(gp);
        }

        public Map<String, Satellite> getGl() {
            return new HashMap<>(gl);
        }

        public Map<String, Satellite> getGa() {
            return new HashMap<>(ga);
        }

        public Map<String, Satellite> getBd() {
            return new HashMap<>(bd);
        }

        // endregion
    }

    public class GSVData {
        private String prn;
        private String snr;
        private String elevation;
        private String azimuth;
        private String band;

        // region get, set

        public String getPrn() {
            return prn;
        }

        public String getSnr() {
            return snr;
        }

        public String getElevation() {
            return elevation;
        }

        public String getAzimuth() {
            return azimuth;
        }

        public String getBand() {
            return band;
        }


        // endregion
    }

    public class GGAData {
        private Double latitude;
        private Double longitude;
        private Double altitude;
        private String NS;
        private String EW;
        private Integer satteliteNumber;
        private String fixType;
        private Double hdop;

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

        public String getNS() {
            return NS;
        }

        public String getEW() {
            return EW;
        }

        public Integer getSatelliteNumber() {
            return satteliteNumber;
        }

        public String getFixType() {
            return fixType;
        }

        public Double getHdop() {
            return hdop;
        }

        // endregion
    }

    public class RMCData {
        String magnVar;
        String ewMagn;
        String trackMG;
        String speedKnots;

        // region get, set

        public String getMagnVar() {
            return magnVar;
        }

        public String getEwMagn() {
            return ewMagn;
        }

        public String getTrackMG() {
            return trackMG;
        }

        public String getSpeedKnots() {
            return speedKnots;
        }

        // endregion
    }

    public class VTGData {
        String realDeg;
        String magDeg;
        String speedKnots;
        String speedKmh;

        // region get, set

        public String getRealDeg() {
            return realDeg;
        }

        public String getMagDeg() {
            return magDeg;
        }

        public String getSpeedKnots() {
            return speedKnots;
        }

        public String getSpeedKmh() {
            return speedKmh;
        }


        // endregion
    }

    public class GSAData {
        String fixMode;
        Double hdop;
        Double pdop;
        Double vdop;

        // region get, set

        public String getFixMode() {
            return fixMode;
        }

        public Double getHdop() {
            return hdop;
        }

        public Double getPdop() {
            return pdop;
        }

        public Double getVdop() {
            return vdop;
        }

        // endregion
    }

    public static final String TAG = NMEAParser.class.getSimpleName();
    private static final String UNKNOWN_SIGN = "n/d";

    private Context context;

    private int maxNMEAchars = 10000;
    private String currentNmeaMessage;
    private String totalNmeaMessage = "";
    private int nmeaChars = 0;
    private SNRSatellites SNRSatellites;
    private Cluster cluster;
    private Integer lastFixValue;
    private int samplingNumber = 10;
    // has higher priority than exceeding samplingNumber
    private CentroidFinisher centroidFinisher;

    private GGAReceiver ggaReceiver;
    private GSVReceiver gsvReceiver;
    private RMCReceiver RMCReceiver;
    private VTGReceiver vtgReceiver;
    private GSAReceiver gsaReceiver;
    private PrecisionReceiver precisionReceiver;
    private CentroidComputedReceiver centroidComputedReceiver;
    private CentroidSampleAddReceiver centroidSampleAddReceiver;
    private CentroidFilter centroidFilter;

    private static Integer computeMeanSnr(List<Integer> snrL) {
        List<Integer> snrList = new ArrayList<>(snrL);
        if (snrL.size() < 3) {
            return null;
        }
        int minSnr = 99999;
        int maxSnr = 0;
        int minSnrIndx = 0;
        int maxSnrIndx = 0;

        for (int i = 0; i < snrList.size(); ++i) {
            if (snrList.get(i) > maxSnr) {
                maxSnr = snrList.get(i);
                maxSnrIndx = i;
            }
        }
        snrList.remove(maxSnrIndx);
        for (int i = 0; i < snrList.size(); ++i) {
            if (snrList.get(i) < minSnr) {
                minSnr = snrList.get(i);
                minSnrIndx = i;
            }
        }

        snrList.remove(minSnrIndx);

        int meanSnr = 0;
        for (double snr : snrList) {
            meanSnr += snr;
        }
        meanSnr = (int) Math.floor(meanSnr / snrList.size());
        return meanSnr;
    }

    public NMEAParser(Context context) {
        this.context = context;
        this.SNRSatellites = new SNRSatellites();
        this.cluster = new Cluster();
    }

    public NMEAParser(Context context, int maxNMEAchars) {
        this(context);
        this.maxNMEAchars = maxNMEAchars;
    }

    @Override
    public void receive(NMEAScanner.NMEAHolder nmeaHolder) {
        currentNmeaMessage = nmeaHolder.getNmeaMessage();
        nmeaRawMessage();
        parseNMEAMessage();
    }

    private void nmeaRawMessage() {
        if (currentNmeaMessage == null) {
            return;
        }
        int nmeaMessageLength = currentNmeaMessage.length();
        if (nmeaChars + nmeaMessageLength <= maxNMEAchars) {
            totalNmeaMessage += currentNmeaMessage;
            nmeaChars += nmeaMessageLength;
        }
    }

    private void parseNMEAMessage() {

        String[] splitted = currentNmeaMessage.split(",");
        String prefix = splitted[0].substring(1);
        switch (prefix) {
            // GSV
            case "GAGSV":
                parseGSV(splitted, SATELLITE_TYPE.GALILEO);
                break;
            case "GPGSV":
                parseGSV(splitted, SATELLITE_TYPE.GPS);
                break;
            case "GLGSV":
                parseGSV(splitted, SATELLITE_TYPE.GLONASS);
                break;
            case "BDGSV":
                parseGSV(splitted, SATELLITE_TYPE.BEIDOU);
                break;
            // GGA
            case "GNGGA":
                parseGGA(splitted);
                break;
            case "GPGGA":
                parseGGA(splitted);
                break;
            case "GLGGA":
                parseGGA(splitted);
                break;
            case "GAGGA":
                parseGGA(splitted);
                break;
            // RMC
            case "GNRMC":
                parseRMC(splitted);
                break;
            case "GPRMC":
                parseRMC(splitted);
                break;
            case "GLRMC":
                parseRMC(splitted);
                break;
            case "GARMC":
                parseRMC(splitted);
                break;
            // VTG
            case "GNVTG":
                parseVTG(splitted);
                break;
            // GSA
            case "GPGSA":
                parseGSA(splitted);
                break;
            case "GLGSA":
                parseGSA(splitted);
                break;
            case "GNGSA":
                parseGSA(splitted);
                break;
        }
    }

    private void parseGSV(String[] splitted, SATELLITE_TYPE SATELLITETYPE) {
        String freq = "E1";
        splitted[splitted.length - 1] = splitted[splitted.length - 1].split("\\*")[0];

        if (SATELLITE_TYPE.GALILEO == SATELLITETYPE) {
            if ("1".equals(splitted[splitted.length - 1])) {
                freq = "E5";
            }
        }

        if (SATELLITE_TYPE.GPS == SATELLITETYPE) {
            if ("8".equals(splitted[splitted.length - 1])) {
                freq = "L5";
            } else {
                freq = "L1";
            }
        }
        List<GSVData> gsvDataList = new ArrayList<>();
        int numBlock = (splitted.length - 4) / 4;
        for (int i = 1; i <= numBlock; ++i) {
            if (splitted.length >= (4 * i) + 3) {
                GSVData nmeagsvData = new GSVData();
                nmeagsvData.prn = splitted[4 * i];
                nmeagsvData.elevation = splitted[(4 * i) + 1];
                nmeagsvData.azimuth = splitted[(4 * i) + 2];
                nmeagsvData.snr = splitted[(4 * i) + 3];
                if (SATELLITE_TYPE.GALILEO == SATELLITETYPE || SATELLITE_TYPE.GPS == SATELLITETYPE) {
                    nmeagsvData.band = freq;
                    nmeagsvData.prn = nmeagsvData.prn + "_" + nmeagsvData.band;
                } else {
                    nmeagsvData.band = "n/d";
                }
                if (nmeagsvData.snr.trim().isEmpty()) {
                    continue;
                }
                try {
                    SNRSatellites.addSatellite(SATELLITETYPE, new SNRSatellites.Satellite(nmeagsvData.prn, Util.parseNullableInteger(nmeagsvData.snr), Util.parseNullableInteger(nmeagsvData.elevation), Util.parseNullableInteger(nmeagsvData.azimuth), nmeagsvData.getBand()));
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error additing object.", e);
                }
                gsvDataList.add(nmeagsvData);
            }
        }
        GSVReceiverUpdate(gsvDataList, SATELLITETYPE);
    }

    private void parseGGA(String[] sp) {
        List<String> splittedList = new ArrayList<>(Arrays.asList(sp));
        if (splittedList.size() < 15) {
            for (int i = splittedList.size() - 1; i <= 15; ++i) {
                splittedList.add(UNKNOWN_SIGN);
            }
        }
        GGAData ggaData = new GGAData();
        ggaData.latitude = parseGGADegMinToDec(splittedList.get(2));
        ggaData.longitude = parseGGADegMinToDec(splittedList.get(4));
        Double alt = null;
        if (splittedList.get(9) != null && !splittedList.get(9).isEmpty()) {
            alt = Double.valueOf(splittedList.get(9));
        }
        ggaData.altitude = alt;
        ggaData.NS = splittedList.get(3);
        ggaData.EW = splittedList.get(5);
        if ("S".equals(ggaData.NS) && ggaData.latitude != null) {
            ggaData.latitude *= -1;
        }
        if ("W".equals(ggaData.EW) && ggaData.longitude != null) {
            ggaData.longitude *= -1;
        }
        ggaData.satteliteNumber = (splittedList.get(7).isEmpty() ? null : Util.parseNullableInteger(splittedList.get(7)));
        ggaData.fixType = (splittedList.get(6).isEmpty() ? null : splittedList.get(6));
        ggaData.hdop = splittedList.get(8).isEmpty() ? null : Double.valueOf(splittedList.get(8));
        GGAReceiverUpdate(ggaData);
        computeCentroid(ggaData);
    }

    private void parseRMC(String[] sp) {
        List<String> splitted = new ArrayList<>(Arrays.asList(sp));
        if (splitted.size() < 13) {
            for (int i = splitted.size() - 1; i <= 13; ++i) {
                splitted.add(UNKNOWN_SIGN);
            }
        }
        RMCData rmsData = new RMCData();
        rmsData.magnVar = splitted.get(10);
        rmsData.ewMagn = splitted.get(11);
        rmsData.trackMG = splitted.get(8);
        rmsData.speedKnots = splitted.get(7);
        MCRReceiverUpdate(rmsData);
    }

    private void parseVTG(String[] sp) {
        List<String> splitted = new ArrayList<>(Arrays.asList(sp));
        if (splitted.size() < 11) {
            for (int i = splitted.size() - 1; i <= 11; ++i) {
                splitted.add(UNKNOWN_SIGN);
            }
        }
        VTGData vtgData = new VTGData();
        vtgData.realDeg = splitted.get(1);
        vtgData.magDeg = splitted.get(3);
        vtgData.speedKnots = splitted.get(5);
        vtgData.speedKmh = splitted.get(7);
        VTGReceiverUpdate(vtgData);
    }

    private void parseGSA(String[] sp) {
        List<String> splitted = new ArrayList<>(Arrays.asList(sp));
        if (splitted.size() < 18) {
            for (int i = splitted.size() - 1; i <= 18; ++i) {
                splitted.add(UNKNOWN_SIGN);
            }
        }
        GSAData gsaData = new GSAData();
        gsaData.fixMode = splitted.get(2);
        lastFixValue = gsaData.fixMode == null ? null : Integer.valueOf(gsaData.fixMode);
        if (splitted.get(16) != null && !splitted.get(16).isEmpty()) {
            gsaData.hdop = Double.valueOf(splitted.get(16));
        }
        if (splitted.get(15) != null && !splitted.get(15).isEmpty()) {
            gsaData.pdop = Double.valueOf(splitted.get(15));
        }
        if (splitted.get(17) != null && !splitted.get(17).isEmpty()) {
            String vdopS = splitted.get(17).split("\\*")[0];
            if (!vdopS.isEmpty()) {
                gsaData.vdop = Double.valueOf(vdopS);
            }
        }
        GSAReceiverUpdate(gsaData);
    }

    private void GGAReceiverUpdate(GGAData GGAData) {
        if (ggaReceiver != null) {
            ggaReceiver.receive(GGAData);
        }
    }

    private void GSVReceiverUpdate(List<GSVData> gsvDataList, SATELLITE_TYPE SATELLITETYPE) {
        if (gsvReceiver != null) {
            gsvReceiver.receive(gsvDataList, SATELLITETYPE);
        }
    }

    private void MCRReceiverUpdate(RMCData rmcData) {
        if (RMCReceiver != null) {
            RMCReceiver.receive(rmcData);
        }
    }

    private void VTGReceiverUpdate(VTGData vtgData) {
        if (vtgReceiver != null) {
            vtgReceiver.receive(vtgData);
        }
    }

    private void GSAReceiverUpdate(GSAData gsaData) {
        if (gsaReceiver != null) {
            gsaReceiver.receive(gsaData);
        }
    }

    private void PrecisionRecevierUpdate(double distance) {
        if (precisionReceiver != null) {
            precisionReceiver.receive(distance);
        }
    }

    private void CentroidComputedReceiverUpdate(double latitude, double longitude) {
        if (centroidComputedReceiver != null) {
            centroidComputedReceiver.receive(latitude, longitude);
        }
    }

    private void CentroidSamplaAddReceiverUpdate(int count) {
        if (centroidSampleAddReceiver != null) {
            centroidSampleAddReceiver.receive(count);
        }
    }

    private Double parseGGADegMinToDec(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        int delim = value.indexOf(".");
        if (delim >= 2) {
            delim -= 2;
        }
        if (delim < 0) {
            delim = value.length();
        }
        Double deg = Double.valueOf(value.substring(0, delim));
        Double min = Double.valueOf(value.substring(delim));
        return deg + (min / 60);
    }

    private void computeCentroid(GGAData ggaData) {
        if (ggaData.getLatitude() == null || ggaData.getLongitude() == null) {
            return;
        }
        if (centroidSampleAddReceiver == null && centroidComputedReceiver == null && precisionReceiver == null) {
            return;
        }
        if (centroidFilter == null || centroidFilter.testSample(ggaData)) {
            cluster.addPoint(new Point(ggaData.getLatitude(), ggaData.getLongitude()));
            CentroidSamplaAddReceiverUpdate(cluster.getSize());
        }
        int processedSampleNumber = cluster.getSize();
        if ((centroidFinisher != null && centroidFinisher.finish(ggaData, processedSampleNumber))
                || (centroidFinisher == null && processedSampleNumber >= samplingNumber)) {
            List<Point> lastPerimeter = new ArrayList<>();
            Point centroid = cluster.computeCentroid(lastPerimeter);
            CentroidComputedReceiverUpdate(centroid.getX(), centroid.getY());
            PrecisionRecevierUpdate(calculateDistance(centroid, lastPerimeter));
            cluster.reset();
        }
    }

    private double calculateDistance(Point centroid, List<Point> perimeter) {
        double sum = 0;
        Location centroidLocation = new Location("");
        centroidLocation.setLatitude(centroid.getX());
        centroidLocation.setLongitude(centroid.getY());
        for (Point point : perimeter) {
            Location perimLocation = new Location("");
            perimLocation.setLatitude(point.getX());
            perimLocation.setLongitude(point.getY());
            sum += centroidLocation.distanceTo(perimLocation);
        }
        return sum / perimeter.size();
    }

    public void resetSNRSattelites() {
        SNRSatellites = new SNRSatellites();
    }

    public void resetNMEATotalMessage() {
        nmeaChars = 0;
        totalNmeaMessage = null;
    }

    // region get, set


    public Integer getLastFixValue() {
        return lastFixValue;
    }

    public String getCurrentNmeaMessage() {
        return currentNmeaMessage;
    }

    public String getNmeaTotalMessage() {
        return totalNmeaMessage;
    }

    public int getNmeaChars() {
        return nmeaChars;
    }

    public void setGgaReceiver(GGAReceiver ggaReceiver) {
        this.ggaReceiver = ggaReceiver;
    }

    public void setGsvReceiver(GSVReceiver gsvReceiver) {
        this.gsvReceiver = gsvReceiver;
    }

    public void setRMCReceiver(NMEAParser.RMCReceiver RMCReceiver) {
        this.RMCReceiver = RMCReceiver;
    }

    public void setVtgReceiver(VTGReceiver vtgReceiver) {
        this.vtgReceiver = vtgReceiver;
    }

    public void setGsaReceiver(GSAReceiver gsaReceiver) {
        this.gsaReceiver = gsaReceiver;
    }

    public void setPrecisionReceiver(PrecisionReceiver precisionReceiver) {
        this.precisionReceiver = precisionReceiver;
    }

    public void setCentroidComputedReceiver(CentroidComputedReceiver centroidComputedReceiver) {
        this.centroidComputedReceiver = centroidComputedReceiver;
    }

    public void setCentroidSampleAddReceiver(CentroidSampleAddReceiver centroidSampleAddReceiver) {
        this.centroidSampleAddReceiver = centroidSampleAddReceiver;
    }

    public void setCentroidFilter(CentroidFilter centroidFilter) {
        this.centroidFilter = centroidFilter;
    }

    public int getSamplingNumber() {
        return samplingNumber;
    }

    public void setSamplingNumber(int samplingNumber) {
        this.samplingNumber = samplingNumber;
    }

    public void setCentroidFinisher(CentroidFinisher centroidFinisher) {
        this.centroidFinisher = centroidFinisher;
    }

    public int getMaxNMEAchars() {
        return maxNMEAchars;
    }

    public void setMaxNMEAchars(int maxNMEAchars) {
        this.maxNMEAchars = maxNMEAchars;
    }

    public NMEAParser.SNRSatellites getSNRSatellites() {
        return SNRSatellites;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
