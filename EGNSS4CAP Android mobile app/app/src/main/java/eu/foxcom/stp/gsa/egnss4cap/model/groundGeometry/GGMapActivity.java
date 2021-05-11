package eu.foxcom.stp.gsa.egnss4cap.model.groundGeometry;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;

import eu.foxcom.stp.gsa.egnss4cap.model.Requestor;

public interface GGMapActivity {
    GoogleMap getMap();

    void alert(String title, String text);

    Requestor getRequestor();

    PolygonManager getPolygonManager();

    MarkerManager getMarkerManager();
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */