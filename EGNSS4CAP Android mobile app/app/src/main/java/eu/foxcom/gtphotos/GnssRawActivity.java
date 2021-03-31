package eu.foxcom.gtphotos;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.foxcom.gtphotos.model.gnss.NMEAParserApp;
import eu.foxcom.gnss_scan.GnssStatusScanner;
import eu.foxcom.gnss_scan.NMEAParser;
import eu.foxcom.gnss_scan.NMEAScanner;

public class GnssRawActivity extends BaseActivity {

    class SatsListAdapter extends ArrayAdapter<String> {

        List<String> values;

        public SatsListAdapter(@NonNull Context context, List<String> values) {
            super(context, R.layout.sats_list_item, R.id.sli_textView_value, values);
            this.values = values;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.sats_list_item, parent, false);
            }

            String value = values.get(position);
            TextView valueTextView = convertView.findViewById(R.id.sli_textView_value);
            valueTextView.setText(value);
            return super.getView(position, convertView, parent);
        }
    }

    public static final String TAG = GnssRawActivity.class.getSimpleName();
    public static final int REQUEST_LOCATION_UPDATE_TIMEOUT = 5000;
    public static final int UPDATE_SATS_NUMBER_INTERVAL = 4000;

    DecimalFormat decimalFormat;
    DecimalFormat decimalFormat8;

    private GnssStatusScanner gnssStatusUnit;
    private NMEAScanner nmeaScanner;
    private NMEAParser nmeaParser;
    private NMEAParser.GGAData ggaData;

    private Handler updateSatsHandler;
    private Runnable updateSatsRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gnss_raw);
        setToolbar(R.id.toolbar);

        decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
        decimalFormat8 = new DecimalFormat("#.########");
        decimalFormat8.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            goToStartActivity();
            return;
        }

        gnssStatusUnit = new GnssStatusScanner(this);
        gnssStatusUnit.startScan(REQUEST_LOCATION_UPDATE_TIMEOUT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nmeaScanner = new NMEAScanner(this);

            nmeaParser = new NMEAParserApp(this);
            nmeaScanner.registerReceiver(nmeaParser);
            nmeaParser.setGgaReceiver(new NMEAParser.GGAReceiver() {
                @Override
                public void receive(NMEAParser.GGAData GGAData) {
                    ggaData = GGAData;
                    updateGeolocationInfoGGA();
                }
            });
            nmeaParser.setRMCReceiver(new NMEAParser.RMCReceiver() {
                @Override
                public void receive(NMEAParser.RMCData rmcData) {
                    updateRMCInfo(rmcData);
                }
            });
            nmeaParser.setVtgReceiver(new NMEAParser.VTGReceiver() {
                @Override
                public void receive(NMEAParser.VTGData vtgData) {
                    updateVTGInfo(vtgData);
                }
            });
            nmeaParser.setGsaReceiver(new NMEAParser.GSAReceiver() {
                @Override
                public void receive(NMEAParser.GSAData gsaData) {
                    updateGeolocationInfoGSA(gsaData);
                }
            });
            nmeaParser.setPrecisionReceiver(new NMEAParser.PrecisionReceiver() {
                @Override
                public void receive(double distance) {
                    updatePrecision(distance);
                }
            });
            nmeaScanner.startScan();
            updateSatsHandler = new Handler();
            updateSatsRunnable = new Runnable() {
                @Override
                public void run() {
                    updateSats();
                    updateSatsHandler.postDelayed(this, UPDATE_SATS_NUMBER_INTERVAL);
                }
            };
            updateSatsHandler.postDelayed(updateSatsRunnable, UPDATE_SATS_NUMBER_INTERVAL);
        }

        MS.startLocationMonitoring(new MainService.LocationReceiver() {
            @Override
            public void receive(Location location) {
                updateGeolocationInfoFused(location);
            }
        });

        BasicInfoFragment basicInfoFragment = (BasicInfoFragment) getSupportFragmentManager().findFragmentByTag(BasicInfoFragment.TAG);
        if (basicInfoFragment == null) {
            basicInfoFragment = new BasicInfoFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.gnss_frameLayout_basicInfo, basicInfoFragment, BasicInfoFragment.TAG);
            fragmentTransaction.commit();
        }
        basicInfoFragment.initMeasurement(gnssStatusUnit);
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void updateSats() {
        updateSatsNumber();
        updateSatsGalileo();
        updateSatsGPS();
        updateSatsGlonass();
        updateSatsBeidou();
        updateMeanSnr();
        nmeaParser.resetSNRSattelites();
        ggaData = nmeaParser.new GGAData();
    }

    private void updateSatsNumber() {
        int totSat = 0;
        Map<String, Map<String, NMEAParser.SNRSatellites.Satellite>> list = nmeaParser.getSNRSatellites().getListNets();
        for (Map.Entry<String, Map<String, NMEAParser.SNRSatellites.Satellite>> entry : list.entrySet()) {
            totSat += entry.getValue().size();
        }
        TextView satInViewTextView = findViewById(R.id.gnss_textView_satInView);
        satInViewTextView.setText(String.valueOf(totSat));

        if (ggaData != null) {
            TextView satInUseTextView = findViewById(R.id.gnss_textView_satInUse);
            String satInUse = getString(R.string.gnss_nd);
            if (ggaData.getSatelliteNumber() != null) {
                satInUse = String.valueOf(ggaData.getSatelliteNumber());
            }
            satInUseTextView.setText(satInUse);
            String fixType = ggaData.getFixType();
            TextView egnosTextView = findViewById(R.id.gnss_textView_egnos);
            if (fixType != null) {
                if (fixType.equals("1")) {
                    egnosTextView.setText(getString(R.string.gnss_no));
                } else if (fixType.equals("2")) {
                    egnosTextView.setText(getString(R.string.gnss_yes));
                } else {
                    egnosTextView.setText(getString(R.string.gnss_nd));
                }
            } else {
                egnosTextView.setText(getString(R.string.gnss_nd));
            }
        }
    }

    private void updateSatsGalileo() {
        ListView listView = findViewById(R.id.gnss_listView_galileo);
        listView.setAdapter(new SatsListAdapter(this, getSatsValueList(NMEAParser.SATELLITE_TYPE.GALILEO)));
    }

    private void updateSatsGPS() {
        ListView listView = findViewById(R.id.gnss_listView_gps);
        listView.setAdapter(new SatsListAdapter(this, getSatsValueList(NMEAParser.SATELLITE_TYPE.GPS)));
    }

    private void updateSatsGlonass() {
        ListView listView = findViewById(R.id.gnss_listView_glonass);
        listView.setAdapter(new SatsListAdapter(this, getSatsValueList(NMEAParser.SATELLITE_TYPE.GLONASS)));
    }

    private void updateSatsBeidou() {
        ListView listView = findViewById(R.id.gnss_listView_beidou);
        listView.setAdapter(new SatsListAdapter(this, getSatsValueList(NMEAParser.SATELLITE_TYPE.BEIDOU)));
    }

    private void updateMeanSnr() {
        TextView textView = findViewById(R.id.gnss_textView_meanSnr);
        // přímý výpočet
        Integer meanSnr = nmeaParser.getSNRSatellites().getMeanSnr();
        if (meanSnr != null) {
            textView.setText(String.valueOf(meanSnr));
        } else {
            textView.setText(getString(R.string.gnss_nd));
        }
    }

    private List<String> getSatsValueList(NMEAParser.SATELLITE_TYPE SATELLITETYPE) {
        List<NMEAParser.SNRSatellites.Satellite> nets = new ArrayList(nmeaParser.getSNRSatellites().getListNets().get(SATELLITETYPE.ID).values());
        List<String> valueList = new ArrayList<>();
        for (NMEAParser.SNRSatellites.Satellite netwM : nets) {
            valueList.add(netwM.getPrn() + " - snr: " + netwM.getSnr() + "dB");
        }
        return valueList;
    }

    private void updateRMCInfo(NMEAParser.RMCData rmcData) {
        TextView magVarTextView = findViewById(R.id.gnss_textView_magneticVariation);
        magVarTextView.setText(rmcData.getMagnVar());
        TextView ewMagnTextView = findViewById(R.id.gnss_textView_EWRMC);
        ewMagnTextView.setText(rmcData.getEwMagn());
        TextView trueNorthAngleTextView = findViewById(R.id.gnss_textView_trueNorthAngle);
        trueNorthAngleTextView.setText(rmcData.getTrackMG());
        Double speedKnots = null;
        if (rmcData.getSpeedKnots() != null && !rmcData.getSpeedKnots().isEmpty()) {
            speedKnots = Double.valueOf(rmcData.getSpeedKnots());
        }
        Double speedKmh = null;
        if (speedKnots != null) {
            speedKmh = speedKnots * 1.852;
        }
        String speedKnotsS = getString(R.string.gnss_nd);

        if (speedKnots != null) {
            speedKnotsS = decimalFormat.format(speedKnots);
        }
        String speedKmhS = getString(R.string.gnss_nd);
        if (speedKmh != null) {
            speedKmhS = decimalFormat.format(speedKmh);
        }
        TextView speedKnotsTextView = findViewById(R.id.gnss_textView_speedKnotsRMC);
        speedKnotsTextView.setText(speedKnotsS);
        TextView speedKmhTextView = findViewById(R.id.gnss_textView_speedKmhRMC);
        speedKmhTextView.setText(speedKmhS);
    }

    private void updateVTGInfo(NMEAParser.VTGData vtgData) {
        TextView trueDegreesTextView = findViewById(R.id.gnss_textView_trueDegrees);
        if (vtgData.getRealDeg() != null && !vtgData.getRealDeg().isEmpty()) {
            trueDegreesTextView.setText(vtgData.getRealDeg());
        } else {
            trueDegreesTextView.setText(getString(R.string.gnss_nd));
        }
        TextView magDegTextView = findViewById(R.id.gnss_textView_magneticDegrees);
        if (vtgData.getMagDeg() != null && !vtgData.getMagDeg().isEmpty()) {
            magDegTextView.setText(vtgData.getMagDeg());
        } else {
            magDegTextView.setText(getString(R.string.gnss_nd));
        }
        TextView speedKnotsTextView = findViewById(R.id.gnss_textView_speedKnotsVTG);
        if (vtgData.getSpeedKnots() != null && !vtgData.getSpeedKnots().isEmpty()) {
            speedKnotsTextView.setText(vtgData.getSpeedKnots());
        } else {
            speedKnotsTextView.setText(getString(R.string.gnss_nd));
        }
        TextView speedKmhTextView = findViewById(R.id.gnss_textView_speedKmhVTG);
        if (vtgData.getSpeedKmh() != null && !vtgData.getSpeedKmh().isEmpty()) {
            speedKmhTextView.setText(vtgData.getSpeedKmh());
        } else {
            speedKmhTextView.setText(R.string.gnss_nd);
        }
    }

    private void updateGeolocationInfoGGA() {
        TextView latitudeTextView = findViewById(R.id.gnss_textView_latitude);
        if (ggaData.getLatitude() != null) {
            latitudeTextView.setText(decimalFormat8.format(ggaData.getLatitude()));
        } else {
            latitudeTextView.setText(getString(R.string.gnss_nd));
        }
        TextView longitudeTextView = findViewById(R.id.gnss_textView_longitude);
        if (ggaData.getLongitude() != null) {
            longitudeTextView.setText(decimalFormat8.format(ggaData.getLongitude()));
        } else {
            longitudeTextView.setText(getString(R.string.gnss_nd));
        }
        TextView altitudeTextView = findViewById(R.id.gnss_textView_altitude);
        if (ggaData.getAltitude() != null) {
            altitudeTextView.setText(decimalFormat.format(ggaData.getAltitude()) + " m");
        } else {
            altitudeTextView.setText(getString(R.string.gnss_nd));
        }
        TextView nsTextView = findViewById(R.id.gnss_textView_NS);
        if (ggaData.getNS() != null) {
            nsTextView.setText(ggaData.getNS());
        } else {
            nsTextView.setText(getString(R.string.gnss_nd));
        }
        TextView ewTextView = findViewById(R.id.gnss_textView_EW_geoInfo);
        if (ggaData.getEW() != null) {
            ewTextView.setText(ggaData.getEW());
        } else {
            ewTextView.setText(getString(R.string.gnss_nd));
        }
    }

    private void updateGeolocationInfoGSA(NMEAParser.GSAData gsaData) {
        TextView hdopTextView = findViewById(R.id.gnss_textView_HDOP);
        if (gsaData.getHdop() != null) {
            hdopTextView.setText(decimalFormat.format(gsaData.getHdop()));
        } else {
            hdopTextView.setText(getString(R.string.gnss_nd));
        }
        TextView pdopTextView = findViewById(R.id.gnss_textView_PDOP);
        if (gsaData.getPdop() != null) {
            pdopTextView.setText(decimalFormat.format(gsaData.getPdop()));
        } else {
            pdopTextView.setText(getString(R.string.gnss_nd));
        }
        TextView vdopTextView = findViewById(R.id.gnss_textView_VDOP);
        if (gsaData.getVdop() != null) {
            vdopTextView.setText(decimalFormat.format(gsaData.getVdop()));
        } else {
            vdopTextView.setText(getString(R.string.gnss_nd));
        }
        TextView fixQualityTextView = findViewById(R.id.gnss_textView_fixQuality);
        if (gsaData.getFixMode() != null) {
            fixQualityTextView.setText(gsaData.getFixMode());
        } else {
            fixQualityTextView.setText(getString(R.string.gnss_nd));
        }
    }

    private void updateGeolocationInfoFused(Location location) {
        TextView accuracyTextView = findViewById(R.id.gnss_textView_accuracy);
        accuracyTextView.setText(decimalFormat.format(location.getAccuracy()) + " m");
    }

    private void updatePrecision(double precision) {
        TextView precisionTextView = findViewById(R.id.gnss_textView_precision);
        precisionTextView.setText(decimalFormat.format(precision) + " m");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gnssStatusUnit.stopScan();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nmeaScanner.stopScan();
            updateSatsHandler.removeCallbacks(updateSatsRunnable);
        }
        MS.stopLocationMonitoring(null, null, null);
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
