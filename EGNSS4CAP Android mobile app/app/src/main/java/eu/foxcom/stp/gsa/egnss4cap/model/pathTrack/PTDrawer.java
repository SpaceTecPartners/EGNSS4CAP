package eu.foxcom.stp.gsa.egnss4cap.model.pathTrack;

import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import eu.foxcom.stp.gsa.egnss4cap.R;
import eu.foxcom.stp.gsa.egnss4cap.model.AppDatabase;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;

class PTDrawer {

    private static final int CAMERA_PADDING = 100;
    private static final int CAMERA_MIN_ZOOM = 16;

    private PTManager ptManager;

    private MarkerManager.Collection markerCollection;
    private PolylineManager.Collection polylineCollection;
    private PolygonManager.Collection polygonCollection;

    private PTPath currentPtPath;
    private PTPoint lastPoint;
    private View currentPathInfoView;

    private DecimalFormat coordinateFormat;
    private DateTimeFormatter dateTimeFormatter;
    private DecimalFormat decimalFormat0;

    public PTDrawer(PTManager ptManager){
        this.ptManager = ptManager;
        markerCollection = ptManager.ptMapActivity.getMarkerManager().newCollection();
        polylineCollection = ptManager.ptMapActivity.getPolylineManager().newCollection();
        polygonCollection = ptManager.ptMapActivity.getPolygonManager().newCollection();

        coordinateFormat = Util.createPrettyCoordinateFormat();
        dateTimeFormatter = Util.createPrettyDateTimeFormat();
        decimalFormat0 = new DecimalFormat("#");
        decimalFormat0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));

        markerCollection.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                PTPoint ptPoint = (PTPoint) marker.getTag();
                AppCompatActivity appCompatActivity = ptManager.ptMapActivity.getAppCompatActivity();
                LayoutInflater inflater = appCompatActivity.getLayoutInflater();
                View view = inflater.inflate(R.layout.info_map_pt_point, null, false);
                TextView latitudeTextView = view.findViewById(R.id.pt_textView_infoPointLat);
                latitudeTextView.setText(coordinateFormat.format(ptPoint.getLatitude()));
                TextView longitudeTextView = view.findViewById(R.id.pt_textView_infoPointLng);
                longitudeTextView.setText(coordinateFormat.format(ptPoint.getLongitude()));
                TextView altitudeTextView = view.findViewById(R.id.pt_textView_infoPointAltitude);
                altitudeTextView.setText(ptPoint.getAltitude() == null ? appCompatActivity.getString(R.string.gn_unavailable) : decimalFormat0.format(ptPoint.getAltitude()));
                TextView accuracyTextView = view.findViewById(R.id.pt_textView_infoPointAccuracy);
                accuracyTextView.setText(ptPoint.getAccuracy() == null ? appCompatActivity.getString(R.string.gn_unavailable)  : decimalFormat0.format(ptPoint.getAccuracy()));
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

    // reload bez vykreslení
    boolean reloadCurrentPathData(AppDatabase appDatabase) {
        if (currentPtPath == null) {
            return false;
        }
        PTPath ptPath = PTPath.createFromAppDatabase(appDatabase, currentPtPath.getAutoId());
        if (ptPath == null) {
            currentPtPath = null;
            return false;
        }
        currentPtPath = ptPath;
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

    void drawCurrentPathInfo() {
        lazyLoadCurrentPathInfoLayout();
        currentPathInfoView.setVisibility(View.VISIBLE);
        TextView idTextView = currentPathInfoView.findViewById(R.id.pt_textView_PTinfoCurPathId);
        TextView infoTitle = currentPathInfoView.findViewById(R.id.pt_textView_PTinfoCurTitle);
        ptManager.isTrackingDisposable(isTracking -> infoTitle.setText(isTracking ? R.string.pt_infoCurPathTitleRecording : R.string.pt_infoCurPathTitleShown));
        TextView centroidsNoteTextView = currentPathInfoView.findViewById(R.id.pt_textView_PTinfoCurPathCentroidsNote);
        if (currentPtPath.isByCentroids()) {
            centroidsNoteTextView.setVisibility(View.VISIBLE);
        } else {
            centroidsNoteTextView.setVisibility(View.GONE);
        }
        drawPathInfoMode(ptManager.isPause() == null ? false : ptManager.isPause());
        if (currentPtPath.isSent()) {
            idTextView.setText(String.valueOf(currentPtPath.getRealId()));
            idTextView.setTypeface(null, Typeface.NORMAL);
        } else {
            idTextView.setText(ptManager.ptMapActivity.getAppCompatActivity().getString(R.string.pt_notSent));
            idTextView.setTypeface(null, Typeface.ITALIC);
        }
        String name = currentPtPath.getName();
        if (name != null) {
            TextView nameTextView = currentPathInfoView.findViewById(R.id.pt_textView_PTinfoCurPathName);
            nameTextView.setText(name);
        }
    }

    void drawPathInfoMode(boolean isVertex) {
        ptManager.isTrackingDisposable(isTracking -> {
            if (currentPathInfoView == null) {
                return;
            }
            TextView pauseTextView = currentPathInfoView.findViewById(R.id.pt_textView_PTinfoCurPathPause);
            pauseTextView.setVisibility(isTracking ? View.VISIBLE : View.GONE);
            pauseTextView.setText(isVertex ? R.string.pt_infoCurPathVertex : R.string.pt_infoCurPathContinuous);
        });
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
        zoom = zoom < CAMERA_MIN_ZOOM ? CAMERA_MIN_ZOOM : zoom;
        CameraPosition  cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(zoom).build();
        ptManager.ptMapActivity.requestAnimatePTOnPoint(CameraUpdateFactory.newCameraPosition(cameraPosition));
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
        ptManager.ptMapActivity.requestAnimatePTOnPath(CameraUpdateFactory.newLatLngBounds(latLngBounds, CAMERA_PADDING));
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
        currentPtPath = null;
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

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
