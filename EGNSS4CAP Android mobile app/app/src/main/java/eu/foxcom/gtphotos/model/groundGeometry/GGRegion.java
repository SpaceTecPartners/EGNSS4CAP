package eu.foxcom.gtphotos.model.groundGeometry;

import com.google.android.gms.maps.model.LatLngBounds;

class GGRegion {
    private double minLat;
    private double maxLat;
    private double minLng;
    private double maxLng;

    public GGRegion(double minLat, double maxLat, double minLng, double maxLng) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLng = minLng;
        this.maxLng = maxLng;
    }

    public GGRegion(LatLngBounds latLngBounds) {
        minLat = Math.min(latLngBounds.northeast.latitude, latLngBounds.southwest.latitude);
        maxLat = Math.max(latLngBounds.northeast.latitude, latLngBounds.southwest.latitude);
        minLng = Math.min(latLngBounds.northeast.longitude, latLngBounds.southwest.longitude);
        maxLng = Math.max(latLngBounds.northeast.longitude, latLngBounds.southwest.longitude);
    }
    // region get, set

    public double getMinLat() {
        return minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public double getMinLng() {
        return minLng;
    }

    public double getMaxLng() {
        return maxLng;
    }

    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
