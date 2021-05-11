package eu.foxcom.stp.gsa.egnss4cap;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.text.DecimalFormat;

import eu.foxcom.stp.gsa.egnss4cap.model.Photo;
import eu.foxcom.stp.gsa.egnss4cap.model.PhotoList;
import eu.foxcom.stp.gsa.egnss4cap.model.Task;
import eu.foxcom.stp.gsa.egnss4cap.model.Util;


public class TaskPhotoGalleryActivity extends BaseActivity {

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoAdapterHolder> {

        private class PhotoAdapterHolder extends RecyclerView.ViewHolder {

            private ImageView photoImageView;
            private ConstraintLayout errorConstraintLayout;
            private TextView errorTextView;

            public PhotoAdapterHolder(@NonNull View itemView) {
                super(itemView);
                photoImageView = itemView.findViewById(R.id.tpg_imageView_photo);
                errorConstraintLayout = itemView.findViewById(R.id.tpg_constraintLayout_error);
                errorTextView = itemView.findViewById(R.id.tpg_textView_photoError);
            }
        }

        private Context context;
        private PhotoList photoList;

        public PhotoAdapter(Context context, PhotoList photoList) {
            this.context = context;
            this.photoList = photoList;
        }


        @NonNull
        @Override
        public PhotoAdapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.photo_gallery_item, parent, false);
            return new PhotoAdapterHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoAdapterHolder holder, int position) {
            Photo photo = photoList.getPhotos().get(position);
            try {
                /* DEBUGCOM
                if (position == 1 || position == 2) {
                    throw new IOException("DEBUG IO EXCEPTION");
                }
                /**/
                holder.photoImageView.setImageBitmap(photo.getRotatedBitmap());
            } catch (IOException e) {
                holder.errorConstraintLayout.setVisibility(View.VISIBLE);
                holder.errorTextView.setText(context.getString(R.string.tpg_imageDrawError, e.getMessage()));
            }

        }

        @Override
        public int getItemCount() {
            return photoList.getPhotos().size();
        }
    }

    public static String INTENT_ACTION_START = "intentActionStart";
    public static String INTENT_ACTION_START_TASK_ID = "intentActionStartTaskId";
    public static String INTENT_ACTION_START_INIT_PHOTO_INDX = "intentActionStartInitPhotoIndx";

    private static String SAVED_INSTANCE_STATE_INIT_PHOTO_INDX = "SAVED_INSTANCE_STATE_INIT_PHOTO_INDX";

    private String taskId;
    private Task task;
    private PhotoList photoList;
    private int initPhotoIndx = 0;
    private ViewPager2 photoPager2;
    private DecimalFormat latLngDecimalFormat = Util.createPrettyCoordinateFormat();
    private DateTimeFormatter dateTimeFormatter = Util.createPrettyDateTimeFormat();
    private Bundle savedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_photo_gallery_v1);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setToolbar(R.id.toolbar);
        } else {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.savedInstanceState = savedInstanceState;

        photoPager2 = findViewById(R.id.tpg_viewPager2_photoPager);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (serviceController.isServiceInitialized() && photoPager2 != null) {
            outState.putInt(SAVED_INSTANCE_STATE_INIT_PHOTO_INDX, photoPager2.getCurrentItem());
        } else {
            outState.putInt(SAVED_INSTANCE_STATE_INIT_PHOTO_INDX, initPhotoIndx);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        if (intentActionStart()) {
            initUI();
            showInitIndxPhoto();
        } else {
            finish();
        }
    }

    private boolean intentActionStart() {
        Intent intent = getIntent();
        if (intent.getAction() == null) {
            return false;
        }
        if (intent.getAction().equals(INTENT_ACTION_START)) {
            if (!intent.hasExtra(INTENT_ACTION_START_TASK_ID)) {
                return false;
            }
            taskId = intent.getStringExtra(INTENT_ACTION_START_TASK_ID);
            if (taskId == null) {
                return false;
            }
            task = Task.createFromAppDatabase(taskId, MS.getAppDatabase());
            photoList = PhotoList.createFromAppDatabaseByTaskGroup(MS.getAppDatabase(), taskId, this);
            if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_INSTANCE_STATE_INIT_PHOTO_INDX)) {
                initPhotoIndx = savedInstanceState.getInt(SAVED_INSTANCE_STATE_INIT_PHOTO_INDX, 0);
            } else if (intent.hasExtra(INTENT_ACTION_START_INIT_PHOTO_INDX)) {
                initPhotoIndx = intent.getIntExtra(INTENT_ACTION_START_INIT_PHOTO_INDX, 0);
            }
            return true;
        } else {
            return false;
        }
    }

    private void initUI() {
        photoPager2.setAdapter(new PhotoAdapter(this, photoList));
        photoPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateUI();
            }
        });
    }

    private void showInitIndxPhoto() {
        photoPager2.setCurrentItem(initPhotoIndx, false);
        updateUI();
    }

    public void nextPhoto(View view) {
        if (photoPager2.getCurrentItem() + 1 == photoList.getPhotos().size()) {
            photoPager2.setCurrentItem(0, false);
        } else {
            photoPager2.setCurrentItem(photoPager2.getCurrentItem() + 1, false);
        }
        updateUI();
    }

    public void previousPhoto(View view) {
        if (photoPager2.getCurrentItem() - 1 < 0) {
            photoPager2.setCurrentItem(photoList.getPhotos().size(), false);
        } else {
            photoPager2.setCurrentItem(photoPager2.getCurrentItem() - 1, false);
        }
        updateUI();
    }

    public void deletePhotoDialog(View view) {
        if (!isPhotoDeletable(getCurrentlyShownPhoto())) {
            return;
        }
        TaskFulfillActivity.TaskDeletePhotoDialogBuilder builder = new TaskFulfillActivity.TaskDeletePhotoDialogBuilder(this) {
            @Override
            void deletePhoto() {
                TaskPhotoGalleryActivity.this.deletePhoto();
            }
        };
        builder.build().show();
    }

    private void deletePhoto() {
        if (!isPhotoDeletable(getCurrentlyShownPhoto())) {
            return;
        }
        photoList.removePhotoAt(photoPager2.getCurrentItem());
        if (photoList.getPhotos().size() == 0) {
            finish();
            return;
        }
        Intent intent = new Intent(this, TaskPhotoGalleryActivity.class);
        intent.setAction(TaskPhotoGalleryActivity.INTENT_ACTION_START);
        intent.putExtra(INTENT_ACTION_START_TASK_ID, taskId);
        int nextIndx = photoPager2.getCurrentItem() - 1;
        if (nextIndx < 0) {
            nextIndx = 0;
        }
        intent.putExtra(INTENT_ACTION_START_INIT_PHOTO_INDX, nextIndx);
        finish();
        startActivity(intent);
    }

    private void updateUI() {
        updatePhotoStats();

        ImageButton deleteImageButton = findViewById(R.id.tpg_imageButton_delete);
        Photo photo = photoList.getPhotos().get(photoPager2.getCurrentItem());
        if (isPhotoDeletable(photo)) {
            deleteImageButton.setVisibility(View.VISIBLE);
        } else {
            deleteImageButton.setVisibility(View.GONE);
        }
    }

    private boolean isPhotoDeletable(Photo photo) {
        return task.isEditable() && !photo.isSent();
    }

    private void updatePhotoStats() {
        TextView taskIdTextView = findViewById(R.id.tpg_textView_taskId);
        taskIdTextView.setText(taskId);
        TextView orderTextView = findViewById(R.id.tpg_textView_order);
        int curIndx = photoPager2.getCurrentItem();
        Photo photo = photoList.getPhotos().get(curIndx);
        orderTextView.setText(String.valueOf(curIndx + 1));
        TextView latTextView = findViewById(R.id.tpg_textView_latitude);
        latTextView.setText(latLngDecimalFormat.format(photo.getLat()));
        TextView lonTextView = findViewById(R.id.tpg_textView_longitude);
        lonTextView.setText(latLngDecimalFormat.format(photo.getLng()));
        TextView createdTextView = findViewById(R.id.tpg_textView_created);
        createdTextView.setText(photo.getCreated().toString(dateTimeFormatter));

        TextView counterTextView = findViewById(R.id.tpg_textView_photoCounter);
        counterTextView.setText((curIndx + 1) + "/" + photoList.getPhotos().size());
    }

    private Photo getCurrentlyShownPhoto() {
        return photoList.getPhotos().get(photoPager2.getCurrentItem());
    }

}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */