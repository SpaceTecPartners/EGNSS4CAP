package eu.foxcom.stp.gsa.egnss4cap;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import eu.foxcom.stp.gsa.egnss4cap.model.MyAlertDialog;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;
import eu.foxcom.stp.gsa.egnss4cap.model.pathTrack.PTPath;

public class PathTrackingOverviewActivity extends BaseActivity {

    class PathTrackingAdapter extends RecyclerView.Adapter<PathTrackingAdapter.PathTrackingHolder> {

        class PathTrackingHolder extends RecyclerView.ViewHolder {

            ConstraintLayout mainLayout;
            TextView id;
            TextView name;
            TextView sent;
            TextView start;
            TextView end;
            TextView area;
            TextView byCentroids;
            ImageButton deleteButton;

            public PathTrackingHolder(@NonNull View itemView) {
                super(itemView);
                mainLayout = itemView.findViewById(R.id.pt_constraintLayout_mainLayout);
                id = itemView.findViewById(R.id.pt_textView_id);
                name = itemView.findViewById(R.id.pt_textView_name);
                sent = itemView.findViewById(R.id.pt_textView_sent);
                start = itemView.findViewById(R.id.pt_textView_start);
                end = itemView.findViewById(R.id.pt_textView_end);
                area = itemView.findViewById(R.id.pt_textView_area);
                byCentroids = itemView.findViewById(R.id.pt_textView_byCentroids);
                deleteButton = itemView.findViewById(R.id.pt_imageButton_delete);
            }
        }

        List<PTPath> ptPaths;
        DateTimeFormatter dateTimeFormatter = Util.createPrettyDateTimeFormat();

        PathTrackingAdapter(List<PTPath> ptPaths) {
            this.ptPaths = ptPaths;
        }

        @NonNull
        @Override
        public PathTrackingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.item_track, parent, false);
            PathTrackingHolder holder = new PathTrackingHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull PathTrackingHolder holder, int position) {
            PTPath ptPath = ptPaths.get(position);
            if (ptPath.isSent()) {
                holder.id.setText(String.valueOf(ptPath.getRealId()));
                holder.id.setTypeface(holder.id.getTypeface(), Typeface.NORMAL);
            } else {
                holder.id.setText(getString(R.string.pt_notSent));
                holder.id.setTypeface(holder.id.getTypeface(), Typeface.ITALIC);
            }
            holder.name.setText(ptPath.getName());
            holder.sent.setText(ptPath.isLocked() ? getString(R.string.gn_no) + " (" + getString(R.string.pt_lockState) + ")" : (ptPath.isSent() ? getString(R.string.gn_yes) : getString(R.string.gn_no)));
            holder.start.setText(ptPath.getStartT().toString(dateTimeFormatter));
            holder.end.setText(ptPath.getEndT().toString(dateTimeFormatter));
            holder.area.setText(ptPath.getArea() == null ? "0" : PTPath.AREA_FORMAT.format(ptPath.getArea()));
            holder.byCentroids.setText(ptPath.isByCentroids() ? getString(R.string.gn_yes) : getString(R.string.gn_no));
            holder.mainLayout.setOnClickListener(v -> drawPath(ptPath.getAutoId()));
            if (ptPath.isSent() || ptPath.isLocked()) {
                holder.deleteButton.setVisibility(View.GONE);
            } else {
                holder.deleteButton.setVisibility(View.VISIBLE);
            }
            holder.deleteButton.setOnClickListener(v -> {
                if (ptPath.isSent() || ptPath.isLocked()) {
                    return;
                }
                MyAlertDialog.Builder builder = new MyAlertDialog.Builder(PathTrackingOverviewActivity.this);
                MyAlertDialog myAlertDialog = builder.build();
                myAlertDialog.setTitle(getString(R.string.pt_deleteDialogTitle));
                myAlertDialog.setMessage(getString(R.string.pt_deleteDialogText, ptPath.getName()));
                myAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), (dialog, which) -> {
                });
                myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dl_OK), (dialog, which) -> {
                    deletePath(holder, ptPath.getAutoId());
                });
                myAlertDialog.show();
            });
        }

        @Override
        public int getItemCount() {
            return ptPaths.size();
        }
    }


    private RecyclerView recyclerView;
    private PathTrackingAdapter pathTrackingAdapter;
    private List<PTPath> ptPaths;
    private List<String> uploadPathsErrs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_tracking_overview);
        setToolbar(R.id.toolbar);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        recyclerView = findViewById(R.id.pt_recycleView_paths);
        ptPaths = loadPaths();
        pathTrackingAdapter = new PathTrackingAdapter(ptPaths);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(pathTrackingAdapter);
    }

    private void refreshPathTrackingAdapter() {
        ptPaths.clear();
        ptPaths.addAll(loadPaths());
        pathTrackingAdapter.notifyDataSetChanged();
    }

    private List<PTPath> loadPaths() {
        List<PTPath> ptPaths = PTPath.createListFromAppDatabase(MS.getAppDatabase(), false);
        for (PTPath ptPath : ptPaths) {
            if (ptPath.getArea() == null && !ptPath.isPointsLoaded()) {
                ptPath.loadPoints(MS.getAppDatabase());
                ptPath.calculateArea();
                ptPath.saveToDB(MS.getAppDatabase());
            }
        }
        return ptPaths;
    }

    private void drawPath(Long pathId) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.setAction(MapActivity.INTENT_ACTION_SHOW_TRACK);
        intent.putExtra(MapActivity.INTENT_ACTION_SHOW_TRACK_ID, pathId);
        startActivity(intent);
    }

    public void uploadAllPathsDialog(View view) {
        if (!serviceController.isServiceInitialized()) {
            return;
        }
        if (MS.isPathsUploading()) {
            return;
        }
        List<PTPath> ptPaths = PTPath.createListFromAppDatabaseUnsent(MS.getAppDatabase());
        if (ptPaths.size() == 0) {
            alert(getString(R.string.pt_alertNothingToUploadTitle), getString(R.string.pt_alertNothingToUploadText));
            return;
        }

        MyAlertDialog.Builder builder = new MyAlertDialog.Builder(this);
        MyAlertDialog myAlertDialog = builder.build();
        myAlertDialog.setTitle(getString(R.string.pt_uploadDialogPathsTitle));
        myAlertDialog.setMessage(getString(R.string.pt_uploadDialogPathsText, String.valueOf(ptPaths.size())));
        myAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), (dialog, which) -> {
            // nothing
        });
        myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dl_OK), (dialog, which) -> {
            uploadAllPaths(ptPaths);
        });
        myAlertDialog.show();
    }

    private void uploadAllPaths(List<PTPath> ptPaths) {
        if (MS.isPathsUploading()) {
            return;
        } else {
            MS.setPathsUploading(true);
        }
        Toast.makeText(getApplicationContext(), getString(R.string.pt_uploadPathsStarted), Toast.LENGTH_LONG).show();
        Phaser phaser = new Phaser(ptPaths.size() + 1);
        PTPath.UploadReceiver uploadReceiver = new PTPath.UploadReceiver() {
            @Override
            protected void success() {
                // nothing
            }

            @Override
            protected void failed(String errMsg) {
                if (isCreated) {
                    uploadPathsErrs.add(errMsg);
                }
            }

            @Override
            protected void complete() {
                phaser.arriveAndDeregister();
            }
        };
        uploadPathsErrs.clear();
        for (PTPath ptPath : ptPaths) {
            ptPath.upload(MS.getAppDatabase(), MS.getRequestor(), uploadReceiver);
        }
        Handler joiner = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().submit(() -> {
            phaser.awaitAdvance(phaser.arriveAndDeregister());
            joiner.post(() -> {
                uploadAllPathsComplete(MS);
            });
        });
    }

    private void uploadAllPathsComplete(MainService mainService) {
        if (isCreated) {
            if (uploadPathsErrs.size() > 0) {
                // failed
                String errs = "";
                /* DEBUGCOM
                uploadPathsErrs.add("pokus chyba 1");
                uploadPathsErrs.add("pokus chyba 2");
                /**/
                for (String err : uploadPathsErrs) {
                    errs += err + "\n\n";
                }
                alert(getString(R.string.pt_errorUploadPathsTitle), getString(R.string.pt_errorUploadPathsText, errs));
                Toast.makeText(getApplicationContext(), getString(R.string.pt_uploadPathsFailed), Toast.LENGTH_LONG).show();
            } else {
                // success
                Toast.makeText(getApplicationContext(), getString(R.string.pt_uploadPathsCompleted), Toast.LENGTH_LONG).show();
            }
            refreshPathTrackingAdapter();
        }
        mainService.setPathsUploading(false);
    }

    private void deletePath(PathTrackingAdapter.PathTrackingHolder holder, Long pathId) {
        if (!serviceController.isServiceInitialized()) {
            return;
        }
        if (MS.isPathsUploading()) {
            return;
        }
        PTPath ptPath = PTPath.createFromAppDatabase(MS.getAppDatabase(), pathId);
        if (ptPath.isSent()) {
            return;
        }
        int actualPosition = holder.getAdapterPosition();
        ptPath.delete(MS.getAppDatabase());
        ptPaths.remove(actualPosition);
        pathTrackingAdapter.notifyItemRemoved(actualPosition);
        pathTrackingAdapter.notifyItemRangeChanged(actualPosition, ptPaths.size());
    }
}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */