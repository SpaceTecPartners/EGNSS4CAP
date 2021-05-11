package eu.foxcom.stp.gsa.egnss4cap.model.fusedLocation;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.GoogleMap;

public interface FLDelegateActivity {
    AppCompatActivity getAppCompatActivity();

    public GoogleMap getGoogleMap();

    void onNewFusedLocations(LocationResult locationResult);

    void onFLStarted();

    void onFLEnded();
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
