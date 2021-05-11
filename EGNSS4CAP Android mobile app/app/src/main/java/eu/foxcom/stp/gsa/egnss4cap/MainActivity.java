package eu.foxcom.stp.gsa.egnss4cap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import eu.foxcom.gnss_scan.GnssStatusScanner;
import eu.foxcom.stp.gsa.egnss4cap.model.LoggedUser;
import eu.foxcom.stp.gsa.egnss4cap.model.Photo;
import eu.foxcom.stp.gsa.egnss4cap.model.Task;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;

public class MainActivity extends BaseActivity {

    public static final int REQUEST_LOCATION_UPDATE_TIMEOUT = 10000;

    private GnssStatusScanner gnssStatusUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setToolbar(R.id.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setLogo(R.drawable.title_logo);
        setShowMenu(true);
        resolveShowingLoggedUser();


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
        TextView numberOpenTasksTextView = findViewById(R.id.hs_textView_numberOpenTasks);
        numberOpenTasksTextView.setText(String.valueOf(Task.numberOfTasks(MS.getAppDatabase(), Task.STATUS.OPEN, loggedUser.getId())));
        TextView numberPhotosTextView = findViewById(R.id.hs_textView_numberPhotos);
        numberPhotosTextView.setText(String.valueOf(Photo.numberOfPhotos(MS.getAppDatabase(), loggedUser.getId())));
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

    protected AlertDialog alertBuild_old(String title, String text) {
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

    private void resolveShowingLoggedUser() {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String mode = applicationInfo.metaData.getString(getString(R.string.meta_appMode_name));
            if (getString(R.string.meta_appMode_gsa).equals(mode)) {
                LinearLayout numberOpenTaskLinearLayout = findViewById(R.id.hs_linearLayout_numberOpenTaskRow);
                numberOpenTaskLinearLayout.setVisibility(View.VISIBLE);
                LinearLayout numberPhotosLinearLayout = findViewById(R.id.hs_linearLayout_numberPhotosRow);
                numberPhotosLinearLayout.setVisibility(View.VISIBLE);
            } else if (getString(R.string.meta_appMode_szif).equals(mode)) {
                LinearLayout nameLinearLayout = findViewById(R.id.hs_linearLayout_nameRow);
                nameLinearLayout.setVisibility(View.VISIBLE);
                LinearLayout surnameLinearLayout = findViewById(R.id.hs_linearLayout_surnameRow);
                surnameLinearLayout.setVisibility(View.VISIBLE);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // nothing
        }
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
