package eu.foxcom.stp.gsa.egnss4cap.model.groundGeometry;

import android.graphics.Bitmap;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;
import com.google.maps.android.ui.IconGenerator;

import java.util.List;

class GGDrawer {
    GGManager ggManager;
    GoogleMap map;
    PolygonManager.Collection polygonCollection;
    MarkerManager.Collection markerCollection;

    GGDrawer(GGManager ggManager) {
        this.ggManager = ggManager;
        this.map = ggManager.getGgMapActivity().getMap();
        this.polygonCollection = ggManager.getGgMapActivity().getPolygonManager().newCollection();
        this.markerCollection = ggManager.getGgMapActivity().getMarkerManager().newCollection();
    }

    void draw() {
        synchronized (ggManager.getGgObjectsMutex()) {
            List<GGObject> ggObjects = ggManager.getGgObjects();
            for(GGObject ggObject : ggObjects) {
                PolygonOptions polygonOptions = new PolygonOptions();
                 polygonOptions.addAll(ggObject.getOuterPoints());
                List<List<LatLng>> holes = ggObject.getInnerPoints();
                for (List<LatLng> hole : holes) {
                    polygonOptions.addHole(hole);
                }
                polygonOptions.fillColor(0x30ea3122);
                polygonOptions.strokeColor(0xffea3122);
                ggObject.setPolygon(polygonCollection.addPolygon(polygonOptions));

                IconGenerator iconGenerator = new IconGenerator(ggManager.getContext());
                TextView textView = new TextView(ggManager.getContext());
                textView.setText(ggObject.getLabel());
                iconGenerator.setContentView(textView);
                Bitmap icon = iconGenerator.makeIcon();
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(ggObject.getCentroid());
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
                ggObject.setMarker(markerCollection.addMarker(markerOptions));

                ggObject.setDrawn(true);
            }
        }
    }

    void delete() {
        synchronized (ggManager.getGgObjectsMutex()) {
            List<GGObject> ggObjects = ggManager.getGgObjects();
            for (GGObject ggObject : ggObjects) {
                ggObject.getPolygon().remove();
                ggObject.getMarker().remove();
            }
        }
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
