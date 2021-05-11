package eu.foxcom.stp.gsa.egnss4cap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import eu.foxcom.stp.gsa.egnss4cap.model.MyAlertDialog;
import eu.foxcom.stp.gsa.egnss4cap.model.PersistData;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;
import eu.foxcom.stp.gsa.egnss4cap.model.component.SeekBarAPI26;

public class SettingsActivity extends BaseActivity {

    public static final int SAMPLING_NUMBER_MIN = 1;
    public static final int SAMPLING_NUMBER_MAX = 50;
    public static final int SAMPLING_NUMBER_DEFAULT = 20;

    public static final int CAPTURE_POINT_PERIOD_MIN = 1;
    public static final int CAPTURE_POINT_PERIOD_MAX = 60;
    public static final int CAPTURE_POINT_PERIOD_DEFAULT = 1;


    SeekBarAPI26 samplingNumberSeekBar;
    TextView samplingNumberTextView;

    SeekBarAPI26 capturePeriodSeekBar;
    TextView capturePeriodTextView;

    public static boolean isManualBrightnessActive(Context context) {
        PersistData.MANUAL_BRIGHTNESS_ACTIVE brightnessActive = PersistData.getManualBrightnessCorrectionActive(context);
        boolean brightnessActiveBoolean;
        if (brightnessActive.equals(PersistData.MANUAL_BRIGHTNESS_ACTIVE.DEFAULT)) {
            // default turn off brightness correction for unsupported phones
            if (Util.getPhoneModel().toLowerCase().trim().equals("Mi note 10 Pro".toLowerCase().trim())) {
                brightnessActiveBoolean = false;
            } else {
                brightnessActiveBoolean = true;
            }
        } else {
            brightnessActiveBoolean = brightnessActive.equals(PersistData.MANUAL_BRIGHTNESS_ACTIVE.TRUE);
        }
        return brightnessActiveBoolean;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setToolbar(R.id.toolbar);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();
    }

    public void showCentroidPositionFiltersSettings(View view) {
        Intent intent = new Intent(this, SettingFilterActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        initPhotoCentroidItem();
        initFilterCentroidItem();
        initSamplingNumberItem();
        initButtonSnapshotItem();
        initBrightnessItem();
        initAutoPanItem();
        initBeepItem();
        initCapturePeriodItem();
    }

    private void initButtonSnapshotItem() {
        Switch buttonSnapshotSwitch = findViewById(R.id.se_switch_buttonSnapshot);
        buttonSnapshotSwitch.setChecked(PersistData.getButtonSnapshotActive(this));
        showButtonSnapshotKeyCode(PersistData.getButtonSnapshotKeyCode(this));
    }

    private void initSamplingNumberItem() {
        samplingNumberSeekBar = findViewById(R.id.se_seekBarAPI26_samplingNumber);
        samplingNumberTextView = findViewById(R.id.se_textView_samplingNumber);
        samplingNumberSeekBar.setMinMax(SAMPLING_NUMBER_MIN, SAMPLING_NUMBER_MAX);
        samplingNumberSeekBar.setProgress(Double.valueOf(PersistData.getSamplingNumber(this)).intValue());
        samplingNumberTextView.setText(String.valueOf(samplingNumberSeekBar.getProgress()));
        samplingNumberSeekBar.setOnSeekBarChangeListenerAPI26(new SeekBarAPI26.OnSeekBarChangeListenerAPI26() {
            @Override
            public void onProgressChanged(SeekBarAPI26 seekBar, int progress, boolean fromUser) {
                samplingNumberTextView.setText(String.valueOf(progress));
                PersistData.saveSamplingNumber(SettingsActivity.this, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBarAPI26 seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBarAPI26 seekBar) {
            }
        });
    }

    private void initFilterCentroidItem() {
        Switch filterCentroidSwitch = findViewById(R.id.se_switch_filterCentroidActive);
        filterCentroidSwitch.setChecked(PersistData.getCentroidFilterActive(this));
        filterCentroidSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.saveCentroidFilterActive(this, isChecked);
        });
    }

