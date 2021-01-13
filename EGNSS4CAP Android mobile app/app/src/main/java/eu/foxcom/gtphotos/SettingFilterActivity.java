package eu.foxcom.gtphotos;

import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import eu.foxcom.gtphotos.model.PersistData;
import eu.foxcom.gtphotos.model.component.SeekBarAPI26;
import eu.foxcom.gtphotos.model.functionInterface.BiConsumer;
import eu.foxcom.gtphotos.model.functionInterface.Function;

public class SettingFilterActivity extends BaseActivity {
    public static final String TAG = SettingFilterActivity.class.getSimpleName();

    public static final int MIN_MIN_HDOP = 0;
    public static final int MAX_MIN_HDOP = 10;
    public static final int DEFAULT_MIN_HDOP = 1;
    public static final int MIN_MIN_SAT_NUMBER = 1;
    public static final int MAX_MIN_SAT_NUMBER = 40;
    public static final int DEFAULT_MIN_SAT_NUMBER = 7;
    public static final int MIN_MIN_MEAN_SNR = 2;
    public static final int MAX_MIN_MEAN_SNR = 100;
    public static final int DEFAULT_MIN_MEAN_SNR = 31;
    public static final int MIN_MIN_FIX = 0;
    public static final int MAX_MIN_FIX = 3;
    public static final int DEFAULT_MIN_FIX = 3;

    Switch filterActiveSwitch;
    SeekBarAPI26 minHdopSeekBar;
    TextView minHdopTextView;
    SeekBarAPI26 minSatNumberSeekBar;
    TextView miSatNumberTextView;
    SeekBarAPI26 minMeanSnrSeekBar;
    TextView minMeanSnrTextView;
    SeekBarAPI26 minFixSeekBar;
    TextView minFixTextView;
    Button setToDefaulButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_filter);

        filterActiveSwitch = findViewById(R.id.sef_switch_filterActive);
        minHdopSeekBar = findViewById(R.id.sef_seekBar_hdopMin);
        minHdopTextView = findViewById(R.id.sef_textView_hdopMin);
        minSatNumberSeekBar = findViewById(R.id.sef_seekBar_minSatNumber);
        miSatNumberTextView = findViewById(R.id.sef_textView_minSatNumber);
        minMeanSnrSeekBar = findViewById(R.id.sef_seekBar_minMeanSnr);
        minMeanSnrTextView = findViewById(R.id.sef_textView_minMeanSnr);
        minFixSeekBar = findViewById(R.id.sef_seekBar_minFix);
        minFixTextView = findViewById(R.id.sef_textView_minFix);
        setToDefaulButton = findViewById(R.id.sef_button_toDefault);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        boolean active = PersistData.getCentroidFilterActive(this);
        filterActiveSwitch.setChecked(active);
        toggleActive(active);
        filterActiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleActive(isChecked);
        });

        initFilter(minHdopSeekBar, minHdopTextView,
                PersistData::getMinHDOP,
                PersistData::saveMinHDOP,
                MIN_MIN_HDOP, MAX_MIN_HDOP);
        initFilter(minSatNumberSeekBar, miSatNumberTextView,
                (context -> (double) PersistData.getMinNumberSats(context)),
                ((context, aDouble) -> PersistData.saveMinNumberSats(context, aDouble.intValue())),
                MIN_MIN_SAT_NUMBER, MAX_MIN_SAT_NUMBER);
        initFilter(minMeanSnrSeekBar, minMeanSnrTextView,
                PersistData::getMinSNR,
                PersistData::saveMinSNR,
                MIN_MIN_MEAN_SNR, MAX_MIN_MEAN_SNR);
        initFilter(minFixSeekBar, minFixTextView,
                context -> (double) PersistData.getMinFix(context),
                (context, aDouble) -> PersistData.saveMinFix(context, aDouble.intValue()),
                MIN_MIN_FIX, MAX_MIN_FIX);

        setToDefaulButton.setOnClickListener(v -> {
            setDefaultValues();
        });
    }

    private void initFilter(SeekBarAPI26 seekBar, TextView textView, Function<Context, Double> getter, BiConsumer<Context, Double> saver, int min, int max) {
        seekBar.setMinMax(min, max);
        seekBar.setProgress(Double.valueOf(PersistData.getMinHDOP(this)).intValue());
        seekBar.setProgress(Double.valueOf(getter.apply(this)).intValue());
        textView.setText(String.valueOf(seekBar.getProgress()));
        seekBar.setOnSeekBarChangeListenerAPI26(new SeekBarAPI26.OnSeekBarChangeListenerAPI26() {
            @Override
            public void onProgressChanged(SeekBarAPI26 seekBar, int progress, boolean fromUser) {
                textView.setText(String.valueOf(progress));
                saver.accept(SettingFilterActivity.this, Double.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBarAPI26 seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBarAPI26 seekBar) {
            }
        });
    }

    private void toggleActive(boolean active) {
        PersistData.saveCentroidFilterActive(this, active);
        minHdopSeekBar.setEnabled(active);
        minSatNumberSeekBar.setEnabled(active);
        minMeanSnrSeekBar.setEnabled(active);
        minFixSeekBar.setEnabled(active);
        setToDefaulButton.setEnabled(active);
    }

    private void setDefaultValues() {
        minHdopSeekBar.setProgress(DEFAULT_MIN_HDOP);
        minSatNumberSeekBar.setProgress(DEFAULT_MIN_SAT_NUMBER);
        minMeanSnrSeekBar.setProgress(DEFAULT_MIN_MEAN_SNR);
        minFixSeekBar.setProgress(DEFAULT_MIN_FIX);
    }
}