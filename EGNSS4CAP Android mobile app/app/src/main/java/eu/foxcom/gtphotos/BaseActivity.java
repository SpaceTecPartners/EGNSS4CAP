package eu.foxcom.gtphotos;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import eu.foxcom.gtphotos.MainService.BROADCAST_MSG;
import eu.foxcom.gtphotos.MainService.BROADCAST_REFRESH_TASKS_PARAMS;
import eu.foxcom.gtphotos.model.LoggedUser;
import eu.foxcom.gtphotos.model.FileLogger;

public abstract class BaseActivity extends AppCompatActivity implements ServiceInit {

    public enum INTENT_MSG {
        ;

        public final String ID;

        INTENT_MSG(String id) {
            ID = id;
        }
    }

    public static final String TAG = BaseActivity.class.getSimpleName();
    private static final int EXIT_STATUS = 11;

    public static final int MULTIPLE_PERMISSIONS = 10;
    public static final int OWN_RESOLVER_PERMISSIONS = 11;

    private static final int LOCATION_REQUEST_MILS = 1000;
    private static final int LOCATION_REQUEST_MILS_FASTEST = 1000;
    private static final int SYNC_MONITORINT_INTERVAL_MILS = 2000;

    protected FileLogger fileLogger;

    protected ServiceController serviceController;
    protected MainService MS;
    private boolean isShowMenu = false;
    /**
     * resume -> pause
     */
    protected boolean isActive = false;
    /**
     * created -> destroy
     */
    protected boolean isCreated = false;
    /**
     * start -> stop
     */
    protected boolean isRunning = false;
    protected boolean isDestroying = false;
    protected boolean isAutoCheckPermissions = true;

    private Handler syncMonitoringHandler;
    private Runnable syncMonitoringRunnable;

    private AlertDialog syncDialog;

    String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isDestroying = false;

        fileLogger = new FileLogger(getApplicationContext(), this.getClass());

