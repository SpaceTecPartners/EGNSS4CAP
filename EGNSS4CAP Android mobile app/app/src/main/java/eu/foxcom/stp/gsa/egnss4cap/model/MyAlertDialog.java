package eu.foxcom.stp.gsa.egnss4cap.model;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import eu.foxcom.stp.gsa.egnss4cap.R;

public class MyAlertDialog {

    public static class Builder {

        private Context context;
        private AlertDialog.Builder builder;

        public Builder(Context context) {
            this.context = context;
            builder = new AlertDialog.Builder(context);
            builder.setView(R.layout.dialog_my_alert_dialog);
        }

        public MyAlertDialog build() {
            return new MyAlertDialog(context, builder.create());
        }

        // region get, set
        public AlertDialog.Builder getBuilder() {
            return builder;
        }
        // endregion
    }

    private class OwnerLifecycleObserver implements LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        private void onDestroy() {
                if (alertDialog != null && alertDialog.isShowing()) { ;
                    alertDialog.dismiss();
                }
        }
    }

    private Context context;
    private LifecycleObserver lifecycleObserver;
    private AlertDialog alertDialog;
    // isCustomView = true has higher priority than customLayoutId = true
    private boolean isCustomLayout = false;
    private boolean isCustomView = false;
    private boolean isAutoButtons = true;
    private boolean isFirstShown = false;

    private LayoutInflater layoutInflater;
    private ConstraintLayout baseConstraintLayout;
    private ConstraintLayout simpleConstraintLayout;
    private TextView titleTextView;
    private TextView messageTextView;
    private FrameLayout customFrameLayout;
    private FrameLayout buttonsFrameLayout;
    private Button neutralButton;
    private Button positiveButton;
    private Button negativeButton;

    private String title;
    private String message;
    private int customLayoutId;
    private View customView;

    private String neutralText;
    private String positiveText;
    private String negativeText;
    private DialogInterface.OnClickListener neutralListener;
    private DialogInterface.OnClickListener positiveListener;
    private DialogInterface.OnClickListener negativeListener;

    private MyAlertDialog(Context context, AlertDialog alertDialog) {
        this.context = context;
        if (context instanceof AppCompatActivity) {
            this.lifecycleObserver = new OwnerLifecycleObserver();
            ((AppCompatActivity) context).getLifecycle().addObserver(lifecycleObserver);
        }
        this.alertDialog = alertDialog;

        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        layoutInflater = LayoutInflater.from(context);
    }

    private void onShow() {
        if (!isFirstShown) {
            init();
            isFirstShown = true;
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCustomLayout(int layoutId) {
        isCustomLayout = true;
        customLayoutId = layoutId;
    }

    public void setCustomView(View view) {
        isCustomView = true;
        customView = view;
    }

    public void setAutoButtons(boolean isAutoButtons) {
        this.isAutoButtons = isAutoButtons;
    }

    private void init() {
        initLayouts();
        initButtons();

        // resolve bug with lazy load focusable views
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    private void initLayouts() {
        baseConstraintLayout = alertDialog.findViewById(R.id.myAlertDialog_constraintLayout_base);
        simpleConstraintLayout = alertDialog.findViewById(R.id.myAlertDialog_constraintLayout_contentSimple);
        customFrameLayout = alertDialog.findViewById(R.id.myAlertDialog_frameLayout_contentCustom);
        titleTextView = alertDialog.findViewById(R.id.myAlertDialog_textView_title);
        messageTextView = alertDialog.findViewById(R.id.myAlertDialog_textView_message);
        if (isCustomView || isCustomLayout) {
            simpleConstraintLayout.setVisibility(View.GONE);
            if (isCustomView) {
                customFrameLayout.addView(customView);
            } else {
                layoutInflater.inflate(customLayoutId, customFrameLayout, true);
            }
        } else {
            if (title == null) {
                titleTextView.setVisibility(View.GONE);
            } else {
                titleTextView.setText(title);
            }
            if (message == null) {
                messageTextView.setVisibility(View.GONE);
            } else {
                messageTextView.setText(message);
            }
        }
        buttonsFrameLayout = alertDialog.findViewById(R.id.myAlertDialog_frameLayout_buttons);
    }

    private void initButtons() {
        if (isAutoButtons) {
            layoutInflater.inflate(R.layout.dialog_my_alert_dialog_buttons, buttonsFrameLayout, true);
        }
        if (neutralText != null) {
            neutralButton = alertDialog.findViewById(R.id.myAlertDialog_button_neutral);
            neutralButton.setVisibility(View.VISIBLE);
            neutralButton.setText(neutralText);
            neutralButton.setOnClickListener(v -> {
                if (neutralListener != null) {
                    neutralListener.onClick(alertDialog, AlertDialog.BUTTON_NEUTRAL);
                }
                alertDialog.dismiss();
            });
        }
        if (positiveText != null) {
            positiveButton = alertDialog.findViewById(R.id.myAlertDialog_button_positive);
            positiveButton.setVisibility(View.VISIBLE);
            positiveButton.setText(positiveText);
            positiveButton.setOnClickListener(v -> {
                if (positiveListener != null) {
                    positiveListener.onClick(alertDialog, AlertDialog.BUTTON_POSITIVE);
                }
                alertDialog.dismiss();
            });
        }
        if (negativeText != null) {
            negativeButton = alertDialog.findViewById(R.id.myAlertDialog_button_negative);
            negativeButton.setVisibility(View.VISIBLE);
            negativeButton.setText(negativeText);
            negativeButton.setOnClickListener(v -> {
                if (negativeListener != null) {
                    negativeListener.onClick(alertDialog, AlertDialog.BUTTON_NEGATIVE);
                }
                alertDialog.dismiss();
            });
        }

    }

    public void show() {
        alertDialog.show();
        onShow();
    }

    public void hide() {
        alertDialog.hide();
    }

    public void setButton(int whichButton, String text, DialogInterface.OnClickListener listener) {
        switch (whichButton) {
            case AlertDialog.BUTTON_NEUTRAL:
                neutralListener = listener;
                neutralText = text;
                break;
            case AlertDialog.BUTTON_POSITIVE:
                positiveListener = listener;
                positiveText = text;
                break;
            case AlertDialog.BUTTON_NEGATIVE:
                negativeListener = listener;
                negativeText = text;
                break;
        }
    }


    // region get, set

    public AlertDialog getAlertDialog() {
        return alertDialog;
    }

    public Button getNeutralButton() {
        return neutralButton;
    }

    public Button getPositiveButton() {
        return positiveButton;
    }

    public Button getNegativeButton() {
        return negativeButton;
    }

    // endregion

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
