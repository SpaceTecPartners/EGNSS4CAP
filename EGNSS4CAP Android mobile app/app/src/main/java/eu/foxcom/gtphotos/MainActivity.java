package eu.foxcom.gtphotos;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import eu.foxcom.gtphotos.model.LoggedUser;
import eu.foxcom.gnss_scan.GnssStatusScanner;
import eu.foxcom.gtphotos.model.Util;

public class MainActivity extends BaseActivity {

    public static final int REQUEST_LOCATION_UPDATE_TIMEOUT = 10000;

    private GnssStatusScanner gnssStatusUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setShowMenu(true);
        checkLocationService();
    }

    @Override
    public void serviceInit() {
        super.serviceInit();
        initBasicInfo();
        initLoggedUser();

        checkLocationEnabled();
    }

    private void initBasicInfo() {
        gnssStatusUnit = new GnssStatusScanner(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            goToStartActivity();
            return;
        }
        gnssStatusUnit.startScan(REQUEST_LOCATION_UPDATE_TIMEOUT);

        BasicInfoFragment basicInfoFragment = (BasicInfoFragment) getSupportFragmentManager().findFragmentByTag(BasicInfoFragment.TAG);
        if (basicInfoFragment == null) {
            basicInfoFragment = new BasicInfoFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.hs_frameLayout_basicInfo, basicInfoFragment, BasicInfoFragment.TAG);
            fragmentTransaction.commit();
        }
        basicInfoFragment.initMeasurement(gnssStatusUnit);
    }

    private void initLoggedUser() {
        LoggedUser loggedUser = LoggedUser.createFromAppDatabase(MS.getAppDatabase());
        TextView loginTextView = findViewById(R.id.hs_textView_login);
        loginTextView.setText(loggedUser.getLogin());
        TextView nameTextView = findViewById(R.id.hs_textView_name);
        nameTextView.setText(loggedUser.getName());
        TextView surnameTextView = findViewById(R.id.hs_textView_surname);
        surnameTextView.setText(loggedUser.getSurname());
    }

    private void checkLocationService() {
        if (!Util.isLocationServiceEnabled(this)) {
            alert(getString(R.string.hs_noLocationServiceTitle), getString(R.string.hs_noLocationServiceText));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gnssStatusUnit.stopScan();
    }

    private void checkLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean enabled;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            enabled = locationManager.isLocationEnabled();
        } else {
            enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        if (!enabled) alert(getString(R.string.ms_deviceConditionTitle),getString(R.string.ms_deviceConditionDetail));
    }

    protected AlertDialog alertBuild(String title, String text) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        return alertDialog;
    }
}
