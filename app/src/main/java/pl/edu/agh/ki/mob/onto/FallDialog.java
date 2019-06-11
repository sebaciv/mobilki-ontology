package pl.edu.agh.ki.mob.onto;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;

public class FallDialog extends DialogFragment {

    public interface FallDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    private OntologyUtils.HumanStatus humanStatus;
    private FallDialogListener listener;
    private CountDownTimer countDownTimer;

    public void setHumanStatus(OntologyUtils.HumanStatus humanStatus) {
        this.humanStatus = humanStatus;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (FallDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FallDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle("Fall detected!")
                .setMessage("Cause: " + humanStatus.name())
                .setPositiveButton("Confirm", (dialog, id) -> {
                    listener.onDialogPositiveClick(FallDialog.this);
                    countDownTimer.cancel();
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    listener.onDialogNegativeClick(FallDialog.this);
                    countDownTimer.cancel();
                });
        AlertDialog dialog = builder.create();

        countDownTimer = new CountDownTimer(15 * 1000, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
               // dialog.setMessage(DateUtils.formatElapsedTime(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                listener.onDialogPositiveClick(FallDialog.this);
                dialog.cancel();
            }
        };
        countDownTimer.start();

        return dialog;
    }


}
