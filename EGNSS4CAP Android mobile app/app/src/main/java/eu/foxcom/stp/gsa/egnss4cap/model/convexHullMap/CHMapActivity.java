package eu.foxcom.stp.gsa.egnss4cap.model.convexHullMap;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.maps.android.collections.MarkerManager;

public interface CHMapActivity {
    public MarkerManager getMarkerManager();

    public AppCompatActivity getAppCompatActivity();

    public TextView getSampleCountValue();

    public void onNewCentroidCH(CHService.Centroid centroid);
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */