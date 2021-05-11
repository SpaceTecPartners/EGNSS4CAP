package eu.foxcom.stp.gsa.egnss4cap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.PathParser;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;
import com.google.maps.android.collections.PolylineManager;

import org.joda.time.DateTime;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.foxcom.stp.gsa.egnss4cap.model.AppDatabase;
import eu.foxcom.stp.gsa.egnss4cap.model.MyAlertDialog;
import eu.foxcom.stp.gsa.egnss4cap.model.PersistData;
import eu.foxcom.stp.gsa.egnss4cap.model.Photo;
import eu.foxcom.stp.gsa.egnss4cap.model.PhotoList;
import eu.foxcom.stp.gsa.egnss4cap.model.Requestor;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;
import eu.foxcom.stp.gsa.egnss4cap.model.convexHullMap.CHDrawer;
import eu.foxcom.stp.gsa.egnss4cap.model.convexHullMap.CHMapActivity;
import eu.foxcom.stp.gsa.egnss4cap.model.convexHullMap.CHService;
import eu.foxcom.stp.gsa.egnss4cap.model.fusedLocation.FLDelegateActivity;
import eu.foxcom.stp.gsa.egnss4cap.model.fusedLocation.FLManager;
import eu.foxcom.stp.gsa.egnss4cap.model.groundGeometry.GGManager;
import eu.foxcom.stp.gsa.egnss4cap.model.groundGeometry.GGMapActivity;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTIsPathsUploadingBinder;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTManager;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTMapActivity;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTOnNoPointToAdd;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTOnNoPointToDelete;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTPath;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTPoint;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTService;

public class MapActivity extends BaseActivity implements OnMapReadyCallback, GGMapActivity, PTMapActivity, PTOnNoPointToAdd, PTOnNoPointToDelete, CHMapActivity, FLDelegateActivity {

    private static class PTIsPathsUploadingBinderImpl implements PTIsPathsUploadingBinder {

        MainService mainService;

        private PTIsPathsUploadingBinderImpl(MainService mainService) {
            this.mainService = mainService;
        }

        @Override
        public boolean isPathsUploading() {
            return mainService.isPathsUploading();
        }

        @Override
        public void setPathsUploading(boolean isUploading) {
            mainService.setPathsUploading(isUploading);
        }
    }

    // use enum.name () for intent value
    public enum START_MODE {
        PATH_TRACKING,
        TASK_PHOTOS,
        UNOWNED_PHOTOS,
        ALL_PHOTOS,
    }

    public static final String INTENT_ACTION_START = "INTENT_ACTION_START";
    public static final String INTENT_ACTION_START_MODE = "INTENT_ACTION_START_MODE";
    public static final String INTENT_ACTION_START_TASK_ID = "INTENT_ACTION_START_TASK_ID";

    public static final String INTENT_ACTION_SHOW_TRACK = "INTENT_ACTION_SHOW_TRACK";
    public static final String INTENT_ACTION_SHOW_TRACK_ID = "INTENT_ACTION_SHOW_TRACK_ID";

    private static final int ICON_PHOTO_WIDTH = 200;
    private static final int ICON_PHOTO_HEIGHT = 366;
    private static final int ICON_PHOTO_CONTENT_WIDTH = 200;
    private static final int ICON_PHOTO_CONTENT_HEIGHT = 243;
    private static final int ICON_PHOTO_BORDER_WIDTH = 11;
    private static final int ICON_PHOTO_BORDER_HEIGHT = 11;
    private static final int CAMERA_ANIMATION_DURATION_MILS = 1000;
    private static final int CAMERA_ANIMATION_DELAY_MILS = 3000;
    private static final int BOUND_SPACE_PADDING = 350;
    private static final double MIN_BOUND_DEGREE = 0.001;
    private static final int FUSED_LOCATION_INTERVAL = 1000;
    private static final int FUSED_LOCATION_INTERVAL_FASTEST = 800;
    private static final int FUSED_LOCATION_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private static final int CAMERA_ANIMATION_TRACK_DURATION = 700;
    private static final int CAMERA_MIN_ZOOM = 16;

    private START_MODE startMode;
    private String taskId;
    private Map<Long, Photo> photoMap = new HashMap<>();
    private GoogleMap mMap;
    private boolean isMapLoaded = false;
    private boolean isMapInitialized = false;
    private DateTime lastCameraTrackTimestamp;
    private boolean isCameraTrackAnimating = false;
    // <photoId, marker>
    private Map<Long, Marker> markers = new HashMap<>();
    private DecimalFormat altitudeDecimalFormat;
    private PolygonManager polygonManager;
    private MarkerManager markerManager;
    private PolylineManager polylineManager;
    private MarkerManager.Collection photoMarkerCollection;
    private MarkerManager.Collection azimMarkerCollection;
    private GGManager ggManager;
    private PTManager ptManager;
    private CHDrawer chDrawer;
    private FLManager flManager;

