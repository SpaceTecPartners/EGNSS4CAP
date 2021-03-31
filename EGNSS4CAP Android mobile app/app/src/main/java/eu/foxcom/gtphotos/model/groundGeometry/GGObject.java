package eu.foxcom.gtphotos.model.groundGeometry;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class GGObject {

    private static int HEURISTIC_STEP_LIMIT = 5;

    static class GGParseException extends Exception {
        GGParseException(String message) {
            super(message);
        }
    }

    private String label;
    private List<LatLng> outerPoints = new ArrayList<>();
    private List<List<LatLng>> innerPoints = new ArrayList<>();
    private boolean isDrawn = false;
    private Polygon polygon;
    private Marker marker;
    private LatLng centroid;

    static List<GGObject> createListFromResponse(JSONArray shapes) throws JSONException, GGParseException {
        List<GGObject> ggObjects = new ArrayList<>();
        for (int i = 0; i < shapes.length(); ++i) {
            ggObjects.add(createFromJSON(shapes.getJSONObject(i)));
        }
        return ggObjects;
    }

    private static GGObject createFromJSON(JSONObject jsonObject) throws JSONException, GGParseException {
        GGObject ggObject = new GGObject();
        ggObject.label = jsonObject.getString("identificator");
        JSONArray paths = new JSONArray(jsonObject.getString("wgs_geometry"));
        if (paths.length() == 0) {
            throw new GGParseException("wgs_geometry for " + ggObject.label + " is empty array");
        }
        for (int i = 0; i < paths.length(); ++i) {
            JSONArray path = paths.getJSONArray(i);
            if (i > 0) {
                ggObject.innerPoints.add(new ArrayList<>());
            }
            for (int j = 0; j < path.length(); ++j) {
                JSONArray point = path.getJSONArray(j);
                Double lat = point.getDouble(0);
                Double lng = point.getDouble(1);
                LatLng latLng = new LatLng(lat, lng);
                if (i == 0) {
                    ggObject.outerPoints.add(latLng);
                } else {
                    ggObject.innerPoints.get(i - 1).add(latLng);
                }
            }
        }
        ggObject.computeCentroid();
        return ggObject;
    }

    // primitive heuristics walking along axes passing through the center of the square bounding the polygon;
    // determines the centroid on the first hit into the polygon
    private void computeCentroid() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : outerPoints) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        LatLng center = bounds.getCenter();
        if (isPointIn(center)) {
            centroid = center;
            return;
        }
        Location northWest = new Location("");
        northWest.setLatitude(bounds.northeast.latitude);
        northWest.setLongitude(bounds.southwest.longitude);
        Location northEast = new Location("");
        northEast.setLatitude(bounds.northeast.latitude);
        northEast.setLongitude(bounds.northeast.longitude);
        Location southWest = new Location("");
        southWest.setLatitude(bounds.southwest.latitude);
        southWest.setLongitude(bounds.southwest.longitude);
        double widthStep = northWest.distanceTo(northEast) / HEURISTIC_STEP_LIMIT;
        double heightStep = northWest.distanceTo(southWest) / HEURISTIC_STEP_LIMIT;
        LatLng testPoint;
        for (int i = 1; i <= HEURISTIC_STEP_LIMIT; ++i) {
            testPoint = SphericalUtil.computeOffset(center, heightStep * i, 0);
            if (isPointIn(testPoint)) {
                centroid = testPoint;
                return;
            }
            testPoint = SphericalUtil.computeOffset(center, widthStep * i, 90);
            if (isPointIn(testPoint)) {
                centroid = testPoint;
                return;
            }
            testPoint = SphericalUtil.computeOffset(center, heightStep * i, 180);
            if (isPointIn(testPoint)) {
                centroid = testPoint;
                return;
            }
            testPoint = SphericalUtil.computeOffset(center, widthStep * i, 270);
            if (isPointIn(testPoint)) {
                centroid = testPoint;
                return;
            }
        }
        // failed to find point
        centroid = center;
    }

    private boolean isPointIn(LatLng latLng) {
        for (List<LatLng> hole : innerPoints) {
            if (PolyUtil.containsLocation(latLng, hole, false)) {
                return false;
            }
        }
        return PolyUtil.containsLocation(latLng, outerPoints, false);
    }

    private GGObject() {}

    // region get, set

    String getLabel() {
        return label;
    }

    List<LatLng> getOuterPoints() {
        return outerPoints;
    }

    List<List<LatLng>> getInnerPoints() {
        return innerPoints;
    }

    boolean isDrawn() {
        return isDrawn;
    }

    void setDrawn(boolean drawn) {
        isDrawn = drawn;
    }

    Polygon getPolygon() {
        return polygon;
    }

    void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public LatLng getCentroid() {
        return centroid;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    // endregion

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */