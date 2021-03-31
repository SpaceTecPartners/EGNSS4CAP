package eu.foxcom.gtphotos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

public class PermissionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        setToolbar(R.id.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setAutoCheckPermissions(false);
    }

    public void grandPermissions(View view) {
        if(checkLocatiomPermissions()) {
            Intent intent = new Intent(this, StartActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (onRequestPermissionResultAllGranted(requestCode, permissions, grantResults)) {
            Intent intent = new Intent(this, StartActivity.class);
            startActivity(intent);
            finish();
        } else {
            onRequestPermissionResultAlert(requestCode, permissions, grantResults);
        }
    }
}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */