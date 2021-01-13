package eu.foxcom.gtphotos.model.groundGeometry;

import com.google.android.gms.maps.GoogleMap;

class GGWatcher {

    private final int LIMIT_ZOOM_LEVEL = 15;

    private GGManager ggManager;
    private GGMapActivity ggMapActivity;
    private GoogleMap map;

    public GGWatcher(GGManager ggManager) {
        this.ggManager = ggManager;
        this.ggMapActivity = ggManager.getGgMapActivity();
        this.map = ggMapActivity.getMap();
        map.setOnCameraIdleListener(() -> {
            if (map.getCameraPosition().zoom > LIMIT_ZOOM_LEVEL) {
                ggManager.watcherDrawGrounds(new GGRegion(map.getProjection().getVisibleRegion().latLngBounds));
            } else {
                ggManager.watcherHideGrounds();
            }
        });
    }
}
