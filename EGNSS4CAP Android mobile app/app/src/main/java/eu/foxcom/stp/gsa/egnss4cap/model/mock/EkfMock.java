package eu.foxcom.stp.gsa.egnss4cap.model.mock;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.joda.time.DateTime;

import eu.foxcom.stp.gsa.egnss4cap.R;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EKFStartExeception;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfCalculationModule;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfController;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfData;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfCreateException;


public class EkfMock extends AppCompatActivity {

    public static final String TAG = EkfMock.class.getSimpleName();

    EkfController ekfController;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ekf_mock);
    }

    @Override
    protected void onStart() {
        super.onStart();

        initEkf();
        initFused();
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            ekfController.start();
        } catch (EKFStartExeception ekfStartExeception) {
            Log.d(TAG, ekfStartExeception.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        ekfController.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ekfController.release();
    }

    private void initEkf() {
        ekfController = new EkfController(this);
        try {
            ekfController.addDefaultModules();
        } catch (EkfCreateException e) {
            Log.d(TAG, e.getMessage());
        }

        ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GALILEO_E1).setEkfReceiver(ekfData -> writeEkfGalileoE1(ekfData));
        ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GALILEO_E5A).setEkfReceiver(ekfData -> writeEkfGalileoE5(ekfData));
        ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GALILEO_IF).setEkfReceiver(ekfData -> writeEkfGalileoIf(ekfData));
        ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GPS_IF).setEkfReceiver(ekfData -> writeEkfGpsIf(ekfData));
        ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GPS_L1).setEkfReceiver(ekfData -> writeEkfGpsL1(ekfData));
        ekfController.getModule(EkfCalculationModule.DEFAULT_MODULE.GPS_L5).setEkfReceiver(ekfData -> writeEkfGpsL5(ekfData));
    }


    @SuppressLint("MissingPermission")
    private void initFused() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(500);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(100);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    writeFused(location);
                }
            }
        };
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void writeFused(Location location) {
        TextView latTextView = findViewById(R.id.ekfMock_fusedLat);
        latTextView.setText(Util.createPrettyCoordinateFormat().format(location.getLatitude()));
        TextView lngTextView = findViewById(R.id.ekfMock_textView_fusedLng);
        lngTextView.setText(Util.createPrettyCoordinateFormat().format(location.getLongitude()));
        TextView altTextView = findViewById(R.id.ekfMock_textView_fusedAlt);
        altTextView.setText(Util.createPrettyCoordinateFormat().format(location.getAltitude()));
        TextView refTextView = findViewById(R.id.ekfMock_textView_fusedRefT);
        refTextView.setText((new DateTime(location.getTime())).toString(Util.createPrettyTimeFormat()));
        TextView comTextView = findViewById(R.id.ekfMock_textView_fusedComT);
        comTextView.setText(DateTime.now().toString(Util.createPrettyTimeFormat()));
    }

    private void writeEkf(EkfData ekfData, int latId, int lngId, int altId, int refT, int comT) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView latTextView = findViewById(latId);
                latTextView.setText(ekfData.getLatitude() == null ? "null" : Util.createPrettyCoordinateFormat().format(ekfData.getLatitude()));
                TextView lngTextView = findViewById(lngId);
                lngTextView.setText(ekfData.getLongitude() == null ? "null" : Util.createPrettyCoordinateFormat().format(ekfData.getLongitude()));
                TextView altTextView = findViewById(altId);
                altTextView.setText(ekfData.getAltitude() == null ? "null" : Util.createPrettyCoordinateFormat().format(ekfData.getAltitude()));
                TextView refTextView = findViewById(refT);
                refTextView.setText(ekfData.getReferenceTime() == null ? "null" : ekfData.getReferenceTime().toString(Util.createPrettyTimeFormat()));
                TextView comTTextView = findViewById(comT);
                comTTextView.setText(ekfData.getComputedTime() == null ? "null" : ekfData.getComputedTime().toString(Util.createPrettyTimeFormat()));
            }
        });
    }

    private void writeEkfGalileoE1(EkfData ekfData) {
        writeEkf(ekfData,
                R.id.ekfMock_galE1Lat,
                R.id.ekfMock_galE1Lng,
                R.id.ekfMock_galE1Alt,
                R.id.ekfMock_galE1RefT,
                R.id.ekfMock_galE1ComT);
    }
    private void writeEkfGalileoE5(EkfData ekfData) {
        writeEkf(ekfData,
                R.id.ekfMock_galE5Lat,
                R.id.ekfMock_galE5Lng,
                R.id.ekfMock_galE5Alt,
                R.id.ekfMock_galE5RefT,
                R.id.ekfMock_galE5ComT);
    }
    private void writeEkfGalileoIf(EkfData ekfData) {
        writeEkf(ekfData,
                R.id.ekfMock_galifLat,
                R.id.ekfMock_galifLng,
                R.id.ekfMock_galifAlt,
                R.id.ekfMock_galifRefT,
                R.id.ekfMock_galifComT);
    }
    private void writeEkfGpsIf(EkfData ekfData) {
        writeEkf(ekfData,
                R.id.ekfMock_gpsIfLat,
                R.id.ekfMock_gpsIfLng,
                R.id.ekfMock_gpsIfAlt,
                R.id.ekfMock_gpsIfRefT,
                R.id.ekfMock_gpsIfComT);
    }
    private void writeEkfGpsL5(EkfData ekfData) {
        writeEkf(ekfData,
                R.id.ekfMock_gpsL5Lat,
                R.id.ekfMock_gpsL5Lng,
                R.id.ekfMock_gpsL5Alt,
                R.id.ekfMock_gpsL5RefT,
                R.id.ekfMock_gpsL5ComT);
    }
    private void writeEkfGpsL1(EkfData ekfData) {
        writeEkf(ekfData,
                R.id.ekfMock_gpsL1Lat,
                R.id.ekfMock_gpsL1Lng,
                R.id.ekfMock_gpsL1Alt,
                R.id.ekfMock_gpsL1RefT,
                R.id.ekfMock_gpsL1ComT);
    }

}