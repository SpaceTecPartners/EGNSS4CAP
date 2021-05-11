package eu.foxcom.stp.gsa.egnss4cap.model.pathTrack;

import android.location.Location;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;
import com.google.maps.android.collections.PolylineManager;

import eu.foxcom.stp.gsa.egnss4cap.model.AppDatabase;
import eu.foxcom.stp.gsa.egnss4cap.model.Requestor;

public interface PTMapActivity {
    AppCompatActivity getAppCompatActivity();

    GoogleMap getGoogleMap();

    AppDatabase getAppDatabase();

    Lifecycle getLifecycle();

    void startingPTException(Exception e);

    void stoppingPTException(Exception e);

    void onStartedPT();

    void onStoppedPT(PTPath ptPath);

    void onPausePT();

    void onContinuePT();

    void onNewUnfilteredLocationPT (Location location);

    void onNoPointsInPath();

    MarkerManager getMarkerManager();

    PolylineManager getPolylineManager();

    PolygonManager getPolygonManager();

    FrameLayout getInfoCurrentPathLayout();

    PTIsPathsUploadingBinder getPtIsPathsUploadingBinder();

    Requestor getRequestor();

    void uploadDrawnPathStarted();

    void uploadDrawnPathSuccess();

    void uploadDrawnPathFailed(String errMsg);

    void uploadDrawnPathComplete();

    void requestAnimatePTOnPoint(CameraUpdate cameraUpdate);

    void requestAnimatePTOnPath(CameraUpdate cameraUpdate);
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
