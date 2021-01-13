package eu.foxcom.gtphotos.model.mock;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import eu.foxcom.gtphotos.R;

public class LocationMock extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest2;
    private LocationCallback locationCallback2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_mock);

        int t1 = 1000;
        int t2 = 5000;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(t1);
        locationRequest.setFastestInterval(t1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                TextView t1TextView = findViewById(R.id.textView_t1);
                Location location = locationResult.getLastLocation();
                String text = "null";
                if (location != null) {
                    text = location.getLatitude() + "; " + location.getLongitude();
                }
                t1TextView.setText(text);
            }
        };

        locationRequest2 = LocationRequest.create();
        locationRequest2.setInterval(t2);
        locationRequest2.setFastestInterval(t2);
        locationRequest2.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback2 = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                TextView t2TextView = findViewById(R.id.textView_t2);
                Location location = locationResult.getLastLocation();
                String text = "null";
                if (location != null) {
                    text = location.getLatitude() + "; " + location.getLongitude();
                }
                t2TextView.setText(text);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest2, locationCallback2, getMainLooper());

        TextView removedTextView = findViewById(R.id.textView_removeR);
        TextView removedFinishedTextView = findViewById(R.id.textView_removeFinished);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Task task = fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        task.addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                removedTextView.setText("SuCCESS");
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                removedTextView.setText(e.getMessage());
            }
        });
        task.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                removedFinishedTextView.setText("COMPLETED");
            }
        });

        Task task2 = fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        task.addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                removedTextView.setText("SuCCESS");
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                removedTextView.setText(e.getMessage());
            }
        });
        task.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                removedFinishedTextView.setText("COMPLETED");
            }
        });
    }
}