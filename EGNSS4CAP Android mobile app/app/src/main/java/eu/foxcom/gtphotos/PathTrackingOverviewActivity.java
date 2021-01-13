package eu.foxcom.gtphotos;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import eu.foxcom.gtphotos.model.Util;
import eu.foxcom.gtphotos.model.pathTrack.PTPath;

public class PathTrackingOverviewActivity extends BaseActivity {

    class PathTrackingAdapter extends RecyclerView.Adapter<PathTrackingAdapter.PathTrackingHolder> {

        class PathTrackingHolder extends RecyclerView.ViewHolder {

            ConstraintLayout mainLayout;
            TextView id;
            TextView name;
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
                start = itemView.findViewById(R.id.pt_textView_start);
                end = itemView.findViewById(R.id.pt_textView_end);
                area = itemView.findViewById(R.id.pt_textView_area);
                byCentroids = itemView.findViewById(R.id.pt_textView_byCentroids);
                deleteButton = itemView.findViewById(R.id.pt_imageButton_delete);
            }
        }

        List<PTPath> ptPaths;
        DateTimeFormatter dateTimeFormatter = Util.createPrettyDateTimeFormat();

        PathTrackingAdapter (List<PTPath> ptPaths) {
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
            Long realId = ptPath.getRealId();
            if (ptPath.isSent()) {
                holder.id.setText(getString(R.string.pt_notSent));
                holder.id.setTypeface(holder.id.getTypeface(), Typeface.ITALIC);
            } else {
                holder.id.setText(String.valueOf(ptPath.getRealId()));
                holder.id.setTypeface(holder.id.getTypeface(), Typeface.NORMAL);
            }
            holder.id.setText(realId == null ? getString(R.string.pt_notSent) : String.valueOf(realId));
            holder.name.setText(ptPath.getName());
            holder.start.setText(ptPath.getStartT().toString(dateTimeFormatter));
            holder.end.setText(ptPath.getEndT().toString(dateTimeFormatter));
            holder.area.setText(ptPath.getArea() == null ? "0" : PTPath.AREA_FORMAT.format(ptPath.getArea()));
            holder.byCentroids.setText(ptPath.isByCentroids() ? getString(R.string.gn_yes) : getString(R.string.gn_no));
            holder.mainLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawPath(ptPath.getAutoId());
                }
            });
            if (ptPath.isSent()) {
                holder.deleteButton.setVisibility(View.GONE);
            } else {
                holder.deleteButton.setVisibility(View.VISIBLE);
            }
            holder.deleteButton.setOnClickListener(v -> {
                if (ptPath.isSent()) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(PathTrackingOverviewActivity.this);
                builder.setTitle(R.string.pt_deleteDialogTitle);
                builder.setMessage(getString(R.string.pt_deleteDialogText, ptPath.getName()));
                builder.setNegativeButton(R.string.dl_Cancel, (dialog, which) -> {
                });
                builder.setPositiveButton(R.string.dl_OK, (dialog, which) -> {
                    int actualPosition = holder.getAdapterPosition();
                    ptPath.delete(MS.getAppDatabase());
                    ptPaths.remove(actualPosition);
                    notifyItemRemoved(actualPosition);
                    notifyItemRangeChanged(actualPosition, ptPaths.size());
                });
                builder.create().show();
            });
        }

        @Override
        public int getItemCount() {
            return ptPaths.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_tracking_overview);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        RecyclerView recyclerView = findViewById(R.id.pt_recycleView_paths);
        List<PTPath> ptPaths = PTPath.createListFromAppDatabase(MS.getAppDatabase());
        PathTrackingAdapter pathTrackingAdapter = new PathTrackingAdapter(ptPaths);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(pathTrackingAdapter);
    }

    private void drawPath(Long pathId) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.setAction(MapActivity.INTENT_ACTION_SHOW_TRACK);
        intent.putExtra(MapActivity.INTENT_ACTION_SHOW_TRACK_ID, pathId);
        startActivity(intent);
    }

    public void deletePath(View view) {
        // TODO

    }
}