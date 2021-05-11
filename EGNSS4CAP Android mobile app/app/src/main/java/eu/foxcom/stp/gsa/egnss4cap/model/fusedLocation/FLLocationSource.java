package eu.foxcom.stp.gsa.egnss4cap.model.fusedLocation;

import android.location.Location;

import com.google.android.gms.maps.LocationSource;

public class FLLocationSource implements LocationSource {

    private FLManager flManager;
    private OnLocationChangedListener onLocationChangedListener;
    private Location lastLocation;
    private boolean isActive = false;

    FLLocationSource(FLManager flManager) {
        this.flManager = flManager;
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        this.onLocationChangedListener = onLocationChangedListener;
        lastLocation = null;
        isActive = true;
    }

    @Override
    public void deactivate() {
        isActive = false;
    }

    void onNewLocation(Location location) {
        if (
                !isActive
                        || onLocationChangedListener == null
                        || location == null
                        || (lastLocation != null && location.getTime() == lastLocation.getTime())) {
            return;
        }
        onLocationChangedListener.onLocationChanged(location);
        lastLocation = location;
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
