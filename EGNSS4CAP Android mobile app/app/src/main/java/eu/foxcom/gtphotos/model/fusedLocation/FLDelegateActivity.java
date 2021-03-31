package eu.foxcom.gtphotos.model.fusedLocation;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.LocationResult;

public interface FLDelegateActivity {
    AppCompatActivity getAppCompatActivity();

    void onNewFusedLocations(LocationResult locationResult);

    void onFLStarted();

    void onFLEnded();
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