    private boolean setStartMode() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        if (!INTENT_ACTION_START.equals(intent.getAction())) {
            return false;
        }
        if (!intent.hasExtra(INTENT_ACTION_START_MODE)) {
            return false;
        }
        try {
            startMode = START_MODE.valueOf(intent.getStringExtra(INTENT_ACTION_START_MODE));
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
        if (startMode.equals(START_MODE.TASK_PHOTOS)) {
            if (!intent.hasExtra(INTENT_ACTION_START_TASK_ID)) {
                return false;
            }
            taskId = intent.getStringExtra(INTENT_ACTION_START_TASK_ID);
            if (taskId == null) {
                return false;
            }
        }
        return true;
    }

    private void initStartModeUI() {
        if (startMode.equals(START_MODE.PATH_TRACKING)) {
            ConstraintLayout pathConstraintLayout = findViewById(R.id.map_constraintLayout_pathTools);
            pathConstraintLayout.setVisibility(View.VISIBLE);
        } else if (startMode.equals(START_MODE.TASK_PHOTOS)
                || startMode.equals(START_MODE.UNOWNED_PHOTOS)
                || startMode.equals(START_MODE.ALL_PHOTOS)
        ) {
            ConstraintLayout photosConstraintLayout = findViewById(R.id.map_constraintLayout_photosTools);
            photosConstraintLayout.setVisibility(View.VISIBLE);
            LayoutInflater inflater = getLayoutInflater();
            FrameLayout photoFrameLayout = findViewById(R.id.map_frameLayout_currentPhotos);
            View currentPathsView = inflater.inflate(R.layout.info_current_photos, photoFrameLayout, true);
            currentPathsView.setOnClickListener(v -> {
                if (!isMapInitialized) {
                    return;
                }
                moveCameraToAllPhotos();
            });
            if (startMode.equals(START_MODE.TASK_PHOTOS)) {
                TableRow taskIdTableRow = findViewById(R.id.map_tableRow_infoCurPSTaskId);
                taskIdTableRow.setVisibility(View.VISIBLE);
            } else if (startMode.equals(START_MODE.ALL_PHOTOS)
                    || startMode.equals(START_MODE.UNOWNED_PHOTOS)
            ) {
                TextView subtitleTextView = findViewById(R.id.map_textView_infoCurPSSubtitle);
                subtitleTextView.setVisibility(View.VISIBLE);
                subtitleTextView.setText(R.string.map_infoCurPSSubtitleAll);
                if (startMode.equals(START_MODE.ALL_PHOTOS)) {
                    subtitleTextView.setText(R.string.map_infoCurPSSubtitleAll);
                } else if (startMode.equals(START_MODE.UNOWNED_PHOTOS)) {
                    subtitleTextView.setText(R.string.map_infoCurPSSubtitleUnowned);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        setToolbar(R.id.toolbar);

        if (!setStartMode()) {
            finish();
            return;
        }
        initStartModeUI();

        altitudeDecimalFormat = new DecimalFormat("#.##");
        altitudeDecimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));

        ToggleButton toggleButton = findViewById(R.id.map_toggleButton_mapType);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMap.setMapType(isChecked ? GoogleMap.MAP_TYPE_SATELLITE : GoogleMap.MAP_TYPE_NORMAL);
            }
        });
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ptManager != null) {
            ptManager.onResume();
        }
        adjustTrackSendButton();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            if (startMode.equals(START_MODE.TASK_PHOTOS) || startMode.equals(START_MODE.UNOWNED_PHOTOS)) {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnCameraMoveStartedListener(i -> {
            if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                createCameraTrackDelay();
            }
        });
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                isMapLoaded = true;
                initMap();
                isMapInitialized = true;
            }
        });
    }

    private void initManagers() {
        polygonManager = new PolygonManager(mMap);
        markerManager = new MarkerManager(mMap);
        polylineManager = new PolylineManager(mMap);
    }

    private void initMap() {
        initManagers();
        initFusedLocation();
        if (startMode.equals(START_MODE.UNOWNED_PHOTOS)
                || startMode.equals(START_MODE.ALL_PHOTOS)
                || startMode.equals(START_MODE.TASK_PHOTOS)
        ) {
            initPhotos();
            initPhotoMarkers();
            afterInitPhotos();
        } else if (startMode.equals(START_MODE.PATH_TRACKING)) {
            initPathTracking();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                initConvexHullDrawer();
            }
        }
        initGroundGeometry();
        initMoveCamera();
    }

    private void initPhotoMarkers() {
        azimMarkerCollection = markerManager.newCollection();
        photoMarkerCollection = markerManager.newCollection();
        for (Map.Entry<Long, Photo> entry : photoMap.entrySet()) {
            addMarkerAzim(entry.getValue());
            addMarkerPhoto(entry.getValue());
        }
    }

    private void initMoveCamera() {
        if (startMode.equals(START_MODE.ALL_PHOTOS)
                || startMode.equals(START_MODE.UNOWNED_PHOTOS)
                || startMode.equals(START_MODE.TASK_PHOTOS)
        ) {
            moveCameraToAllPhotos();
        }
        // TODO přesun na aktuální pozici
    }

    private void initGroundGeometry() {
        ggManager = new GGManager(this);
    }

    private void initPathTracking() {
        ptManager = new PTManager(this, MS.getAppDatabase());
    }

    @SuppressLint("MissingPermission")
    private void initFusedLocation() {
        flManager = new FLManager(getApplicationContext());
        flManager.setFlDelegateActivity(this);
        flManager.setCameraZoom(CAMERA_MIN_ZOOM);
        flManager.setCameraAnimateDurationMils(CAMERA_ANIMATION_DURATION_MILS);
        flManager.requestCameraMoveToNewLocation();
        flManager.setLooper(getMainLooper());
        flManager.setRequest(FUSED_LOCATION_INTERVAL, FUSED_LOCATION_INTERVAL_FASTEST, FUSED_LOCATION_PRIORITY);
        if (locationPermissionCheck()) {
            flManager.start();
            mMap.setLocationSource(flManager.getFlLocationSource());
            mMap.setMyLocationEnabled(true);
        }

    }

    private boolean locationPermissionCheck() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            alert(getString(R.string.map_failedCurrentLocationTitle), getString(R.string.map_failedCurrentLocationText));
            return false;
        } else {
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initConvexHullDrawer() {
        chDrawer = new CHDrawer(this);
        TableRow centroidLabelTableRow = findViewById(R.id.map_tableRow_centroidLabel);
        centroidLabelTableRow.setVisibility(View.VISIBLE);
        TableRow samplesTableRow = findViewById(R.id.map_tableRow_samples);
        samplesTableRow.setVisibility(View.VISIBLE);
        TableRow latTableRow = findViewById(R.id.map_tableRow_latCentroid);
        latTableRow.setVisibility(View.VISIBLE);
        TableRow lngTableRow = findViewById(R.id.map_tableRow_lngCentroid);
        lngTableRow.setVisibility(View.VISIBLE);
    }

    private void moveCameraToAllPhotos() {
        if (markers.size() == 0) {
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Map.Entry<Long, Photo> entry : photoMap.entrySet()) {
            Photo photo = entry.getValue();
            LatLng point = new LatLng(photo.getLat(), photo.getLng());
            builder.include(point);
        }
        LatLngBounds latLngBounds = builder.build();
        LatLng center = latLngBounds.getCenter();
        builder.include(new LatLng(center.latitude - MIN_BOUND_DEGREE, center.longitude - MIN_BOUND_DEGREE));
        builder.include(new LatLng(center.latitude + MIN_BOUND_DEGREE, center.longitude + MIN_BOUND_DEGREE));
        latLngBounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, BOUND_SPACE_PADDING), CAMERA_ANIMATION_DURATION_MILS, null);
    }

    private double getMaxDistance(List<Location> locations) {
        double maxDistance = 0;
        for (int i = 0; i < locations.size(); ++i) {
            for (int j = 0; j < locations.size(); ++j) {
                if (i == j) {
                    continue;
                }
                double distance = locations.get(i).distanceTo(locations.get(j));
                if (distance > maxDistance) {
                    maxDistance = distance;
                }
            }
        }
        return maxDistance;
    }

    private void addMarkerPhoto(Photo photo) {
        MarkerOptions myMarkerOptions = new MarkerOptions();
        Bitmap photoBitmap = null;
        try {
            photoBitmap = photo.getRotatedBitmap();
        } catch (IOException e) {
            alert(getString(R.string.map_drawingError), getString(R.string.map_failedDrawIconPhotoText, e.getMessage()));
        }
        if (photoBitmap != null) {
            myMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(createIconPhotoBitmap(photoBitmap)));
        }
        photoMarkerCollection.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View view = getLayoutInflater().inflate(R.layout.map_photo_info, null);
                Photo photo = (Photo) marker.getTag();
                if (photo == null) {
                    return null;
                }
                TextView taskIdTextView = view.findViewById(R.id.map_textView_taskIdValue);
                String taskId = photo.getTaskId();
                if (taskId != null) {
                    taskIdTextView.setText(photo.getTaskId().toString());
                } else {
                    taskIdTextView.setText(getString(R.string.map_unownedTask));
                    taskIdTextView.setTypeface(taskIdTextView.getTypeface(), Typeface.ITALIC);
                    TextView infoTouchTextView = view.findViewById(R.id.map_textView_infoTouch);
                    infoTouchTextView.setVisibility(View.GONE);
                }
                TextView latTextView = view.findViewById(R.id.map_textView_latValue);
                latTextView.setText(photo.getLat() == null ? "" : photo.getLat().toString());
                TextView lngTextView = view.findViewById(R.id.map_textView_lngValue);
                lngTextView.setText(photo.getLng() == null ? "" : photo.getLng().toString());
                TextView altTextView = view.findViewById(R.id.map_photoInfo_altValue);
                Double alt = photo.getAltitude();
                String altS = getString(R.string.map_nd);
                if (alt != null) {
                    altS = altitudeDecimalFormat.format(alt);
                }
                altTextView.setText(altS);
                TextView createdTextView = view.findViewById(R.id.map_textView_createdValue);
                createdTextView.setText(photo.getCreated() == null ? getString(R.string.map_nd) : photo.getCreated().toString());

                return view;
            }
        });
        photoMarkerCollection.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Photo photo = (Photo) marker.getTag();

                Intent intent = new Intent(MapActivity.this, TaskFulfillActivity.class);
                intent.setAction(TaskFulfillActivity.INTENT_ACTION_START);
                intent.putExtra(TaskFulfillActivity.INTENT_ACTION_START_TASK_ID, photo.getTaskId());
                intent.putExtra(TaskFulfillActivity.INTENT_ACTION_START_INIT_MOVE_PHOTO_INDX, photo.getIndx());
                startActivity(intent);
            }
        });
        LatLng borderLocation = new LatLng(photo.getLat(), photo.getLng());
        myMarkerOptions.position(borderLocation);
        myMarkerOptions.title(photo.getId() + " - " + photo.getTaskId());
        Marker marker = photoMarkerCollection.addMarker(myMarkerOptions);
        marker.setTag(photo);
        markers.put(photo.getId(), marker);
    }

    private void addMarkerAzim(Photo photo) {
        Double azim = photo.getPhotoHeading();
        if (azim == null) {
            return;
        }
        MarkerOptions myMarkerOptions = new MarkerOptions();
        myMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(createIconAzimBitmap()));
        LatLng borderLocation = new LatLng(photo.getLat(), photo.getLng());
        myMarkerOptions.position(borderLocation);
        myMarkerOptions.rotation(azim.floatValue());
        myMarkerOptions.flat(true);
        azimMarkerCollection.addMarker(myMarkerOptions);
    }

    private Bitmap createIconPhotoBitmap(Bitmap photoBitmap) {
        //Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo_map_frame_old);
        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo_map_frame);
        Bitmap overlay = Bitmap.createBitmap(ICON_PHOTO_WIDTH, ICON_PHOTO_HEIGHT, iconBitmap.getConfig());
        Canvas canvas = new Canvas(overlay);
        Matrix iconMatrix = new Matrix();
        iconMatrix.postScale((float) ICON_PHOTO_WIDTH / iconBitmap.getWidth(), (float) (ICON_PHOTO_HEIGHT) / iconBitmap.getHeight());

        Matrix photoMatrix = new Matrix();
        float photoContentWidth = ICON_PHOTO_CONTENT_WIDTH;
        float photoContentHeight = ICON_PHOTO_CONTENT_HEIGHT;
        photoMatrix.postScale((float) (photoContentWidth - 2 * ICON_PHOTO_BORDER_WIDTH + 2) / photoBitmap.getWidth(), (float) (photoContentHeight - ICON_PHOTO_BORDER_HEIGHT + 5) / photoBitmap.getHeight());
        Path path = PathParser.createPathFromPathData(getString(R.string.map_iconPhotoFramePath));
        RectF pathNewBounds = new RectF(0, 0, photoBitmap.getWidth(), photoBitmap.getHeight());
        RectF pathOrigBounds = new RectF();
        path.computeBounds(pathOrigBounds, true);
        Matrix pathMatrix = new Matrix();
        pathMatrix.setRectToRect(pathOrigBounds, pathNewBounds, Matrix.ScaleToFit.FILL);
        path.transform(pathMatrix);
        photoBitmap = Util.getCroppedBitmap(photoBitmap, path);

        photoMatrix.postTranslate(10, 11);
        canvas.drawBitmap(iconBitmap, iconMatrix, null);
        canvas.drawBitmap(photoBitmap, photoMatrix, null);
        return overlay;
    }

    private Bitmap createIconAzimBitmap() {
        Bitmap azimBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_azimuth);
        Matrix azimMatrix = new Matrix();
        azimMatrix.postScale((float) (ICON_PHOTO_WIDTH) / azimBitmap.getWidth(), (float) (ICON_PHOTO_WIDTH) / azimBitmap.getHeight());
        return Bitmap.createBitmap(azimBitmap, 0, 0, azimBitmap.getWidth(), azimBitmap.getHeight(), azimMatrix, false);
    }

    private void initPhotos() {
        Map<Long, Photo> photos = null;
        TextView subtitleTextView = findViewById(R.id.map_textView_infoCurPSSubtitle);
        if (startMode.equals(START_MODE.TASK_PHOTOS)) {
            photos = PhotoList.getMapWithIds(PhotoList.createFromAppDatabaseByTaskGroup(MS.getAppDatabase(), taskId, this));
            subtitleTextView.setText(R.string.map_infoCurPSSubtitleTask);
            TableRow taskIdTableRow = findViewById(R.id.map_tableRow_infoCurPSTaskId);
            taskIdTableRow.setVisibility(View.VISIBLE);
            TextView taskIdTextView = findViewById(R.id.map_textView_infoCurPSTaskId);
            taskIdTextView.setText(taskId);
        } else if (startMode.equals(START_MODE.UNOWNED_PHOTOS)) {
            photos = PhotoList.getMapWithIds(PhotoList.createFromAppDatabaseByTaskGroup(MS.getAppDatabase(), null, this));
            subtitleTextView.setText(R.string.map_infoCurPSSubtitleUnowned);
        } else if (startMode.equals(START_MODE.ALL_PHOTOS)) {
            photos = PhotoList.getMapWithIds(PhotoList.createFromAppDatabaseUserPhoto(MS.getAppDatabase(), this));
            subtitleTextView.setText(R.string.map_infoCurPSSubtitleAll);
        }

        Map<Long, Photo> photosFiltered = new HashMap<>();
        for (Map.Entry<Long, Photo> entry : photos.entrySet()) {
            Photo photo = entry.getValue();
            if (photo.getLng() != null && photo.getLat() != null) {
                photosFiltered.put(photo.getId(), photo);
            }
        }
        photoMap = photosFiltered;

        TextView countTextView = findViewById(R.id.map_textView_infoCurPSCount);
        countTextView.setText(String.valueOf(photosFiltered.size()));
    }

    private void afterInitPhotos() {
        ProgressBar currentPhotosProgressBar = findViewById(R.id.map_progressBar_photosLoad);
        currentPhotosProgressBar.setVisibility(View.GONE);
        FrameLayout currentPhotosFrameLayout = findViewById(R.id.map_frameLayout_currentPhotos);
        currentPhotosFrameLayout.setVisibility(View.VISIBLE);
    }

    public void recordTracking(View view) {
        ptManager.isTrackingDisposable(new PTService.IsTrackingDisposable() {
            @Override
            public void onIsTrackingDisposable(boolean isTracking) {
                if (!isActive) {
                    return;
                }
                if (isTracking) {
                    MyAlertDialog.Builder builder = new MyAlertDialog.Builder(MapActivity.this);
                    MyAlertDialog myAlertDialog = builder.build();
                    myAlertDialog.setMessage(getString(R.string.pt_dialogStopMessage));
                    myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.pt_dialogStopButton), (dialog, which) -> {
                        ptManager.stopTracking();
                    });
                    myAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), (dialog, which) -> {
                        // nothing
                    });
                    myAlertDialog.show();
                } else {
                    MyAlertDialog.Builder builder = new MyAlertDialog.Builder(MapActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_new_track, null);
                    MyAlertDialog myAlertDialog = builder.build();
                    myAlertDialog.setCustomView(view);
                    TextView centroidsNoteTextView = view.findViewById(R.id.pt_textView_centroidsNote);

                    /*PTService - P1*/
                    /*
                    if (PersistData.getPhotoWithCentroiLocation(MapActivity.this)) {
                        centroidsNoteTextView.setVisibility(View.VISIBLE);
                    } else {
                        centroidsNoteTextView.setVisibility(View.GONE);
                    }
                    */
                    centroidsNoteTextView.setVisibility(View.GONE);
                    /**/

                    myAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.pt_dialogStartButton), (dialog, which) -> {
                        TextInputEditText textInputEditText = myAlertDialog.getAlertDialog().findViewById(R.id.pt_textInputEditText_name);
                        String name = textInputEditText.getText().toString();
                        ptManager.startTracking(name == null || name.isEmpty() ? getString(R.string.pt_defaultPathName) : name);
                    });
                    myAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), (dialog, which) -> {

                    });
                    myAlertDialog.getAlertDialog().setCancelable(false);
                    myAlertDialog.show();
                }
            }
        });
    }

    public void pauseTracking(View view) {
        if (ptManager == null) {
            return;
        }
        Boolean isPause = ptManager.isPause();
        if (isPause != null) {
            ptManager.setPause(!isPause);
        }
    }

    public void addCurrentPoint(View view) {
        if (!isMapInitialized || ptManager == null) {
            return;
        }

        ptManager.forceAddCurrentPoint();
    }

    public void addCentroidPoint(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        if (!isMapInitialized
                || ptManager == null || chDrawer == null || chDrawer.getLastCentroid() == null) {
            alert(getString(R.string.pt_errorNoCentroidPointTitle), getString(R.string.pt_errorNoCentroidPointText));
            return;
        }

        ptManager.addLocationManually(chDrawer.getLastCentroid().toLocation());
    }

    public void deletePointDialog(View view) {
        if (ptManager == null) {
            return;
        }
        PTPath ptPath = ptManager.getCurrentPath();
        if (ptPath == null) {
            return;
        }
        PTPoint ptPoint;
        if (ptPath.getPoints().size() == 0) {
            ptOnNoPointToDelete();
            return;
        } else {
            ptPoint = ptPath.getPoints().get(ptPath.getPoints().size() - 1);
        }

        MyAlertDialog.Builder builder = new MyAlertDialog.Builder(this);
        MyAlertDialog myAlertDialog = builder.build();
        myAlertDialog.setTitle(getString(R.string.pt_dialogDeleteLastPointTitle));
        myAlertDialog.setMessage(getString(R.string.pt_dialogDeleteLastPointText));
        myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.pt_dialogDeleteLastPointButton), (dialog, which) -> {
            ptManager.deletePoint(ptPoint);
        });
        myAlertDialog.setButton((AlertDialog.BUTTON_NEGATIVE), getString(R.string.dl_Cancel), (dialog, which) -> {
            // nothing
        });
        myAlertDialog.show();
    }

    private void adjustTrackingUI(boolean isTracking) {
        ProgressBar progressBar = findViewById(R.id.map_progressBar_trackLoad);
        progressBar.setVisibility(View.GONE);
        LinearLayout loadTrackLinearLayout = findViewById(R.id.map_linearLayout_trackLoadControls);
        loadTrackLinearLayout.setVisibility(View.VISIBLE);
        Button recImageButton = findViewById(R.id.map_button_rec);
        recImageButton.setText(isTracking ? getString(R.string.pt_stopButton) : getString(R.string.pt_recButton));
        Button modeButton = findViewById(R.id.map_button_mode);
        modeButton.setVisibility(isTracking ? View.VISIBLE : View.GONE);
        Boolean isPause = ptManager.isPause();
        adjustModeControls(isPause == null ? false : isPause, isTracking);
        adjustTrackSendButton();
    }

    private void adjustTrackSendButton() {
        if (!isCreated || ptManager == null) {
            return;
        }
        ptManager.isTrackingDisposable(isTracking -> {
            Button button = findViewById(R.id.map_button_trackSend);
            if (!isTracking && ptManager.getDrawnPath() != null && !ptManager.getDrawnPath().isSent()) {
                button.setVisibility(View.VISIBLE);
            } else {
                button.setVisibility(View.GONE);
            }
        });
    }

    private void adjustModeControls(boolean isVertex, boolean isTracking) {
        Button pauseButton = findViewById(R.id.map_button_mode);
        pauseButton.setText(isVertex ? R.string.pt_continueButton : R.string.pt_pauseButton);
        LinearLayout vertexLinearLayout = findViewById(R.id.map_linearLayout_vertexControl);
        vertexLinearLayout.setVisibility(isVertex && isTracking ? View.VISIBLE : View.GONE);
    }

    public void gotToPathTrackingOverview(View view) {
        Intent intent = new Intent(this, PathTrackingOverviewActivity.class);
        startActivity(intent);
    }

    private boolean isCameraDelayElapsed() {
        if (lastCameraTrackTimestamp == null) {
            return true;
        }
        return DateTime.now().toDate().getTime() - lastCameraTrackTimestamp.toDate().getTime() >= CAMERA_ANIMATION_DELAY_MILS;
    }

    private void adjustPathTrackingCameraLocation(Location location) {
        if (location == null) {
            return;
        }
        if (ptManager == null) {
            return;
        }
        if (!PersistData.getAutoPan(this)) {
            return;
        }
        if (!isCameraDelayElapsed()) {
            return;
        }
        if (isCameraTrackAnimating) {
            return;
        }
        ptManager.isTrackingDisposable(isTracking -> {
            if (isTracking /*&& ptManager.isPause() != null && !ptManager.isPause()*/) {
                float zoom = mMap.getCameraPosition().zoom;
                zoom = zoom < CAMERA_MIN_ZOOM ? CAMERA_MIN_ZOOM : zoom;
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                        .zoom(zoom)
                        .bearing(mMap.getCameraPosition().bearing)
                        .tilt(mMap.getCameraPosition().tilt)
                        .build();
                isCameraTrackAnimating = true;
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), CAMERA_ANIMATION_TRACK_DURATION, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        isCameraTrackAnimating = false;
                    }

                    @Override
                    public void onCancel() {
                        isCameraTrackAnimating = false;
                    }
                });
            }
        });
    }

    private void createCameraTrackDelay() {
        lastCameraTrackTimestamp = DateTime.now();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ptManager != null) {
            ptManager.onDestroy();
        }
        if (chDrawer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            chDrawer.onDestroy();

        }
        if (flManager != null) {
            flManager.stop();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getAction() != null
                && intent.getAction().equals(INTENT_ACTION_SHOW_TRACK) && intent.hasExtra(INTENT_ACTION_SHOW_TRACK_ID) && serviceController.isServiceInitialized()) {
            Long pathId = intent.getLongExtra(INTENT_ACTION_SHOW_TRACK_ID, -1);
            if (pathId > -1) {
                drawPath(PTPath.createFromAppDatabase(MS.getAppDatabase(), pathId));
            }
        }
    }

    private void drawPath(PTPath ptPath) {
        ptManager.isTrackingDisposable(new PTService.IsTrackingDisposable() {
            @Override
            public void onIsTrackingDisposable(boolean isTracking) {
                if (isTracking) {
                    alert(getString(R.string.pt_cannotDrawWhileRecTitle), getString(R.string.pt_cannotDrawWhileRecText));
                } else {
                    ptManager.drawPathPolygon(ptPath);
                }
            }
        });
        adjustTrackSendButton();
    }

    public void uploadPathDialog(View view) {
        if (!serviceController.isServiceInitialized()) {
            return;
        }
        if (MS.isPathsUploading()) {
            return;
        }
        if (ptManager == null) {
            return;
        }
        if (ptManager.getDrawnPath() == null) {
            return;
        }

        MyAlertDialog.Builder builder = new MyAlertDialog.Builder(this);
        MyAlertDialog myAlertDialog = builder.build();
        myAlertDialog.setTitle(getString(R.string.pt_uploadDialogPathTitle));
        myAlertDialog.setMessage(getString(R.string.pt_uploadDialogPathText));
        myAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), (dialog, which) -> {
            // nothing
        });
        myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dl_OK), (dialog, which) -> {
            ptManager.uploadDrawnPath();
        });
        myAlertDialog.show();
    }

    private void adjustLocationChipSet(Location location) {
        TextView latTextView = findViewById(R.id.map_textView_latChipSet);
        TextView lngTextView = findViewById(R.id.map_textView_lngChipSet);
        TextView accuracyTextView = findViewById(R.id.map_textView_accuracy);

        if (location == null) {
            latTextView.setText(getString(R.string.map_basicInfoUnavailable));
            lngTextView.setText(getString(R.string.map_basicInfoUnavailable));
            accuracyTextView.setText(R.string.map_basicInfoUnavailable);
        } else {
            DecimalFormat coorDecimalFormat = Util.createPrettyCoordinateFormat();
            DecimalFormat accDecimalFormat = new DecimalFormat("#");
            latTextView.setText(coorDecimalFormat.format(location.getLatitude()));
            lngTextView.setText(coorDecimalFormat.format(location.getLongitude()));
            accuracyTextView.setText(accDecimalFormat.format(location.getAccuracy()) + " " + getString(R.string.map_accuracyUnit));
        }
    }

    @Override
    public GoogleMap getMap() {
        return mMap;
    }

    @Override
    public void alert(String title, String text) {
        super.alert(title, text);
    }

    @Override
    public Requestor getRequestor() {
        return MS.getRequestor();
    }

    @Override
    public PolygonManager getPolygonManager() {
        return polygonManager;
    }

    @Override
    public MarkerManager getMarkerManager() {
        return markerManager;
    }

    @Override
    public PolylineManager getPolylineManager() {
        return polylineManager;
    }

    @Override
    public FrameLayout getInfoCurrentPathLayout() {
        return findViewById(R.id.pt_frameLayout_currentTrack);
    }

    @Override
    public PTIsPathsUploadingBinder getPtIsPathsUploadingBinder() {
        return new PTIsPathsUploadingBinderImpl(MS);
    }

    @Override
    public void uploadDrawnPathStarted() {
        Toast.makeText(getApplicationContext(), R.string.pt_uploadPathStarted, Toast.LENGTH_LONG).show();
    }

    @Override
    public void uploadDrawnPathSuccess() {
        Toast.makeText(getApplicationContext(), R.string.pt_uploadPathCompleted, Toast.LENGTH_LONG).show();
    }

    @Override
    public void uploadDrawnPathFailed(String errMsg) {
        Toast.makeText(getApplicationContext(), R.string.pt_uploadPathFailed, Toast.LENGTH_LONG).show();
        if (isCreated) {
            alert(getString(R.string.pt_errorUploadPathTitle), getString(R.string.pt_errorUploadPathText, errMsg));
        }
    }

    @Override
    public void uploadDrawnPathComplete() {
        adjustTrackSendButton();
    }

    @Override
    public void requestAnimatePTOnPoint(CameraUpdate cameraUpdate) {
        if (!PersistData.getAutoPan(this)) {
            mMap.animateCamera(cameraUpdate);
        }
    }

    @Override
    public void requestAnimatePTOnPath(CameraUpdate cameraUpdate) {
        createCameraTrackDelay();
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public AppCompatActivity getAppCompatActivity() {
        return this;
    }

    @Override
    public void onNewFusedLocations(LocationResult locationResult) {
        Location location = locationResult.getLastLocation();
        if (ptManager != null) {
            ptManager.isTrackingDisposable(isTracking -> {
                if (!isTracking) {
                    adjustLocationChipSet(location);
                }
            });
        } else {
            adjustLocationChipSet(location);
        }
        if (startMode.equals(START_MODE.PATH_TRACKING)) {
            adjustPathTrackingCameraLocation(location);
        }
    }

    @Override
    public void onFLStarted() {
        // nothing
    }

    @Override
    public void onFLEnded() {
        adjustLocationChipSet(null);
    }

    @Override
    public TextView getSampleCountValue() {
        return findViewById(R.id.map_textView_sampleCount);
    }

    @Override
    public void onNewCentroidCH(CHService.Centroid centroid) {
        TextView latTextView = findViewById(R.id.map_textView_latCentroid);
        TextView lngTextView = findViewById(R.id.map_textView_lngCentroid);
        DecimalFormat decimalFormat = Util.createPrettyCoordinateFormat();

        if (centroid == null) {
            latTextView.setText(R.string.map_basicInfoUnavailable);
            lngTextView.setText(R.string.map_basicInfoUnavailable);
        } else {
            latTextView.setText(decimalFormat.format(centroid.latitude));
            lngTextView.setText(decimalFormat.format(centroid.longitude));
        }
    }

    @Override
    public GoogleMap getGoogleMap() {
        return mMap;
    }

    @Override
    public AppDatabase getAppDatabase() {
        return MS.getAppDatabase();
    }

    @Override
    public void startingPTException(Exception e) {
        alert(getString(R.string.pt_startingExceptionTitle), getString(R.string.pt_startingExceptionText, e.getMessage()));
    }

    @Override
    public void stoppingPTException(Exception e) {
        alert(getString(R.string.pt_stoppingExceptionTitle), getString(R.string.pt_stoppingExceptionText, e.getMessage()));
    }

    @Override
    public void onStartedPT() {
        MS.setKeepScreenOnActive(true);
        adjustTrackingUI(true);
    }

    @Override
    public void onStoppedPT(PTPath ptPath) {
        MS.setKeepScreenOnActive(false);
        if (ptPath != null) {
            ptManager.drawPathPolygon(ptPath);
        }
        adjustTrackingUI(false);
    }

    @Override
    public void onPausePT() {
        adjustModeControls(true, true);
    }

    @Override
    public void onContinuePT() {
        adjustModeControls(false, true);
    }

    @Override
    public void onNewUnfilteredLocationPT(Location location) {
        adjustLocationChipSet(location);
    }

    @Override
    public void onNoPointsInPath() {
        alert(getString(R.string.pt_noPointsTitle), getString(R.string.pt_noPointsText));
    }

    @Override
    public void ptOnNoPointToAdd() {
        alert(getString(R.string.pt_errorNoCapturePointTitle), getString(R.string.pt_errorNoCapturePointText));
    }

    @Override
    public void ptOnNoPointToDelete() {
        alert(getString(R.string.pt_errorNoPointToDeleteTitle), getString(R.string.pt_errorNoPointToDeleteText));
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