        serviceController = new ServiceController(this, new ServiceGetter() {
            @Override
            public Service getService(IBinder binder) {
                return ((MainService.LocalBinder) binder).getService();
            }
        }, MainService.class);
        serviceController.setServiceInit(this);
        serviceController.registerBroadcastReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        broadcastReceiver(context, intent);
                    }
                }, new IntentFilter(BROADCAST_MSG.BROADCAST_ID.ID)
        );
        serviceController.startService();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        isCreated = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isAutoCheckPermissions) {
            checkPermissions();
        }

        isRunning = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActive = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        serviceController.unbindService();
        serviceController.unregisterBroadcastReceiver();
        serviceController = null;

        syncMonitoringHandler.removeCallbacks(syncMonitoringRunnable);
        syncMonitoringHandler = null;
        syncMonitoringRunnable = null;

        isCreated = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MULTIPLE_PERMISSIONS) {
            if (!onRequestPermissionResultAllGranted(requestCode, permissions, grantResults)) {
                goToStartActivity();
            }
        }
    }

    protected void goToStartActivity() {
        Intent intent = new Intent(this, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        finish();
    }

    protected void onRequestPermissionResultAlert(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MULTIPLE_PERMISSIONS) {
            int size = grantResults.length;
            for (int i = 0; i < size; ++i) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    alertMissingPermission(permissions[i]);
                }
            }
        }
    }

    protected boolean onRequestPermissionResultAllGranted(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allGranted = true;
        int size = grantResults.length;
        for (int i = 0; i < size; ++i) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        return allGranted;
    }

    public void alertMissingPermission(String permission) {
        alert(getString(R.string.bs_permissionDeniedTitle), getString(R.string.bs_permissionDeniedContent, permission));
    }

    protected BROADCAST_MSG broadcastReceiver(Context context, Intent intent) {
        BROADCAST_MSG broadcastMsg = BROADCAST_MSG.createFromID(intent.getStringExtra(BROADCAST_MSG.TYPE.ID));
        if (isFinishing()) {
            return broadcastMsg;
        }
        switch (broadcastMsg) {
            case STARTED:
                serviceStarted();
                break;
            case REFRESH_TASKS_STARTED:
                if (!isActive) {
                    break;
                }
                refreshTasksStarted();
                break;
            case REFRESH_TASKS:
                if (!isActive) {
                    break;
                }
                refreshTasks(intent);
                break;
            case UPLOAD_TASK_STATUS:
                if (!isActive) {
                    break;
                }
                uploadTaskStatus(intent);
                break;
            case REFRESH_PHOTOS:
                refreshPhotos(intent);
                break;
        }
        // override
        return broadcastMsg;
    }

    protected void intentReceiver() {
        Intent intent = getIntent();

    }

    private void syncLockActivate() {
        if (syncDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(R.layout.dialog_sync);
            AlertDialog dialog = builder.create();
            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.dl_OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            syncDialog = dialog;
        }
        syncDialog.setCancelable(false);
        syncDialog.show();
        syncDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.GONE);
        TextView stateTextView = syncDialog.findViewById(R.id.dsync_textView_state);
        stateTextView.setText(R.string.bs_syncStateProgress);
        ConstraintLayout waitConstraintLayout = syncDialog.findViewById(R.id.dsync_constraintLayout_wait);
        TextView errorsTextView = syncDialog.findViewById(R.id.dsync_textView_errors);
        errorsTextView.setText("");
        waitConstraintLayout.setVisibility(View.VISIBLE);
    }

    private void syncLockDeactivate() {
        if (syncDialog != null && syncDialog.isShowing()) {
            syncDialog.setCancelable(true);
            TextView stateTextView = syncDialog.findViewById(R.id.dsync_textView_state);
            stateTextView.setText(MS.isSyncSuccess() ? R.string.bs_syncStateCompleteSuccess : R.string.bs_syncStateCompleteError);
            ConstraintLayout waitConstraintLayout = syncDialog.findViewById(R.id.dsync_constraintLayout_wait);
            waitConstraintLayout.setVisibility(View.GONE);
            TextView errorsTextView = syncDialog.findViewById(R.id.dsync_textView_errors);
            if (MS.isSyncSuccess()) {
                errorsTextView.setText("");
            } else {
                String errors = "";
                for (String error : MS.getLastSyncErrors()) {
                    errors += error + "\n\n";
                }
                errorsTextView.setText(errors);
            }
            syncDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
        }
    }

    protected void refreshTasksStarted() {
        syncLockActivate();
    }

    protected void refreshTasks(Intent intent) {
        if (!isActive) {
            return;
        }
        syncLockDeactivate();
        /* disabled
        boolean success = intent.getBooleanExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false);
        if (success) {
            Toast.makeText(this, getString(R.string.gn_taskRefreshSuccess), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.gn_taskRefreshError, intent.getStringExtra(BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID)), Toast.LENGTH_SHORT).show();
        }
         */
    }

    protected void uploadTaskStatus(Intent intent) {
        boolean success = intent.getBooleanExtra(MainService.BROADCAST_UPLOAD_TASK_STATUS_PARAMS.SUCCESS.ID, false);
        if (!success) {
            Toast.makeText(this, getString(R.string.gn_taskUploadStatusFailed, intent.getStringExtra(BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID)), Toast.LENGTH_SHORT).show();
        }
    }

    protected void serviceStarted() {
        // override
    }

    protected void refreshPhotos(Intent intent) {
        if (!isActive) {
            return;
        }
        if (intent.getBooleanExtra(MainService.BROADCAST_REFRESH_PHOTOS_PARAMS.SUCCESS.ID, false)) {
            return;
        }
        // alert(getString(R.string.gn_failure), getString(R.string.gn_failedPhotoDownload, intent.getStringExtra(MainService.BROADCAST_REFRESH_PHOTOS_PARAMS.ERROR_MSG.ID)));
    }

    @Override
    public void serviceInit() {
        if (isDestroying) {
            return;
        }

        MS = (MainService) serviceController.getService();
        syncMonitoringHandler = new Handler();
        syncMonitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (isActive) {
                    if (MS.isSync()) {
                        syncLockActivate();
                    } else {
                        syncLockDeactivate();
                    }
                }
                syncMonitoringHandler.postDelayed(this, SYNC_MONITORINT_INTERVAL_MILS);
            }
        };
        syncMonitoringHandler.postDelayed(syncMonitoringRunnable, 0);
        intentReceiver();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        // override
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

    protected void alert(String title, String text) {
        AlertDialog alertDialog = alertBuild(title, text);
        alertDialog.show();
    }

    protected AlertDialog alertModal(String title, String text) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setCancelable(false);
        alertDialog.show();
        return alertDialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isShowMenu) {
            MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(R.menu.main_menu, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menuItem_syncAll:
                MS.syncAll();
                return true;
            case R.id.menuItem_home:
                showHome();
                return true;
            case R.id.menuItem_taskOverview:
                showTaskOverView();
                return true;
            case R.id.menuItem_unownedPhoto:
                showUnownedPhoto();
                return true;
            case R.id.menuItem_map:
                showMap();
                return true;
            case R.id.menuItem_about:
                showAbout();
                return true;
            case R.id.menuItem_logout:
                logout();
                return true;
            case R.id.menuItem_settings:
                showSettings();
                return true;
            case R.id.menuItem_gnssRawData:
                showGnssRawData();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    protected void logout() {
        quit(getWindow().getDecorView().getRootView());
        LoggedUser.logout(MS.getAppDatabase());
        restartApp();
    }

    protected void restartApp() {
        Intent intent = new Intent(this, StartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    protected void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void showHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void showTaskOverView() {
        Intent intent = new Intent(this, TaskOverviewActivity.class);
        startActivity(intent);
    }

    private void showMap() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    private void showAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    public void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void showGnssRawData() {
        Intent intent = new Intent(this, GnssRawActivity.class);
        startActivity(intent);
    }

    public void showUnownedPhoto() {
        Intent intent = new Intent(this, UnownedPhotoOverviewActivity.class);
        startActivity(intent);
    }

    public void quit(View view) {
        serviceController.stopService();
        finishAndRemoveTask();
    }

    protected boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }


    // region get, set

    public ServiceController getServiceController() {
        return serviceController;
    }

    public boolean isShowMenu() {
        return isShowMenu;
    }

    public void setShowMenu(boolean showMenu) {
        isShowMenu = showMenu;
    }

    public boolean isAutoCheckPermissions() {
        return isAutoCheckPermissions;
    }

    public void setAutoCheckPermissions(boolean autoCheckPermissions) {
        isAutoCheckPermissions = autoCheckPermissions;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isCreated() {
        return isCreated;
    }

    public boolean isRunning() {
        return isRunning;
    }

    // endregion
}
