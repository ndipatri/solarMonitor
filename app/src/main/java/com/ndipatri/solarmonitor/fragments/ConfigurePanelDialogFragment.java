package com.ndipatri.solarmonitor.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ndipatri.iot.googleproximity.GoogleProximity;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo;
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.CompletableObserver;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;

import static com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.CONFIGURATION_ERROR;
import static com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.CONFIGURING_PANEL;
import static com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.NO_PANEL_FOUND;
import static com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.PANEL_FOUND;
import static com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.SEARCHING_FOR_PANEL;

public class ConfigurePanelDialogFragment extends DialogFragment {

    public static final String TAG = ConfigurePanelDialogFragment.class.getSimpleName();

    enum USER_STATE {
        SEARCHING_FOR_PANEL,
        NO_PANEL_FOUND,
        PANEL_FOUND,
        CONFIGURING_PANEL,
        CONFIGURATION_ERROR,
        ;
    }
    private USER_STATE userState;

    @BindView(R.id.panelDescriptionEditText)
    EditText panelDescriptionEditText;

    @BindView(R.id.customerIdEditText)
    EditText customerIdEditText;

    @BindView(R.id.userActionButton)
    Button userActionButton;

    @BindView(R.id.progressBar)
    View progressBar;

    @BindView(R.id.progressTextView)
    TextView progressTextView;

    @Inject PanelScanProvider panelScanProvider;

    private Disposable panelDisposable;

    private PanelInfo foundPanelInfo;

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
                .setNeutralButton("Done", (dialog, which) -> dismiss());

        Dialog dialog = builder.create();

        dialog.getWindow().getAttributes().windowAnimations = R.style.slideup_dialog_animation;
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        // NJD TODO - need to add a manual search button maybe?
        searchForPanel();

        updateStatusViews();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        GoogleProximity.getInstance().redirectToAuthenticationActivityIfNecessary(getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (null != panelDisposable) {
            panelDisposable.dispose();
        }
    }

    private void updateStatusViews() {

        switch (userState) {
            case SEARCHING_FOR_PANEL:
                panelDescriptionEditText.setVisibility(View.INVISIBLE);
                customerIdEditText.setVisibility(View.INVISIBLE);

                progressBar.setVisibility(View.VISIBLE);
                progressTextView.setVisibility(View.VISIBLE);
                progressTextView.setText(R.string.search_for_nearby_panel);

                userActionButton.setVisibility(View.INVISIBLE);

                break;

            case NO_PANEL_FOUND:
                panelDescriptionEditText.setVisibility(View.INVISIBLE);
                customerIdEditText.setVisibility(View.INVISIBLE);

                progressTextView.setVisibility(View.VISIBLE);
                progressTextView.setText(R.string.no_nearby_panels_were_found);
                progressBar.setVisibility(View.INVISIBLE);

                userActionButton.setText(R.string.search_again);
                userActionButton.setVisibility(View.VISIBLE);
                userActionButton.setOnClickListener(v -> searchForPanel());

                break;


            case PANEL_FOUND:
                panelDescriptionEditText.setVisibility(View.VISIBLE);
                panelDescriptionEditText.setText(foundPanelInfo.getDescription());

                customerIdEditText.setVisibility(View.VISIBLE);
                if (foundPanelInfo.getCustomerId().isPresent()) {
                    customerIdEditText.setHint(foundPanelInfo.getCustomerId().get());
                } else {
                    customerIdEditText.setHint(getActivity().getString(R.string.enter_customer_id));
                }

                progressTextView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);

                userActionButton.setText(getActivity().getString(R.string.configure_panel));
                userActionButton.setVisibility(View.VISIBLE);
                userActionButton.setOnClickListener(v -> configurePanel(panelDescriptionEditText.getText().toString(),
                                                                        customerIdEditText.getText().toString()));

                break;

            case CONFIGURING_PANEL:
                panelDescriptionEditText.setVisibility(View.INVISIBLE);
                customerIdEditText.setVisibility(View.INVISIBLE);

                progressBar.setVisibility(View.VISIBLE);
                progressTextView.setVisibility(View.VISIBLE);
                progressTextView.setText(R.string.configuring_nearby_panel);

                userActionButton.setVisibility(View.INVISIBLE);

                break;

            case CONFIGURATION_ERROR:
                panelDescriptionEditText.setVisibility(View.INVISIBLE);
                customerIdEditText.setVisibility(View.INVISIBLE);

                progressTextView.setVisibility(View.VISIBLE);
                progressTextView.setText(R.string.failed_to_configure_panel);
                progressBar.setVisibility(View.INVISIBLE);

                userActionButton.setText(R.string.OK);
                userActionButton.setVisibility(View.VISIBLE);
                userActionButton.setOnClickListener(v -> {
                    userState = PANEL_FOUND;
                    updateStatusViews();

                });

                break;
        }
    }

    private void configurePanel(final String panelDescription, final String _customerId) {

        String customerId = null;
        if (_customerId.length() == 6) {
            customerId = _customerId;
        }

        PanelInfo updatedPanelInfo = new PanelInfo(panelDescription, customerId);

        userState = CONFIGURING_PANEL;
        updateStatusViews();

        panelScanProvider.configureNearbyPanel(updatedPanelInfo).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                ConfigurePanelDialogFragment.this.panelDisposable = panelDisposable;
            }

            @Override
            public void onComplete() {
                if (updatedPanelInfo.getCustomerId().isPresent()) {
                    SolarMonitorApp.getInstance().setSolarCustomerId(updatedPanelInfo.getCustomerId().get());
                } else {
                    SolarMonitorApp.getInstance().getSolarCustomerId().delete();
                }

                dismiss();
            }

            @Override
            public void onError(Throwable e) {
                userState = CONFIGURATION_ERROR;
                updateStatusViews();
            }
        });
    }

    private void searchForPanel() {

        userState = SEARCHING_FOR_PANEL;
        updateStatusViews();

        panelScanProvider.scanForNearbyPanel().subscribe(new MaybeObserver<PanelInfo>() {
            @Override
            public void onSubscribe(Disposable d) {
                ConfigurePanelDialogFragment.this.panelDisposable = panelDisposable;
            }

            @Override
            public void onSuccess(PanelInfo foundPanelInfo) {
                Log.d(TAG, "Panel found.  Trying to configure ...");

                ConfigurePanelDialogFragment.this.foundPanelInfo = foundPanelInfo;

                userState = PANEL_FOUND;
                updateStatusViews();
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Error while searching for panel.", e);

                userState = NO_PANEL_FOUND;
                updateStatusViews();
            }

            @Override
            public void onComplete() {
                userState = NO_PANEL_FOUND;
                updateStatusViews();
            }
        });
    }
}



