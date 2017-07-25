package com.ndipatri.solarmonitor.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.ndipatri.solarmonitor.R;

public class GrantFineLocationAccessDialogFragment extends DialogFragment {

    private static String TAG = GrantFineLocationAccessDialogFragment.class.getSimpleName();

    public static final int PERMISSION_REQUEST_FINE_LOCATION = 444;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String dialogTitle = getResources().getString(R.string.user_permission_requested);
        TextView titleView = new TextView(getActivity());
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleView.setText(dialogTitle);
        titleView.setGravity(Gravity.CENTER);

        builder.setTitle(dialogTitle)
            .setNeutralButton(R.string.grant_fine_location_access, (dialog, which) ->
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION));

        Dialog dialog = builder.create();

        dialog.getWindow().getAttributes().windowAnimations = R.style.slideup_dialog_animation;
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }
}

