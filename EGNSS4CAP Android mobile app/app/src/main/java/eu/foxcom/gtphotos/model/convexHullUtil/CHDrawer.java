package eu.foxcom.gtphotos.model.convexHullUtil;

import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.collections.MarkerManager;

import eu.foxcom.gtphotos.R;
import eu.foxcom.gtphotos.ServiceController;
import eu.foxcom.gtphotos.ServiceGetter;
import eu.foxcom.gtphotos.ServiceInit;
import eu.foxcom.gtphotos.model.PersistData;

@RequiresApi(api = Build.VERSION_CODES.N)
public class CHDrawer implements ServiceInit {
    private Context context;
    private CHMapActivity chMapActivity;

    private MarkerManager.Collection markerCollection;
    private Marker markerCentroid;

    private ServiceController serviceController;
    private CHService chService;
    private boolean isDestroying = false;

    public CHDrawer(CHMapActivity chMapActivity) {
        this.context = (Context) chMapActivity;
        this.chMapActivity = chMapActivity;
        this.markerCollection = chMapActivity.getMarkerManager().newCollection();

        serviceController = new ServiceController(context, new ServiceGetter() {
            @Override
            public Service getService(IBinder binder) {
                return ((CHService.LocalBinder) binder).getService();
            }
        }, CHService.class);
        serviceController.setServiceInit(this);
        serviceController.startService();
    }
    
    @Override
    public void serviceInit() {
        chService = (CHService) serviceController.getService();
        if (isDestroying) {
            return;
        }
        chService.getLastCentroid().observe(chMapActivity.getAppCompatActivity(), centroid -> {
            if (centroid == null) {
                return;
            }
            drawMarker(centroid);
        });
        chService.getSampleCount().observe(chMapActivity.getAppCompatActivity(), integer -> {
            drawSampleCount(integer);
        });
    }

    private void drawMarker(CHService.Centroid centroid) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(centroid.latitude, centroid.longitude));
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_ch_location));
        if (markerCentroid != null) {
            markerCentroid.remove();
        }
        markerCentroid = markerCollection.addMarker(markerOptions);
    }

    private void drawSampleCount(int count) {
        TextView countTextView = chMapActivity.getSampleCountValue();
        countTextView.setText(context.getString(R.string.map_sampleCountValue, String.valueOf(count), String.valueOf(PersistData.getSamplingNumber(context))));
    }

    public void onDestroy() {
        isDestroying = true;
        if (chService != null) {
            serviceController.stopService();
        }
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
