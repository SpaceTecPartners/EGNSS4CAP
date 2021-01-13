package eu.foxcom.gtphotos.model.pathTrack;

import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;
import com.google.maps.android.collections.PolylineManager;

import org.joda.time.format.DateTimeFormatter;

import java.text.DecimalFormat;
import java.util.List;

import eu.foxcom.gtphotos.R;
import eu.foxcom.gtphotos.model.Util;

class PTDrawer {

    private static final int CAMERA_PADDING = 100;

    private PTManager ptManager;

    private MarkerManager.Collection markerCollection;
    private PolylineManager.Collection polylineCollection;
    private PolygonManager.Collection polygonCollection;

    private PTPath currentPtPath;
    private PTPoint lastPoint;
    private View currentPathInfoView;

    private DecimalFormat coordinateFormat;
    private DateTimeFormatter dateTimeFormatter;

    public PTDrawer(PTManager ptManager){
        this.ptManager = ptManager;
        markerCollection = ptManager.ptMapActivity.getMarkerManager().newCollection();
        polylineCollection = ptManager.ptMapActivity.getPolylineManager().newCollection();
        polygonCollection = ptManager.ptMapActivity.getPolygonManager().newCollection();

        coordinateFormat = Util.createPrettyCoordinateFormat();
        dateTimeFormatter = Util.createPrettyDateTimeFormat();

        markerCollection.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                PTPoint ptPoint = (PTPoint) marker.getTag();
                LayoutInflater inflater = ptManager.ptMapActivity.getAppCompatActivity().getLayoutInflater();
                View view = inflater.inflate(R.layout.info_map_pt_point, null, false);
                TextView latitudeTextView = view.findViewById(R.id.pt_textView_infoPointLat);
                latitudeTextView.setText(coordinateFormat.format(ptPoint.getLatitude()));
                TextView longitudeTextView = view.findViewById(R.id.pt_textView_infoPointLng);
                longitudeTextView.setText(coordinateFormat.format(ptPoint.getLongitude()));
                TextView orderTextView = view.findViewById(R.id.pt_textView_infoPointOrder);
                orderTextView.setText(String.valueOf(ptPoint.getIndex() + 1));
                TextView createdTextView = view.findViewById(R.id.pt_textView_infoPointCreated);
                createdTextView.setText(ptPoint.getCreated().toString(dateTimeFormatter));

                return view;
            }
        });

    }

    private boolean drawAllPath(PTPath ptPath) {
        if (ptPath == null) {
            return false;
        }

        currentPtPath = ptPath;
        drawCurrentPathInfo();

        int n = ptPath.getPoints().size();
        if (n == 0) {
            lastPoint = null;
            return false;
        }
        for (PTPoint ptPoint : ptPath.getPoints()) {
            addMarker(ptPoint);
        }
        lastPoint = ptPath.getPoints().get(n - 1);
        return true;
    }

    void drawAllPathAsPolyline(PTPath ptPath) {
        if (!drawAllPath(ptPath)) {
            return;
        }

        int n = ptPath.getPoints().size();
        if (n > 1) {
            for (int i = 0; i < n - 1; ++i) {
                PTPoint startPoint = ptPath.getPoints().get(i);
                PTPoint endPoint = ptPath.getPoints().get(i + 1);
                addPolyline(startPoint, endPoint);
            }
        }
    }

    void drawAllPathAsPolygon(PTPath ptPath) {
        if (!drawAllPath(ptPath)) {
            return;
        }

        int n = ptPath.getPoints().size();
        if (n > 1) {
            addPolygon(ptPath.getPoints());
        }
    }

    private void lazyLoadCurrentPathInfoLayout() {
        if (currentPathInfoView != null) {
            return;
        }
        FrameLayout frameLayout = ptManager.ptMapActivity.getInfoCurrentPathLayout();
        LayoutInflater inflater = ptManager.ptMapActivity.getAppCompatActivity().getLayoutInflater();
        currentPathInfoView = inflater.inflate(R.layout.info_current_track, frameLayout, true);
        currentPathInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centralizeToPath(currentPtPath);
            }
        });
    }

    private void drawCurrentPathInfo() {
        lazyLoadCurrentPathInfoLayout();
        currentPathInfoView.setVisibility(View.VISIBLE);
        TextView idTextView = currentPathInfoView.findViewById(R.id.pt_textView_infoCurPathId);
        TextView infoTitle = currentPathInfoView.findViewById(R.id.pt_textView_infoCurTitle);
        ptManager.isTrackingDisposable(isTracking -> infoTitle.setText(isTracking ? R.string.pt_infoCurPathTitleRecording : R.string.pt_infoCurPathTitleShown));
        TextView centroidsNoteTextView = currentPathInfoView.findViewById(R.id.pt_textView_infoCurPathCentroidsNote);
        if (currentPtPath.isByCentroids()) {
            centroidsNoteTextView.setVisibility(View.VISIBLE);
        } else {
            centroidsNoteTextView.setVisibility(View.GONE);
        }
        if (currentPtPath.isSent()) {
            idTextView.setText(String.valueOf(currentPtPath.getRealId()));
            idTextView.setTypeface(idTextView.getTypeface(), Typeface.NORMAL);
        } else {
            idTextView.setText(ptManager.ptMapActivity.getAppCompatActivity().getString(R.string.pt_notSent));
            idTextView.setTypeface(idTextView.getTypeface(), Typeface.ITALIC);
        }
        String name = currentPtPath.getName();
        if (name != null) {
            TextView nameTextView = currentPathInfoView.findViewById(R.id.pt_textView_infoCurPathName);
            nameTextView.setText(name);
        }
    }

    private void hideCurrentPathInfo() {
        lazyLoadCurrentPathInfoLayout();
        currentPathInfoView.setVisibility(View.GONE);
    }

    void moveToLocation(Location location) {
        if (location == null) {
            return;
        }
        GoogleMap map = ptManager.ptMapActivity.getGoogleMap();
        float zoom = map.getCameraPosition().zoom;
        zoom = zoom < 16 ? 16 : zoom;
        CameraPosition  cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(zoom).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    void centralizeToPath(PTPath ptPath) {
        LatLngBounds.Builder builder = LatLngBounds.builder();
        if (ptPath.getPoints().size() == 0) {
            return;
        }
        for (PTPoint ptPoint : ptPath.getPoints()) {
            builder.include(new LatLng(ptPoint.getLatitude(), ptPoint.getLongitude()));
        }
        LatLngBounds latLngBounds = builder.build();
        ptManager.ptMapActivity.getGoogleMap().animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, CAMERA_PADDING));
    }

    void addPointToPath(PTPoint ptPoint) {
        if (lastPoint == ptPoint) {
            return;
        }
        addMarker(ptPoint);
        if (lastPoint != null) {
            addPolyline(lastPoint, ptPoint);
        }
        lastPoint = ptPoint;
    }

    void removePath() {
        markerCollection.clear();
        polylineCollection.clear();
        polygonCollection.clear();
        lastPoint = null;
        hideCurrentPathInfo();
    }

    private void addMarker(PTPoint ptPoint) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(ptPoint.getLatitude(), ptPoint.getLongitude()));
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_path_point));
        Marker marker = markerCollection.addMarker(markerOptions);
        marker.setTag(ptPoint);
    }

    private void addPolyline(PTPoint startPoint, PTPoint endPoint) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.add(new LatLng(startPoint.getLatitude(), startPoint.getLongitude()), new LatLng(endPoint.getLatitude(), endPoint.getLongitude()));
        polylineCollection.addPolyline(polylineOptions);
    }

    private void addPolygon(List<PTPoint> points) {
        PolygonOptions polygonOptions = new PolygonOptions();
        for (PTPoint point : points) {
            polygonOptions.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }
        polygonOptions.strokeColor(Color.BLUE);
        polygonOptions.fillColor(Color.argb(100, 0, 0, 225));
        polygonCollection.addPolygon(polygonOptions);
    }

    // region get, set

    public PTPath getCurrentPtPath() {
        return currentPtPath;
    }

    // endregion
}
