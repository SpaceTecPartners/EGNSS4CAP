package eu.foxcom.gtphotos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;

import eu.foxcom.gtphotos.model.AppDatabase;
import eu.foxcom.gtphotos.model.LoggedUser;
import eu.foxcom.gtphotos.model.Photo;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();

        if (!intentActionStart()) {
            finish();
        }
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
    protected void onDestroy() {
        super.onDestroy();
        if (isPhotoDataChanged || isNewPhoto) {
            goToOverView();
        }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pd_deleteDialogTitle);
        builder.setMessage(R.string.pd_deleteDialogText);
        builder.setNegativeButton(R.string.dl_Cancel, (dialog, which) -> {

        });
        builder.setPositiveButton(R.string.dl_OK, (dialog, which) -> {
            deletePhotoDialog();
        });
        builder.create().show();
    }

    private void deletePhotoDialog() {
        photo.delete(MS.getAppDatabase());
        goToOverView();
    }

    public void uploadPhotoDialog(View view) {
        if (!serviceController.isServiceInitialized() ||!photo.isSendable()) {
            return;
        }
        // zkratka pro uzamčenou fotografii
        if (!photo.isEditable() && photo.isSendable()) {
            uploadPhoto();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pd_sendDialogTitle);
        builder.setMessage(R.string.pd_sendDialogText);
        builder.setNegativeButton(R.string.dl_Cancel, (dialog, which) -> {});
        builder.setPositiveButton(R.string.dl_OK, (dialog, which) -> {
           uploadPhoto();
        });
        builder.create().show();
    }

    private void uploadPhoto() {
        if (!serviceController.isServiceInitialized() || !photo.isSendable()) {
            return;
        }
        Consumer failedProcess = (errorString) -> {
            photo.setLastSendFailed(true);
            photo.refreshToDB(MS.getAppDatabase());
            if (isCreated) {
                AlertDialog failedDialog = alertBuild(getString(R.string.pd_errorUploadPhotoTitle), getString(R.string.pd_errorUploadPhotoText, errorString));
                failedDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
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
            }, MS.getRequestor());

        } catch (JSONException e) {
            failedProcess.accept(e.getMessage());
        }
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
        if (photo == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View noteView = inflater.inflate(R.layout.photo_note_dialog, null);
        TextInputEditText noteEditText = noteView.findViewById(R.id.pd_textInputEditText_note);
        noteEditText.setText(photo.getNote());
        builder.setView(noteView);
        builder.setNegativeButton(R.string.dl_Cancel, null);
        builder.setPositiveButton(R.string.dl_OK, (dialog, which) -> {
            AlertDialog alertDialog = (AlertDialog) dialog;
            noteSave(noteEditText.getText().toString());
        });
        builder.create().show();
    }

    private void noteSave(String note) {
        photo.setNote(note);
        photo.refreshToDB(MS.getAppDatabase());
        TextView noteTextView = findViewById(R.id.pd_textView_note);
        noteTextView.setText(note);
        isPhotoDataChanged = true;
    }

    private void initAlerts() {
        // upozornění na mezistav uzamčení
        if (!photo.isEditable() && photo.isSendable()) {
            alert(getString(R.string.pd_lockedPhotoTitle), getString(R.string.pd_lockedPhotoText));
        }
    }
}