package eu.foxcom.gtphotos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import eu.foxcom.gnss_scan.GnssStatusScanner;

public class AboutActivity extends BaseActivity {

    private static final int REQUEST_LOCATION_UPDATE_TIMEOUT = 10000;
    GnssStatusScanner gnssStatusUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setToolbar(R.id.toolbar);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        gnssStatusUnit = new GnssStatusScanner(this);
        if (ActivityCompat.checkSelfPermission(AboutActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            goToStartActivity();
            return;
        }
        gnssStatusUnit.startScan(REQUEST_LOCATION_UPDATE_TIMEOUT);
        BasicInfoFragment basicInfoFragment = (BasicInfoFragment) getSupportFragmentManager().findFragmentByTag(BasicInfoFragment.TAG);
        if (basicInfoFragment == null) {
            basicInfoFragment = new BasicInfoFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.ab_frameLayout_basicInfo, basicInfoFragment, BasicInfoFragment.TAG);
            fragmentTransaction.commit();
        }
        basicInfoFragment.initMeasurement(gnssStatusUnit);

        TextView afterTextView = findViewById(R.id.ab_textView_after);
        afterTextView.setMovementMethod(LinkMovementMethod.getInstance());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            afterTextView.setText(Html.fromHtml(getString(R.string.ab_textAfter), Html.FROM_HTML_MODE_LEGACY));
        } else {
            afterTextView.setText(Html.fromHtml(getString(R.string.ab_textAfter)));
        }


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        gnssStatusUnit.stopScan();
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */