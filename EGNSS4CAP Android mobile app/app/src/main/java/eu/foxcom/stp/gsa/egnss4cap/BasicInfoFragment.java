package eu.foxcom.stp.gsa.egnss4cap;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.foxcom.gnss_scan.GnssStatusScanner;
import eu.foxcom.stp.gsa.egnss4cap.model.PersistData;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;


public class BasicInfoFragment extends BaseFragment {

    public static final String TAG = BasicInfoFragment.class.getName();

    public static final int DEVICE_CONDITION_INERVAL_MILS = 2000;

    private BaseActivity baseActivity;
    private GnssStatusScanner gnssStatusScanner;
    private Object startMeasurementLock = new Object();
    private boolean isMeasurementStarted = false;
    private Handler deviceConditionHandler;
    private Runnable deviceConditionRunnable;
    private AtomicBoolean isDeviceConditionInitiated = new AtomicBoolean(false);
    private AtomicBoolean isDeviceConditionStarted = new AtomicBoolean(false);

    public BasicInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseActivity = (BaseActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_basic_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startMeasurement();
    }

    public void initMeasurement(GnssStatusScanner gnssStatusUnit) {
        this.gnssStatusScanner = gnssStatusUnit;
        startMeasurement();
    }

    @Override
    public void onResume() {
        super.onResume();
        startDeviceConditionChecker();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopDeviceConditionChecker();
    }

    private void startMeasurement() {
        synchronized (startMeasurementLock) {
            if (!isViewPrepared || this.gnssStatusScanner == null || isMeasurementStarted) {
                return;
            }
            isMeasurementStarted = true;
            initDeviceConditionChecker();
            startDeviceConditionChecker();
            initHardwareInfo();
            initGnssStatus();
        }
    }

    private void initHardwareInfo() {
        TextView manufacturerTextView = getView().findViewById(R.id.bi_textView_phoneManufacturer);
        manufacturerTextView.setText(getPhoneManufacturer());
        TextView modelTextView = getView().findViewById(R.id.bi_textView_phoneModel);
        modelTextView.setText(getPhoneModel());
        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS) && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
            ImageView compassGyroscopeImageView = getView().findViewById(R.id.bi_imageView_compassGyroscopeCheck);
            compassGyroscopeImageView.setImageResource(R.drawable.semaphor_green);
        }
    }

    private void initDeviceConditionChecker() {
        synchronized (isDeviceConditionInitiated) {
            if (isDeviceConditionInitiated.get()){
                return;
            }
            deviceConditionHandler = new Handler();
            deviceConditionRunnable = new Runnable() {
                @Override
                public void run() {
                    boolean enabled = Util.isLocationServiceEnabled(getContext());
                    ImageView deviceConditionImageView = getView().findViewById(R.id.bi_imageView_deviceCondition);
                    deviceConditionImageView.setImageResource(enabled ? R.drawable.semaphor_green : R.drawable.semaphor_red);

                    deviceConditionHandler.postDelayed(this, DEVICE_CONDITION_INERVAL_MILS);
                }
            };
            isDeviceConditionInitiated.set(true);
        }
    }

    private void startDeviceConditionChecker() {
        synchronized (isDeviceConditionStarted) {
            if (isDeviceConditionInitiated.get() && !isDeviceConditionStarted.get()) {
                deviceConditionHandler.post(deviceConditionRunnable);
                isDeviceConditionStarted.set(true);
            }
        }
    }

    private void stopDeviceConditionChecker() {
        synchronized (isDeviceConditionStarted) {
            if (isDeviceConditionInitiated.get() && isDeviceConditionStarted.get()) {
                deviceConditionHandler.removeCallbacks(deviceConditionRunnable);
                isDeviceConditionStarted.set(false);
            }
        }
    }

    private void initGnssStatus() {
        gnssStatusScanner.registerReceiver(new GnssStatusScanner.GnssStatusReceiver() {
            @Override
            public void receive(GnssStatusScanner.GnssStatusHolder gnssStatusHolder) {
                if (!isActive) {
                    return;
                }
                if (gnssStatusHolder.getMessageType().equals(GnssStatusScanner.MESSAGE_TYPE.GNSS_STATUS)) {
                    Set<GnssStatusScanner.CONSTELLATION> set = gnssStatusHolder.getConstellations();
                    if (set.contains(GnssStatusScanner.CONSTELLATION.GALILEO)) {
                        setGalileoSignalCheck(true);
                    }
                    if (set.contains(GnssStatusScanner.CONSTELLATION.SBAS)) {
                        setEgnosSignalCheck(true);
                    }
                } else if (gnssStatusHolder.getMessageType().equals(GnssStatusScanner.MESSAGE_TYPE.DUAL_FREQ)) {
                    setDualFrequencySignalCheck(true);
                } else if (gnssStatusHolder.getMessageType().equals(GnssStatusScanner.MESSAGE_TYPE.OSNMA)) {
                    setGalileoNavigationMessageSignalCheck(true);
                }
            }
        });
        setGalileoSignalCheck(PersistData.getGalileoSignalCheck(baseActivity));
        setDualFrequencySignalCheck(PersistData.getDualFrequencySignalCheck(baseActivity));
        setEgnosSignalCheck(PersistData.getEgnosSignalCheck(baseActivity));
        setGalileoNavigationMessageSignalCheck(PersistData.getGalileoNavigationMessageSignalCheck(baseActivity));
    }

    private void setGalileoSignalCheck(boolean checked) {
        if (checked) {
            ImageView imageView = getView().findViewById(R.id.bi_imageView_galileoSignalCheck);
            imageView.setImageResource(R.drawable.semaphor_green);
            PersistData.saveGalileoSignalCheck(baseActivity, checked);
        }
    }

    private void setDualFrequencySignalCheck(boolean checked) {
        if (checked) {
            ImageView imageView = getView().findViewById(R.id.bi_imageView_dualFrequencySignalCheck);
            imageView.setImageResource(R.drawable.semaphor_green);
            PersistData.saveDualFrequencySignalCheck(baseActivity, checked);
        }

    }

    private void setEgnosSignalCheck(boolean checked) {
        if (checked) {
            ImageView imageView = getView().findViewById(R.id.bi_imageView_egnosSignalCheck);
            imageView.setImageResource(R.drawable.semaphor_green);
            PersistData.saveEgnosSignalCheck(baseActivity, checked);
        }
    }

    private void setGalileoNavigationMessageSignalCheck(boolean checked) {
        if (checked) {
            ImageView imageView = getView().findViewById(R.id.bi_imageView_galileoNavigationMessageSignalCheck);
            imageView.setImageResource(R.drawable.semaphor_green);
            PersistData.saveGalileoNavigationMessageSignalCheck(baseActivity, checked);
        }
    }


    private String getPhoneManufacturer() {
        return Util.getPhoneManufacturer();
    }

    private String getPhoneModel() {
        return Util.getPhoneModel();
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
