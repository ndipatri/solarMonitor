package com.ndipatri.solarmonitor.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConfigurePanelDialogFragment extends DialogFragment {

    public static final String TAG = ConfigurePanelDialogFragment.class.getSimpleName();

    @BindView(R.id.panelIdEditText)
    EditText panelIdEditText;

    @BindView(R.id.configurePanelButton)
    EditText configurePanelButton;

    @Inject PanelScanProvider panelScanProvider;

    public ConfigurePanelDialogFragment() {
        SolarMonitorApp.getInstance().getObjectGraph().inject(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String dialogTitle = getResources().getString(R.string.configure_panel);
        TextView titleView = new TextView(getActivity());
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleView.setText(dialogTitle);
        titleView.setGravity(Gravity.CENTER);

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_configure_panel, null);

        // Use ButterKnife for view injection (http://jakewharton.github.io/butterknife/)
        ButterKnife.bind(this, dialogView);

        builder.setTitle(dialogTitle)
                //.setCustomTitle(titleView)
                .setView(dialogView)
                .setNeutralButton("Done", (dialog, which) -> {

                    // nothign yet

                });

        Dialog dialog = builder.create();

        dialog.getWindow().getAttributes().windowAnimations = R.style.slideup_dialog_animation;
        dialog.setCanceledOnTouchOutside(false);

        setupViews();

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    private void setupViews() {
    }
}


