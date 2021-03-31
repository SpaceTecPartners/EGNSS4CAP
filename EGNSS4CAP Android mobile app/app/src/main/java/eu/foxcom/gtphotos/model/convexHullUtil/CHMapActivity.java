package eu.foxcom.gtphotos.model.convexHullUtil;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.maps.android.collections.MarkerManager;

public interface CHMapActivity {
    public MarkerManager getMarkerManager();

    public AppCompatActivity getAppCompatActivity();

    public TextView getSampleCountValue();
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */