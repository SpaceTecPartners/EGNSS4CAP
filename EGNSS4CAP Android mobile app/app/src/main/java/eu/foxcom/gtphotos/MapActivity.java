package eu.foxcom.gtphotos;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;
import com.google.maps.android.collections.PolylineManager;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.foxcom.gtphotos.model.AppDatabase;
import eu.foxcom.gtphotos.model.Photo;
import eu.foxcom.gtphotos.model.PhotoList;
import eu.foxcom.gtphotos.model.Requestor;
import eu.foxcom.gtphotos.model.convexHullUtil.CHDrawer;
import eu.foxcom.gtphotos.model.convexHullUtil.CHMapActivity;
import eu.foxcom.gtphotos.model.groundGeometry.GGManager;
import eu.foxcom.gtphotos.model.groundGeometry.GGMapActivity;
import eu.foxcom.gtphotos.model.pathTrack.PTManager;
import eu.foxcom.gtphotos.model.pathTrack.PTMapActivity;
import eu.foxcom.gtphotos.model.pathTrack.PTPath;
import eu.foxcom.gtphotos.model.pathTrack.PTService;

public class MapActivity extends BaseActivity implements OnMapReadyCallback, GGMapActivity, PTMapActivity, CHMapActivity {

    public static final String INTENT_ACTION_SHOW_TRACK = "INTENT_ACTION_SHOW_TRACK";
    public static final String INTENT_ACTION_SHOW_TRACK_ID = "INTENT_ACTION_SHOW_TRACK_ID";

    private static final int ICON_PHOTO_WIDTH = 200;
    private static final int ICON_PHOTO_BORDER_WIDTH = 6;
    private static final int CAMERA_ANIMATION_DURATION_MILS = 1000;
    private static final int BOUND_SPACE_PADDING = 350;
    private static final double MIN_BOUND_DEGREE = 0.001;

    private Map<Long, Photo> photoMap = new HashMap<>();
    private GoogleMap mMap;
    private boolean isMapLoaded = false;
    // <photoId, marker>
    private Map<Long, Marker> markers = new HashMap<>();
    private DecimalFormat decimalFormat;
    private PolygonManager polygonManager;
    private MarkerManager markerManager;
    private PolylineManager polylineManager;
    private MarkerManager.Collection photoMarkerCollection;
    private MarkerManager.Collection azimMarkerCollection;
    private GGManager ggManager;
    private PTManager ptManager;
    private CHDrawer chDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));

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
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                isMapLoaded = true;
                initMap();
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            alert(getString(R.string.map_failedCurrentLocationTitle), getString(R.string.map_failedCurrentLocationText));
        } else {
            mMap.setMyLocationEnabled(true);
        }
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    private void initManagers() {
        polygonManager = new PolygonManager(mMap);
        markerManager = new MarkerManager(mMap);
        polylineManager = new PolylineManager(mMap);
    }

    private void initMap() {
        initManagers();
        initPhotos();
        initMarkers();
        initMoveCamera();
        initGroundGeometry();
        initPathTracking();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            initConvexHullDrawer();
        }
    }

    private void initMarkers() {
        azimMarkerCollection = markerManager.newCollection();
        photoMarkerCollection = markerManager.newCollection();
        for (Map.Entry<Long, Photo> entry : photoMap.entrySet()) {
            addMarkerAzim(entry.getValue());
            addMarkerPhoto(entry.getValue());
        }
    }

    private void initMoveCamera() {
        moveCameraToAllPhotos();
    }

    private void initGroundGeometry() {
        ggManager = new GGManager(this);
    }

    private void initPathTracking() {
        ptManager = new PTManager(this, MS.getAppDatabase());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initConvexHullDrawer() {
        chDrawer = new CHDrawer(this);
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
                    altS = decimalFormat.format(alt);
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
        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_photo);
        Bitmap overlay = Bitmap.createBitmap(ICON_PHOTO_WIDTH, 2 * ICON_PHOTO_WIDTH, iconBitmap.getConfig());
        Canvas canvas = new Canvas(overlay);
        Matrix iconMatrix = new Matrix();
        iconMatrix.postScale((float) ICON_PHOTO_WIDTH / iconBitmap.getWidth(), (float) (2 * ICON_PHOTO_WIDTH) / iconBitmap.getHeight());
        Matrix photoMatrix = new Matrix();
        float photoWidth = ICON_PHOTO_WIDTH;
        float photoHeight = ICON_PHOTO_WIDTH;
        photoMatrix.postScale((float) (photoWidth - 2 * ICON_PHOTO_BORDER_WIDTH) / photoBitmap.getWidth(), (float) (photoHeight - 2 * ICON_PHOTO_BORDER_WIDTH) / photoBitmap.getHeight());
        photoMatrix.postTranslate(ICON_PHOTO_BORDER_WIDTH, ICON_PHOTO_BORDER_WIDTH);
        canvas.drawBitmap(iconBitmap, iconMatrix, null);
        canvas.drawBitmap(photoBitmap, photoMatrix, null);
        return overlay;
    }

    private Bitmap createIconAzimBitmap() {
        Bitmap azimBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_azimuth);
        Matrix azimMatrix = new Matrix();
        azimMatrix.postScale((float) (ICON_PHOTO_WIDTH - 10) / azimBitmap.getWidth(), (float) (ICON_PHOTO_WIDTH - 10) / azimBitmap.getHeight());
        return Bitmap.createBitmap(azimBitmap, 0, 0, azimBitmap.getWidth(), azimBitmap.getHeight(), azimMatrix, false);
    }

    private void initPhotos() {
        Map<Long, Photo> photos = PhotoList.createFromAppDatabaseByUser(MS.getAppDatabase());
        Map<Long, Photo> photosFiltered = new HashMap<>();
        for (Map.Entry<Long, Photo> entry : photos.entrySet()) {
            Photo photo = entry.getValue();
            if (photo.getLng() != null && photo.getLat() != null) {
                photosFiltered.put(photo.getId(), photo);
            }
        }
        photoMap = photosFiltered;
    }

    public void recordTracking(View view) {
        ptManager.isTrackingDisposable(new PTService.IsTrackingDisposable() {
            @Override
            public void onIsTrackingDisposable(boolean isTracking) {
                if (isTracking) {
                    ptManager.stopTracking();
                } else {
                    if (!isActive) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_new_track, null);
                    builder.setView(view);
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


                    AlertDialog alertDialog = builder.create();
                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dl_OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TextInputEditText textInputEditText = alertDialog.findViewById(R.id.pt_textInputEditText_name);
                            String name = textInputEditText.getText().toString();
                            ptManager.startTracking(name == null || name.isEmpty() ? getString(R.string.pt_defaultPathName) : name);
                        }
                    });
                    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                }
            }
        });
    }

    private void adjustTrackingUI(boolean isTracking) {
        ProgressBar progressBar = findViewById(R.id.map_progressBar_trackLoad);
        progressBar.setVisibility(View.GONE);
        ImageButton recImageButton = findViewById(R.id.map_imageButton_rec);
        recImageButton.setVisibility(View.VISIBLE);
        recImageButton.setImageDrawable(getDrawable(isTracking ? R.drawable.icon_stop_act : R.drawable.icon_record_act));
    }

    public void gotToPathTrackingOverview(View view) {
        Intent intent = new Intent(this, PathTrackingOverviewActivity.class);
        startActivity(intent);
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
    public AppCompatActivity getAppCompatActivity() {
        return this;
    }

    @Override
    public TextView getSampleCount() {
        return findViewById(R.id.map_textView_sampleCount);
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
        adjustTrackingUI(true);
    }

    @Override
    public void onStoppedPT(PTPath ptPath) {
        if (ptPath != null) {
            ptManager.drawPathPolygon(ptPath);
        }
        adjustTrackingUI(false);
    }

    @Override
    public void onNoPointsInPath() {
        alert(getString(R.string.pt_noPointsTitle), getString(R.string.pt_noPointsText));
    }
}
