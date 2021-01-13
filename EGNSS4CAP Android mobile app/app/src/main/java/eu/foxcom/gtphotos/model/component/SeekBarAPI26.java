package eu.foxcom.gtphotos.model.component;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class SeekBarAPI26 extends androidx.appcompat.widget.AppCompatSeekBar {

    public interface OnSeekBarChangeListenerAPI26 {
        public void onProgressChanged(SeekBarAPI26 seekBar, int progress, boolean fromUser);

        public void onStartTrackingTouch(SeekBarAPI26 seekBar);

        public void onStopTrackingTouch(SeekBarAPI26 seekBar);
    }

    private int minimum;
    private int maximum;

    public SeekBarAPI26(Context context) {
        super(context);
    }

    public SeekBarAPI26(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBarAPI26(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMinMax(int min, int max) {
        minimum = min;
        maximum = max;
        int dif = max - min;
        super.setMax(dif);
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress - minimum);
    }

    @Override
    public synchronized int getProgress() {
        return super.getProgress() + minimum;
    }

    public void setOnSeekBarChangeListenerAPI26(OnSeekBarChangeListenerAPI26 l) {
        setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                l.onProgressChanged((SeekBarAPI26) seekBar, minimum + progress, fromUser);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                l.onStartTrackingTouch((SeekBarAPI26) seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                l.onStopTrackingTouch((SeekBarAPI26) seekBar);
            }
        });
    }
}
