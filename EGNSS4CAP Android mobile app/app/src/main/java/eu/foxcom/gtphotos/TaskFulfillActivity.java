package eu.foxcom.gtphotos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.foxcom.gtphotos.model.AppDatabase;
import eu.foxcom.gtphotos.model.Photo;
import eu.foxcom.gtphotos.model.PhotoList;
import eu.foxcom.gtphotos.model.Task;
import eu.foxcom.gtphotos.model.Util;
import eu.foxcom.gtphotos.model.functionInterface.Consumer;

public class TaskFulfillActivity extends BaseActivity {

    static abstract class TaskDeletePhotoDialogBuilder {

        private Context context;

        TaskDeletePhotoDialogBuilder(Context context) {
            this.context = context;
        }

        abstract void deletePhoto();

        final AlertDialog build() {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.tf_deleteDialogTitle);
            builder.setMessage(R.string.tf_deleteDialogText);
            builder.setNegativeButton(R.string.dl_Cancel, (dialog, which) -> {
            });
            builder.setPositiveButton(R.string.dl_OK, (dialog, which) -> {
                deletePhoto();
            });
            return builder.create();
        }
    }

    public static final String INTENT_ACTION_START = "intentStart";
    public static final String INTENT_ACTION_START_TASK_ID = "taskId";
    public static final String INTENT_ACTION_START_INIT_MOVE_PHOTO_INDX = "photoIndx";

    public static final String INTENT_ACTION_SNAP_PHOTO = "snapPhoto";
    public static final String INTENT_ACTION_START_SNAP_PHOTO_ID = "snapPhotoId";

    private String taskId;
    private Integer initPhotoIndx;
    private boolean editable = false;
    private boolean sendable = false;
    private Task task;
    private PhotoList photoList;
    private ViewFlipper photoFlipper;
    private AtomicBoolean isUploading = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_fulfill);

        photoFlipper = findViewById(R.id.tf_viewFlipper_photoFlipper);

        if (!intentActionStart(getIntent())) {
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serviceController.isServiceInitialized()) {
            refreshPhotoFlipper();
        }
    }

    private boolean intentActionStart(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (!(intent.getAction() != null && intent.getAction().equals(INTENT_ACTION_START))) {
            return false;
        }
        taskId = intent.getStringExtra(INTENT_ACTION_START_TASK_ID);
        if (taskId == null) {
            return false;
        }
        if (intent.hasExtra(INTENT_ACTION_START_INIT_MOVE_PHOTO_INDX)) {
            initPhotoIndx = intent.getIntExtra(INTENT_ACTION_START_INIT_MOVE_PHOTO_INDX, 0);
        }
        return true;
    }

    private void intentActionSnapPhoto(Intent intent) {
        if (intent == null) {
            return;
        }
        if (intent.getAction() != null && !intent.getAction().equals(INTENT_ACTION_SNAP_PHOTO)) {
            return;
        }
        if (intent.hasExtra(INTENT_ACTION_START_SNAP_PHOTO_ID)) {
            Long snapPhotoId = intent.getLongExtra(INTENT_ACTION_START_SNAP_PHOTO_ID, 0);
            addPhotoFromSnap(snapPhotoId);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        intentActionSnapPhoto(intent);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        task = Task.createFromAppDatabase(taskId, MS.getAppDatabase());
        if (task == null) {
            finish();
            return;
        }
        editable = task.isEditable();
        sendable = task.isSendable();

        initAlerts();
        loadTaskUI();
    }

    @Override
    protected MainService.BROADCAST_MSG broadcastReceiver(Context context, Intent intent) {
        MainService.BROADCAST_MSG broadcastMsg = super.broadcastReceiver(context, intent);
        switch (broadcastMsg) {
            case BROADCAST_ID:
                break;
            case TYPE:
                break;
            case STARTED:
                break;
            case REFRESH_TASKS_STARTED:
                break;
            case REFRESH_TASKS:
                    restartActivity();
            case UPLOAD_TASK_STATUS:
                break;
            case REFRESH_PHOTOS:
                break;
        }
        return broadcastMsg;
    }

    @Override
    protected void restartActivity() {
        Intent intent = new Intent(this, this.getClass());
        intent.setAction(INTENT_ACTION_START);
        intent.putExtra(INTENT_ACTION_START_TASK_ID, taskId);
        finish();
        startActivity(intent);
    }

    private void initAlerts() {
        // upozornění na mezistav uzamčení
        if (!editable && sendable) {
            alert(getString(R.string.tf_lockedTaskTitle), getString(R.string.tf_lockedTaskText));
        }
    }

    private void loadTaskUI() {
        if (!editable) {
            overloadUIForNoEditable();
        }

        TextView idTextView = findViewById(R.id.tf_textView_id);
        idTextView.setText(task.getId());
        TextView nameTextView = findViewById(R.id.tf_textView_name);
        nameTextView.setText(task.getName());
        TextView statusTextView = findViewById(R.id.tf_textView_status);
        statusTextView.setText(getString(Task.STATUS.createFromDBVal(task.getStatus()).NAME_ID));
        TextView dateCreatedTextView = findViewById(R.id.tf_textView_dateCreated);
        dateCreatedTextView.setText(task.getDateCreated());
        TextView taskDueDateTextView = findViewById(R.id.tf_textView_taskDueDate);
        taskDueDateTextView.setText(task.getTaskDueToDate());
        TextView textTextView = findViewById(R.id.tf_textView_text);
        textTextView.setText(task.getText());
        if (task.getTextReturned() != null) {
            TextView textReturnedTextView = findViewById(R.id.tf_textView_textReturned);
            textReturnedTextView.setText(task.getTextReturned());
            LinearLayout textReturnedLayout = findViewById(R.id.tf_linearLayout_textReturned);
            textReturnedLayout.setVisibility(View.VISIBLE);
        }
        TextInputEditText noteEditText = findViewById(R.id.tf_textInputEditText_note);
        noteEditText.setText(task.getNote());

        refreshPhotoFlipper();
    }

    private void overloadUIForNoEditable() {
        getSupportActionBar().setTitle(R.string.title_taskDetail);
        TextInputEditText noteEditText = findViewById(R.id.tf_textInputEditText_note);
        noteEditText.setEnabled(false);
        ImageButton addPhotoImageButton = findViewById(R.id.tf_imageButton_addPhoto);
        addPhotoImageButton.setVisibility(View.GONE);
        ImageButton removePhotoImageButton = findViewById(R.id.tf_imageButton_removePhoto);
        removePhotoImageButton.setVisibility(View.GONE);
        if (!sendable) {
            Button sendButton = findViewById(R.id.tf_button_send);
            sendButton.setVisibility(View.GONE);
        }
    }

    private void fillPhotoFlipper() {
        photoFlipper.removeAllViews();
        for (Photo photo : photoList.getPhotos()) {
            try {
                Bitmap bitmap = photo.getRotatedBitmap();
                addPhotoToFlipper(bitmap);
            /* DEBUGCOM
            try {
                photo.toJSONObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
            /**/
            } catch (IOException e) {
                alert(getString(R.string.tf_failedLoadPhoto), getString(R.string.tf_failedLoadPhotoText, e.getMessage()));

            }
        }
        if (initPhotoIndx != null) {
            moveToPhotoAt(initPhotoIndx);
        }
    }

    private void refreshPhotoFlipper() {
        photoList = PhotoList.createFromAppDatabase(MS.getAppDatabase(), taskId, getApplicationContext());
        fillPhotoFlipper();
        updatePhotoStats();
    }

    public void previousPhoto(View view) {
        photoFlipper.showPrevious();
        updatePhotoStats();
    }

    public void nextPhoto(View view) {
        photoFlipper.showNext();
        updatePhotoStats();
    }

    private void moveToPhotoAt(int indx) {
        photoFlipper.setDisplayedChild(indx);
        updatePhotoStats();
    }

    public void getPhoto(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.setAction(CameraActivity.INTENT_ACTION_START);
        intent.putExtra(CameraActivity.INTENT_ACTION_START_TASK_ID, taskId);
        startActivity(intent);
    }

    private void addPhotoFromSnap(long photoId) {
        try {
            Photo photo = Photo.createFromAppDatabase(photoId, MS.getAppDatabase(), getApplicationContext());
            addPhotoToFlipper(photo.getRotatedBitmap());
            /* DEBUGCOM
            try {
                photo.toJSONObject();
            }catch (Exception e) {

            }
            photo.readExif();
            /**/
            photoList.addPhoto(photo);
            showLastPhoto();
        } catch (IOException e) {
            alert(getString(R.string.gn_failure), getString(R.string.tf_savePhotoFileFailure, e.getMessage()));
        }
    }

    private void addPhotoToFlipper(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setLayoutParams(new ViewFlipper.LayoutParams(ViewFlipper.LayoutParams.MATCH_PARENT, ViewFlipper.LayoutParams.MATCH_PARENT));
        photoFlipper.addView(imageView);
    }

    public void removePhotoDialog(View view) {
        if (!editable) {
            return;
        }
        int indx = photoFlipper.getDisplayedChild();
        if (photoFlipper.getChildCount() == 0 || photoList.getPhotos().get(indx).isSent()) {
            return;
        }
        TaskDeletePhotoDialogBuilder builder = new TaskDeletePhotoDialogBuilder(this) {
            @Override
            void deletePhoto() {
                removePhoto();
            }
        };
        builder.build().show();
    }

    private void removePhoto() {
        int indx = photoFlipper.getDisplayedChild();
        if (photoFlipper.getChildCount() == 0 || photoList.getPhotos().get(indx).isSent()) {
            return;
        }
        photoFlipper.removeViewAt(indx);
        photoList.removePhotoAt(indx);
        updatePhotoStats();
    }

    private void updatePhotoStats() {
        TextView latTextView = findViewById(R.id.tf_textView_lat);
        TextView lngTextView = findViewById(R.id.tf_textView_lng);
        TextView photoCreatedTextView = findViewById(R.id.tf_textView_photoCreated);
        if (photoList.getPhotos().size() > 0) {
            Photo photo = photoList.getPhotos().get(photoFlipper.getDisplayedChild());
            latTextView.setText(photo.getLat() == null ? "" : photo.getLat().toString());
            lngTextView.setText(photo.getLng() == null ? "" : photo.getLng().toString());
            photoCreatedTextView.setText(photo.getCreated() == null ? "" : photo.getCreated().toString(Util.createPrettyDateTimeFormat()));
            ImageButton deleteImageButton = findViewById(R.id.tf_imageButton_removePhoto);
            if (editable && !photo.isSent()) {
                deleteImageButton.setVisibility(View.VISIBLE);
            } else {
                deleteImageButton.setVisibility(View.GONE);
            }
        } else {
            latTextView.setText("");
            lngTextView.setText("");
            photoCreatedTextView.setText("");
        }
        updatePhotoCounter();
    }

    private void updatePhotoCounter() {
        int sum = photoFlipper.getChildCount();
        int cur = photoFlipper.getDisplayedChild();
        if (sum > 0) {
            ++cur;
        } else {
            cur = 0;
        }
        TextView counterTextView = findViewById(R.id.tf_textView_counter);
        counterTextView.setText(cur + "/" + sum);
        if (sum == 0) {
            return;
        }
    }

    private void showLastPhoto() {
        int sum = photoFlipper.getChildCount();
        if (sum == 0) {
            return;
        }
        photoFlipper.setDisplayedChild(sum - 1);
        updatePhotoStats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        serviceController.addAfterInitializedTask(new ServiceController.Task() {
            @Override
            public void run() {
                saveTask();
            }
        });
    }

    private void saveTask() {
        TextInputEditText noteEditText = findViewById(R.id.tf_textInputEditText_note);
        task.setNote(noteEditText.getText().toString());
        task.saveToDB(MS.getAppDatabase());
    }

    public void sendDialog(View view) {
        if (!serviceController.isServiceInitialized() || !sendable) {
            return;
        }
        synchronized (isUploading) {
            if (isUploading.get()) {
                return;
            }
            isUploading.set(true);
            if (PhotoList.countTaskPhotosNotSent(MS.getAppDatabase(), taskId) == 0) {
                alert(getString(R.string.tf_sendDialogNoPhotosTitle), getString(R.string.tf_sendDialogNoPhotosText));
                isUploading.set(false);
                return;
            }
            // zkratka pro zamčenou úlohu
            if (!editable && sendable) {
                send();
                return;
            }
            AtomicBoolean isUploadStarted = new AtomicBoolean(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tf_sendDialogTitle);
            builder.setMessage(R.string.tf_sendDialogText);
            builder.setNegativeButton(R.string.dl_Cancel, (dialog, which) -> {
                isUploading.set(false);
            });
            builder.setPositiveButton(R.string.dl_OK, (dialog, which) -> {
                isUploadStarted.set(true);
                send();
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!isUploadStarted.get()) {
                        isUploading.set(false);
                    }
                }
            });
            builder.create().show();
        }
    }

    private void send() {
        synchronized (isUploading) {
        /* DEBUGCOM
        if (true) {
            for (Photo photo : photoList.getPhotos()) {
                try {
                    photo.toJSONObject();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        /**/
            setSendingState(true);
            TextInputEditText noteEditText = findViewById(R.id.tf_textInputEditText_note);
            task.setNote(noteEditText.getText().toString());
            task.setNotSentPhotos(true);
            String lastStatus = task.getStatus();
            task.setStatus(Task.STATUS.DATA_PROVIDED.DB_VAL);
            Consumer failedProcess = (errorString) -> {
                task.setLastSendFailed(true);
                task.setStatus(lastStatus);
                task.saveToDB(MS.getAppDatabase());
                if (isCreated) {
                    AlertDialog failedDialog = alertBuild(getString(R.string.tf_sendFailedTitle), errorString.toString());
                    failedDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            restartActivity();
                        }
                    });
                    failedDialog.show();
                }
            };
            Task.UpdateTaskReceiver finalReceiver = new Task.UpdateTaskReceiver() {

                @Override
                public void success(AppDatabase appDatabase, Task task) {
                    task.setLastSendFailed(false);
                    photoList.setSent(true);
                    photoList.recreateToDB();
                    if (isCreated) {
                        TaskFulfillActivity.this.finish();
                    }
                }

                @Override
                protected void success(AppDatabase appDatabase) {
                    // not used
                }

                @Override
                public void failed(String error) {
                    failedProcess.accept(error);
                }

                @Override
                public void finish(boolean success) {
                    setSendingState(false);
                    if (isCreated) {
                        isUploading.set(false);
                    }
                }
            };
            try {
                task.updateCompleteInMultipleRequest(MS.getAppDatabase(), this, finalReceiver, MS.getRequestor());
            } catch (Exception e) {
                isUploading.set(false);
                failedProcess.accept(e.getMessage());
            }
        }
    }

    public void setSendingState(boolean sending) {
        Button button = findViewById(R.id.tf_button_send);
        ProgressBar progressBar = findViewById(R.id.tf_progressBar_sending);
        if (sending) {
            button.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    public void goToGallery(View view) {
        if (photoList.getPhotos().size() == 0) {
            return;
        }
        int indx = photoFlipper.getDisplayedChild();
        Intent intent = new Intent(this, TaskPhotoGalleryActivity.class);
        intent.setAction(TaskPhotoGalleryActivity.INTENT_ACTION_START);
        intent.putExtra(TaskPhotoGalleryActivity.INTENT_ACTION_START_TASK_ID, taskId);
        intent.putExtra(TaskPhotoGalleryActivity.INTENT_ACTION_START_INIT_PHOTO_INDX, indx);
        startActivity(intent);
    }
}
