package eu.foxcom.gtphotos;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import eu.foxcom.gtphotos.model.PhotoDataController;

public class CameraViewModel extends AndroidViewModel {
    public class PositionInfo {

    }

    MainService MS;
    PhotoDataController photoDataController;
    MutableLiveData<Location> currentLocation;

    public CameraViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(MainService MS) {
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
