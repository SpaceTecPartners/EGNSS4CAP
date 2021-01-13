package eu.foxcom.gtphotos.model.mock;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.foxcom.gtphotos.BaseActivity;
import eu.foxcom.gtphotos.MainService;
import eu.foxcom.gtphotos.R;
import eu.foxcom.gtphotos.model.ExifUtil;
import eu.foxcom.gtphotos.model.PhotoDataController;

public class MockSensorActivity extends BaseActivity {

    private PhotoDataController photoDataController;
    private Handler compassUpdaterHandler;
    private Runnable compassUpdaterRunnable;

    String satsInfos = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();
        setContentView(R.layout.activity_mock_sensor);
        startSensor();
    }

    private void startSensor() {
        photoDataController = new PhotoDataController(this);
        MS.startLocationMonitoring(new MainService.LocationReceiver() {
            @Override
            public void receive(Location location) {
                photoDataController.addLocation(location);
            }
        });
        photoDataController.getPositionSensorController().setReceiver(photoDataController.getPositionSensorController().new Receiver() {
            @Override
            public void receive() {
                refreshData(getWindow().getDecorView());
            }
        });
        photoDataController.start();

        compassUpdaterHandler = new Handler();
        compassUpdaterRunnable = new Runnable() {
            @Override
            public void run() {
                photoDataController.getPositionSensorController().updateOrientationAnglesFusedAverage();
                double avgAzimuthFused = photoDataController.getPositionSensorController().getAzimuthDegreesFusedAverage();
                TextView pokusLabel4 = findViewById(R.id._label4);
                pokusLabel4.setText("AVG FUSED AZIM: " + Math.round(avgAzimuthFused));

                photoDataController.getPositionSensorController().updateOrientationAnglesAverage();
                double avgAzimuth = photoDataController.getPositionSensorController().getAzimuthDegreesAverage();
                TextView label5 = findViewById(R.id._label5);
                label5.setText("AVG RAW AZIM: " + Math.round(avgAzimuth));
                double avgPitch = photoDataController.getPositionSensorController().getPitchDegreesAverage();
                TextView label8 = findViewById(R.id._label8);
                label8.setText("PITCH: " + Math.round(avgPitch));
                double avgRoll = photoDataController.getPositionSensorController().getRollDegreesAverage();
                TextView label9 = findViewById(R.id._label9);
                label9.setText("ROLL: " + Math.round(avgRoll));
                int exifOri = ExifUtil.toExifOrientation(avgPitch, avgRoll);
                TextView label10 = findViewById(R.id._label10);
                label10.setText("EXIF ORI: " + exifOri);

                JSONObject networkInfo = photoDataController.getNetworkInfoData();
                String networkInfoS = "NONE";
                if (networkInfo != null) {
                    networkInfoS = networkInfo.toString();
                }
                TextView label11 = findViewById(R.id._label11);
                label11.setText("NETWORK INFO: " + networkInfoS);
                JSONArray sats_info = new JSONArray();
                try {
                    sats_info = photoDataController.getNmeaParser().getSNRSatellites().toCurrSatsInfo();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String satsInfoS = sats_info.toString();
                TextView label12 = findViewById(R.id._label12);
                satsInfos += satsInfoS;
                label12.setText("SATS INFO: " + satsInfoS);
                String nmeaPhoto = photoDataController.getNmeaParser().getNmeaTotalMessage();
                TextView label13 = findViewById(R.id._label13);
                label13.setText("NMEA PHOTO: " + nmeaPhoto);

                compassUpdaterHandler.postDelayed(this, 1000);
            }
        };
        compassUpdaterHandler.postDelayed(compassUpdaterRunnable, 0);

        float[] FOV = photoDataController.getCameraController().calculateFOV();
        TextView label7 = findViewById(R.id._label7);
        label7.setText("FOV = [" + Math.toDegrees(FOV[0]) + "; " + Math.toDegrees(FOV[1]) + "]");
    }

    public void refreshData(View view) {
        Location location = photoDataController.getLocation();
        photoDataController.getPositionSensorController().updateOrientationAngles();
        photoDataController.getPositionSensorController().updateOrientationAnglesFused();
        double azimuth = photoDataController.getPositionSensorController().getAzimuthDegrees();
        double azimuthFused = photoDataController.getPositionSensorController().getAzimuthDegreesFused();
        float[] orientationAngles = photoDataController.getPositionSensorController().getOrientationAngles();
        TextView pokusLabel = findViewById(R.id._label6);
        pokusLabel.setText("RAW sensor AZIM: " + String.valueOf(azimuth));
        TextView pokusLabel2 = findViewById(R.id._label2);
        pokusLabel2.setText("FUSED sensor AZIM: " + String.valueOf(azimuthFused));
    }

    private void stopSensor() {
        photoDataController.stop();
    }
}
