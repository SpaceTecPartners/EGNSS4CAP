package eu.foxcom.stp.gsa.egnss4cap;

import androidx.lifecycle.ViewModel;

public class UnownedPhotoDetailViewModel extends ViewModel {

    private boolean isLastNoteDialogShown = false;
    private String dialogNote;

    // region get, set

    public String getDialogNote() {
        return dialogNote;
    }

    public void setDialogNote(String dialogNote) {
        this.dialogNote = dialogNote;
    }

    public boolean isLastNoteDialogShown() {
        return isLastNoteDialogShown;
    }

    public void setLastNoteDialogShown(boolean lastNoteDialogShown) {
        isLastNoteDialogShown = lastNoteDialogShown;
    }

    // endregion

}


/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */