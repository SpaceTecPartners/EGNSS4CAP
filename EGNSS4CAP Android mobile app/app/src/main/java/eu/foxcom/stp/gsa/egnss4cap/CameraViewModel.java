package eu.foxcom.stp.gsa.egnss4cap;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import eu.foxcom.stp.gsa.egnss4cap.model.PhotoDataController;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EKFStartExeception;
import eu.foxcom.stp.gsa.egnss4cap.model.ekf.EkfCreateException;

public class CameraViewModel extends AndroidViewModel {
    public class PositionInfo {

    }

    MainService MS;
    PhotoDataController photoDataController;
    MutableLiveData<Location> currentLocation;

    public CameraViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(MainService MS) throws EkfCreateException, EKFStartExeception {
        this.MS = MS;
        photoDataController = new PhotoDataController(getApplication());
        currentLocation = new MutableLiveData<>();

        MS.startLocationMonitoring(location -> {
            photoDataController.addLocation(location);
            currentLocation.setValue(location);
        });
        photoDataController.startImmediately();
    }

    public void snapShot() {
        photoDataController.startSnapShot();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        photoDataController.stop();
    }

    // region get, set
    public PhotoDataController getPhotoDataController() {
        return photoDataController;
    }

    public MutableLiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */