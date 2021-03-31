package eu.foxcom.gtphotos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import eu.foxcom.gtphotos.model.MyAlertDialog;
import eu.foxcom.gtphotos.model.PersistData;
import eu.foxcom.gtphotos.model.Util;
import eu.foxcom.gtphotos.model.component.SeekBarAPI26;

public class SettingsActivity extends BaseActivity {

    public static final int MIN_SAMPLING_NUMBER = 1;
    public static final int MAX_SAMPLING_NUMBER = 50;
    public static final int DEFAULT_SAMPLING_NUMBER = 20;

    SeekBarAPI26 samplingNumberSeekBar;
    TextView samplingNumberTextView;

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
        Switch photoCentroidSwitch = findViewById(R.id.se_switch_photoWithCentroid);
        photoCentroidSwitch.setChecked(PersistData.getPhotoWithCentroiLocation(this));
        photoCentroidSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.savePhotoWithCentroiLocation(this, isChecked);
        });
        Switch filterCentroidSwitch = findViewById(R.id.se_switch_filterCentroidActive);
        filterCentroidSwitch.setChecked(PersistData.getCentroidFilterActive(this));
        filterCentroidSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.saveCentroidFilterActive(this, isChecked);
        });

        samplingNumberSeekBar = findViewById(R.id.se_seekBarAPI26_samplingNumber);
        samplingNumberTextView = findViewById(R.id.se_textView_samplingNumber);
        samplingNumberSeekBar.setMinMax(MIN_SAMPLING_NUMBER, MAX_SAMPLING_NUMBER);
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
        Switch buttonSnapshotSwitch = findViewById(R.id.se_switch_buttonSnapshot);
        buttonSnapshotSwitch.setChecked(PersistData.getButtonSnapshotActive(this));
        showButtonSnapshotKeyCode(PersistData.getButtonSnapshotKeyCode(this));
        initBrightnessButton();
        initAutoPanButton();
        initBeepButton();
    }

    private void initBrightnessButton() {
        Switch buttonManualBrightness = findViewById(R.id.se_switch_manualBrightness);
        buttonManualBrightness.setChecked(isManualBrightnessActive(this));
        buttonManualBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.saveManualBrightnessCorrectionActive(this, isChecked ? PersistData.MANUAL_BRIGHTNESS_ACTIVE.TRUE : PersistData.MANUAL_BRIGHTNESS_ACTIVE.FALSE);
        });
    }

    private void initAutoPanButton() {
        Switch autoPanSwitch = findViewById(R.id.se_switch_autoPan);
        autoPanSwitch.setChecked(PersistData.getAutoPan(this));
        autoPanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.saveAutoPan(this, isChecked);
        });
    }

    private void initBeepButton() {
        Switch beepSwitch = findViewById(R.id.se_switch_beep);
        beepSwitch.setChecked(PersistData.getBeepPathPoint(this));
        beepSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PersistData.saveBeepPathPoint(this, isChecked);
        });
    }

    public void centroidSamplingNumberDefault(View view) {
        samplingNumberSeekBar.setProgress(DEFAULT_SAMPLING_NUMBER);
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