    private void initPhotoCentroidItem() {
        Switch photoCentroidSwitch = findViewById(R.id.se_switch_photoWithCentroid);
        photoCentroidSwitch.setChecked(PersistData.getPhotoWithCentroiLocation(this));
        photoCentroidSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.savePhotoWithCentroiLocation(this, isChecked);
        });
    }

    private void initBrightnessItem() {
        Switch buttonManualBrightness = findViewById(R.id.se_switch_manualBrightness);
        buttonManualBrightness.setChecked(isManualBrightnessActive(this));
        buttonManualBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.saveManualBrightnessCorrectionActive(this, isChecked ? PersistData.MANUAL_BRIGHTNESS_ACTIVE.TRUE : PersistData.MANUAL_BRIGHTNESS_ACTIVE.FALSE);
        });
    }

    private void initAutoPanItem() {
        Switch autoPanSwitch = findViewById(R.id.se_switch_autoPan);
        autoPanSwitch.setChecked(PersistData.getAutoPan(this));
        autoPanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.saveAutoPan(this, isChecked);
        });
    }

    private void initBeepItem() {
        Switch beepSwitch = findViewById(R.id.se_switch_beep);
        beepSwitch.setChecked(PersistData.getBeepPathPoint(this));
        beepSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.saveBeepPathPoint(this, isChecked);
        });
    }

    private void initCapturePeriodItem() {
        capturePeriodSeekBar = findViewById(R.id.se_seekBarAPI26_pointCapturePeriod);
        capturePeriodTextView = findViewById(R.id.se_textView_pointCapturePeriod);
        capturePeriodSeekBar.setMinMax(CAPTURE_POINT_PERIOD_MIN, CAPTURE_POINT_PERIOD_MAX);
        capturePeriodSeekBar.setProgress(PersistData.getCapturePointPeriod(this));
        capturePeriodTextView.setText(String.valueOf(capturePeriodSeekBar.getProgress()));
        capturePeriodSeekBar.setOnSeekBarChangeListenerAPI26(new SeekBarAPI26.OnSeekBarChangeListenerAPI26() {
            @Override
            public void onProgressChanged(SeekBarAPI26 seekBar, int progress, boolean fromUser) {
                capturePeriodTextView.setText(String.valueOf(progress));
                PersistData.saveCapturePointPeriod(SettingsActivity.this, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBarAPI26 seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBarAPI26 seekBar) {

            }
        });
    }

    public void centroidSamplingNumberDefault(View view) {
        samplingNumberSeekBar.setProgress(SAMPLING_NUMBER_DEFAULT);
    }

    public void capturePointPeriodDefault(View view) {
        capturePeriodSeekBar.setProgress(CAPTURE_POINT_PERIOD_DEFAULT);
    }

    public void buttonSnapshotToggle(View view) {
        boolean active = PersistData.getButtonSnapshotActive(this);
        if (active) {
            saveButtonSnapshotActive(false);
        } else {
            MyAlertDialog.Builder builder = new MyAlertDialog.Builder(this);
            MyAlertDialog myAlertDialog = builder.build();
            myAlertDialog.setTitle(getString(R.string.se_buttonToSnapTitle));
            myAlertDialog.setMessage(getString(R.string.se_buttonToSnapDesc));
            myAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.dl_Cancel), null);
            myAlertDialog.getAlertDialog().setOnKeyListener((dialog, keyCode, event) -> {
                if (keyCode == KeyEvent.ACTION_DOWN
                        || keyCode == KeyEvent.ACTION_UP
                        || keyCode == KeyEvent.ACTION_MULTIPLE
                        || keyCode == KeyEvent.FLAG_KEEP_TOUCH_MODE) {
                    return false;
                }
                PersistData.saveButtonSnapshotKeyCode(this, keyCode);
                showButtonSnapshotKeyCode(keyCode);
                saveButtonSnapshotActive(true);
                dialog.dismiss();
                return true;
            });
            myAlertDialog.show();
        }
    }

    private void saveButtonSnapshotActive(boolean active) {
        PersistData.saveButtonSnapshotActive(this, active);
        Switch buttonSnapshotSwitch = findViewById(R.id.se_switch_buttonSnapshot);
        buttonSnapshotSwitch.setChecked(active);
    }

    private void showButtonSnapshotKeyCode(int keyCode) {
        TextView keyCodeTextView = findViewById(R.id.se_textView_buttonSnapKeyCode);
        String name = Util.getButtonName(keyCode);
        if (name.equals("UNRECOGNIZE")) {
            name = getString(R.string.se_buttonToSnapUnassigned);
        }
        keyCodeTextView.setText(name);
    }


    // region UI bridges to switches
    public void photoWithCentroidToggle(View view) {
        Switch aSwitch = findViewById(R.id.se_switch_photoWithCentroid);
        aSwitch.performClick();
    }

    public void manualBrightnessToggle(View view) {
        Switch aSwitch = findViewById(R.id.se_switch_manualBrightness);
        aSwitch.performClick();
    }

    public void autoPanToggle(View view) {
        Switch aSwitch = findViewById(R.id.se_switch_autoPan);
        aSwitch.performClick();
    }

    public void beepToggle(View view) {
        Switch aSwitch = findViewById(R.id.se_switch_beep);
        aSwitch.performClick();
    }

    // endregion
}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
