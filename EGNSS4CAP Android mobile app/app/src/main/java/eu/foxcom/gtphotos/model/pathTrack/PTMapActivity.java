package eu.foxcom.gtphotos.model.pathTrack;

import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;
import com.google.maps.android.collections.PolylineManager;

import eu.foxcom.gtphotos.model.AppDatabase;

public interface PTMapActivity {
    AppCompatActivity getAppCompatActivity();

    GoogleMap getGoogleMap();

    AppDatabase getAppDatabase();

    Lifecycle getLifecycle();

    void startingPTException(Exception e);

    void stoppingPTException(Exception e);

    void onStartedPT();

    void onStoppedPT(PTPath ptPath);

    void onNoPointsInPath();

    MarkerManager getMarkerManager();

    PolylineManager getPolylineManager();

    PolygonManager getPolygonManager();

    FrameLayout getInfoCurrentPathLayout();
}
