package eu.foxcom.gtphotos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;

import eu.foxcom.gtphotos.model.AppDatabase;
import eu.foxcom.gtphotos.model.LoggedUser;
import eu.foxcom.gtphotos.model.MyAlertDialog;
import eu.foxcom.gtphotos.model.Photo;
import eu.foxcom.gtphotos.model.PhotoList;
import eu.foxcom.gtphotos.model.Task;
import eu.foxcom.gtphotos.model.Util;
import eu.foxcom.gtphotos.model.functionInterface.Consumer;

public class UnownedPhotoDetailActivity extends BaseActivity {

    public static final String INTENT_ACTION_START = "intentActionStart";
    public static final String INTENT_ACTION_START_PHOTO_ID = "photoId";
    // nullable
    public static final String INTENT_ACTION_START_NEW_PHOTO = "new";

    private Photo photo;
    private boolean isPhotoDataChanged = false;
    private boolean isNewPhoto = false;

    private UnownedPhotoDetailViewModel unownedPhotoDetailViewModel;
    private boolean isNoteDialogShown = false;
    private TextInputEditText noteTextInputEditText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setToolbar(R.id.toolbar);
        } else {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        unownedPhotoDetailViewModel = new ViewModelProvider(this).get(UnownedPhotoDetailViewModel.class);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        if (!intentActionStart()) {
            finish();
            return;
        }

        loadRuntimeDialogNote();
    }

    private boolean intentActionStart() {
        Intent intent = getIntent();
        if (intent.getAction() == null || !intent.getAction().equals(INTENT_ACTION_START)) {
            return false;
        }
        if (!intent.hasExtra(INTENT_ACTION_START_PHOTO_ID)) {
            return false;
        }
        loadPhoto(intent.getLongExtra(INTENT_ACTION_START_PHOTO_ID, 0), intent.getBooleanExtra(INTENT_ACTION_START_NEW_PHOTO, false));
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        saveRuntimeDialogNote();
    }

    private void loadPhoto(long id, boolean isNewPhoto) {
        photo = Photo.createFromAppDatabase(id, MS.getAppDatabase(), getApplicationContext());
        DecimalFormat decimalFormat = Util.createLatLngDecimalFormat();
        TextView latitudeTextView = findViewById(R.id.pd_textView_latitude);
        latitudeTextView.setText(decimalFormat.format(photo.getLat()));
        TextView longitudeTextView = findViewById(R.id.pd_textView_longitude);
        longitudeTextView.setText(decimalFormat.format(photo.getLng()));
        TextView createdTextView = findViewById(R.id.pd_textView_created);
        createdTextView.setText(photo.getCreated().toString(Util.createPrettyDateTimeFormat()));
        TextView sentTextView = findViewById(R.id.pd_textView_sent);
        sentTextView.setText(photo.isSent() ? getString(R.string.pd_sendedYes) : getString(R.string.pd_sendedNo));
        TextView noteTextView = findViewById(R.id.pd_textView_note);
        noteTextView.setText(photo.getNote());
        ImageView photoImageView = findViewById(R.id.pd_imageView_photo);
        try {
            photoImageView.setImageBitmap(photo.getRotatedBitmap());
        } catch (IOException e) {
            alert(getString(R.string.pd_errorImageLoadTitle), getString(R.string.pd_errorImageLoadText, e.getMessage()));
        }
        ImageButton deleteImageButton = findViewById(R.id.pd_imageButton_delete);
        Button uploadImageButton = findViewById(R.id.pd_imageButton_upload);
        ImageButton noteImageButton = findViewById(R.id.pd_imageButton_note);
        if (photo.isEditable()) {
            deleteImageButton.setVisibility(View.VISIBLE);
            uploadImageButton.setVisibility(View.VISIBLE);
            noteImageButton.setVisibility(View.VISIBLE);
        } else {
            deleteImageButton.setVisibility(View.GONE);
            uploadImageButton.setVisibility(View.GONE);
            noteImageButton.setVisibility(View.GONE);
        }
        if (photo.isSendable()) {
            uploadImageButton.setVisibility(View.VISIBLE);
        } else {
            uploadImageButton.setVisibility(View.GONE);
        }
        if (isNewPhoto) {
            this.isNewPhoto = true;
            noteDialog(getWindow().getDecorView().getRootView());
        }
        initAlerts();
    }

    public void deletePhotoDialog(View view) {
        if (!serviceController.isServiceInitialized()) {
            return;
        }
        MyAlertDialog.Builder builder = new MyAlertDialog.Builder(this);
        MyAlertDialog myAlertDialog = builder.build();
        myAlertDialog.setTitle(getString(R.string.pd_deleteDialogTitle));
        myAlertDialog.setMessage(getString(R.string.pd_deleteDialogText));
        myAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), (dialog, which) -> {

        });
        myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dl_OK), (dialog, which) -> {
            deletePhotoDialog();
        });
        myAlertDialog.show();
    }

    private void deletePhotoDialog() {
        photo.delete(MS.getAppDatabase());
        goToOverView();
    }

    public void uploadPhotoDialog(View view) {
        if (!serviceController.isServiceInitialized() || !photo.isSendable()) {
            return;
        }
        // shortcut for locked photo
        if (!photo.isEditable() && photo.isSendable()) {
            uploadPhoto();
            return;
        }
        MyAlertDialog.Builder builder = new MyAlertDialog.Builder(this);
        MyAlertDialog myAlertDialog = builder.build();
        myAlertDialog.setTitle(getString(R.string.pd_sendDialogTitle));
        myAlertDialog.setMessage(getString(R.string.pd_sendDialogText));
        myAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), (dialog, which) -> {
        });
        myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dl_OK), (dialog, which) -> {
            uploadPhoto();
        });
        myAlertDialog.show();
    }

    private void uploadPhoto() {
        if (!serviceController.isServiceInitialized() || !photo.isSendable()) {
            return;
        }
        Consumer failedProcess = (errorString) -> {
            photo.setLastSendFailed(true);
            photo.refreshToDB(MS.getAppDatabase());
            if (isCreated) {
                MyAlertDialog failedDialog = alertBuild(getString(R.string.pd_errorUploadPhotoTitle), getString(R.string.pd_errorUploadPhotoText, errorString));
                failedDialog.getAlertDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        restartActivity();
                    }
                });
                failedDialog.show();
            }
        };
        try {
            Task virtualTask = Task.createFromAppDatabaseSpecialUnownedPhoto(MS.getAppDatabase(), photo.getId(), LoggedUser.createFromAppDatabase(MS.getAppDatabase()).getId());
            virtualTask.updateCompleteInMultipleRequest(MS.getAppDatabase(), this, new Task.UpdateTaskReceiver() {
                @Override
                protected void success(AppDatabase appDatabase, Task task) {
                    photo.setLastSendFailed(false);
                    photo.setSent(true);
                    photo.refreshToDB(appDatabase);
                    if (isCreated) {
                        goToOverView();
                    }
                }

                @Override
                protected void success(AppDatabase appDatabase) {

                }

                @Override
                protected void failed(String error) {
                    failedProcess.accept(error);
                }

                @Override
                protected void finish(boolean success) {

                }
            }, MS.getRequestor(), PhotoList.createFromUnassignedPhoto(MS.getAppDatabase(), this, photo));

        } catch (JSONException e) {
            failedProcess.accept(e.getMessage());
        }
    }

    private void refreshUnownedPhotoOverview() {
        if (!serviceController.isServiceInitialized()) {
            return;
        }
        Intent intent = MS.createBroadcastExplicitIntent(UnownedPhotoOverviewActivity.class, UnownedPhotoOverviewActivity.BROADCAST_ACTION.REFRESH_PHOTOS.name());
        if (isNewPhoto) {
            intent.putExtra(UnownedPhotoOverviewActivity.BROADCAST_REFRESH_PHOTOS.SCROLL_TOP.name(), true);
        }
        MS.sendBroadcastMessage(intent);
    }

    private void goToOverView() {
        Intent intent = new Intent(this, UnownedPhotoOverviewActivity.class);
        intent.setAction(UnownedPhotoOverviewActivity.INTENT_ACTION_REFRESH_PHOTOS);
        if (isNewPhoto) {
            intent.putExtra(UnownedPhotoOverviewActivity.INTENT_ACTION_REFRESH_PHOTOS_SCROLL_TOP, true);
        }
        startActivity(intent);
        finish();
    }

    public void noteDialog(View view) {
        noteDialog((String) null);
    }

    private void noteSave(String note) {
        photo.setNote(note);
        photo.refreshToDB(MS.getAppDatabase());
        TextView noteTextView = findViewById(R.id.pd_textView_note);
        noteTextView.setText(note);
        isPhotoDataChanged = true;
        refreshUnownedPhotoOverview();
    }

    private void noteDialog(String note) {
        if (photo == null) {
            return;
        }
        MyAlertDialog.Builder builder = new MyAlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View noteView = inflater.inflate(R.layout.photo_note_dialog, null);
        TextInputEditText noteEditText = noteView.findViewById(R.id.pd_textInputEditText_note);
        if (note == null) {
            note = photo.getNote();
        }
        noteEditText.setText(note);
        MyAlertDialog myAlertDialog = builder.build();
        myAlertDialog.setCustomView(noteView);
        myAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), null);
        myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dl_OK), (dialog, which) -> {
            noteSave(noteEditText.getText().toString());
        });
        myAlertDialog.getAlertDialog().setOnDismissListener(dialog -> {
            noteTextInputEditText = null;
            isNoteDialogShown = false;
        });
        myAlertDialog.show();
        isNoteDialogShown = true;
        noteTextInputEditText = noteEditText;
    }

    private void saveRuntimeDialogNote() {
        if (isNoteDialogShown && noteTextInputEditText != null) {
            unownedPhotoDetailViewModel.setLastNoteDialogShown(true);
            unownedPhotoDetailViewModel.setDialogNote(noteTextInputEditText.getText().toString());
        } else {
            unownedPhotoDetailViewModel.setLastNoteDialogShown(false);
        }
    }

    private void loadRuntimeDialogNote() {
        if (unownedPhotoDetailViewModel.isLastNoteDialogShown() && isRunning) {
            noteDialog(unownedPhotoDetailViewModel.getDialogNote());
        }
    }

    private void initAlerts() {
        // alert for lock status
        if (!photo.isEditable() && photo.isSendable()) {
            alert(getString(R.string.pd_lockedPhotoTitle), getString(R.string.pd_lockedPhotoText));
        }
    }
}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */