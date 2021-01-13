package eu.foxcom.gtphotos.model.groundGeometry;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;

import eu.foxcom.gtphotos.model.Requestor;

public interface GGMapActivity {
    GoogleMap getMap();

    void alert(String title, String text);

    Requestor getRequestor();

    PolygonManager getPolygonManager();

    MarkerManager getMarkerManager();
}
