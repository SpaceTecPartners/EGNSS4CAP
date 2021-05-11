package eu.foxcom.stp.gsa.egnss4cap.model;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import eu.foxcom.stp.gsa.egnss4cap.R;
import eu.foxcom.stp.gsa.egnss4cap.TaskOverviewActivity;

public class FilterTaskDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = FilterTaskDialogFragment.class.getName();

    private TaskOverviewActivity taskOverviewActivity;

    public FilterTaskDialogFragment(TaskOverviewActivity taskOverviewActivity) {
        this.taskOverviewActivity = taskOverviewActivity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new BottomSheetDialog(getContext(), R.style.sheetDialog);
        if (taskOverviewActivity.getFilterView().getParent() != null) {
            ((ViewGroup) taskOverviewActivity.getFilterView().getParent()).removeView(taskOverviewActivity.getFilterView());
        }
        dialog.setContentView(taskOverviewActivity.getFilterView());
        taskOverviewActivity.initFilter();
        return dialog;
    }

    // region get, set


    // endregion
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
