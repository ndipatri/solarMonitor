package com.ndipatri.solarmonitor.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.rx.preferences2.Preference;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput;
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo;
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.MaybeObserver;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.CONFIGURE;
import static com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.LOAD;
import static com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.LOADED;
import static com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.LOADING;
import static com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.SCAN;
import static com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.SCANNING;

public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    //region dependency injection
    @Inject
    SolarOutputProvider solarOutputProvider;

    @Inject
    PanelScanProvider panelScanProvider;
    //endregion

    //region view injection
    @BindView(R.id.refreshProgressBar)
    ProgressBar refreshProgressBar;

    @BindView(R.id.mainTextView)
    TextView mainTextView;

    @BindView(R.id.detailTextView)
    TextView detailTextView;

    @BindView(R.id.loadFAB)
    FloatingActionButton loadFAB;

    @BindView(R.id.scanFAB)
    FloatingActionButton scanFAB;

    @BindView(R.id.configureFAB)
    FloatingActionButton configureFAB;
    //endregion

    //region local state
    private Disposable disposable;

    private PowerOutput currentPowerOutput;

    enum USER_STATE {
        SCAN,              // user has yet to find nearby panel
        SCANNING,          // user is scanning for nearby panel
        CONFIGURE,         // panel found but it needs to be configured.
        LOAD,              // user has nearby panel, but hasn't loaded output
        LOADING,           // user is loading solar output
        LOADED,            // user is viewing loaded solar output
        ;
    }
    private USER_STATE userState;
    // endregion

    public MainActivity() {
        SolarMonitorApp.getInstance().getObjectGraph().inject(this);
    }

    //region getSet
    public void setSolarOutputProvider(SolarOutputProvider solarOutputProvider) {
        this.solarOutputProvider = solarOutputProvider;
    }

    public void setPanelScanProvider(PanelScanProvider panelScanProvider) {
        this.panelScanProvider = panelScanProvider;
    }
    //endregion

    //region lifecyle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    @Override
    protected void onResume() {
        super.onResume();

        initialize();
    }

    private void initialize() {

        if (getSolarCustomerId().isSet()) {
            userState = LOAD;
        } else {
            userState = SCAN;
        }

        updateStatusViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (null != disposable) {
            disposable.dispose();
        }
    }

    @Override
    protected void handleConfigureFragmentDismiss() {
        initialize();
    }

    //endregion

    //region view processing
    private void updateStatusViews() {

        switch (userState) {
            case SCAN:
                refreshProgressBar.setVisibility(INVISIBLE);

                mainTextView.setVisibility(VISIBLE);
                mainTextView.setText(getString(R.string.click_to_find_nearby_solar_panel));

                detailTextView.setVisibility(INVISIBLE);

                showScanButton();
                hideLoadButton();
                hideConfigureButton();

                break;

            case SCANNING:
                refreshProgressBar.setVisibility(VISIBLE);

                mainTextView.setVisibility(VISIBLE);
                mainTextView.setText(getString(R.string.finding_nearby_solar_panel));

                detailTextView.setVisibility(INVISIBLE);

                hideScanButton();
                hideLoadButton();
                hideConfigureButton();
                break;

            case CONFIGURE:
                refreshProgressBar.setVisibility(INVISIBLE);

                mainTextView.setVisibility(VISIBLE);
                mainTextView.setText(getString(R.string.click_to_configure_nearby_panel));

                detailTextView.setVisibility(INVISIBLE);

                hideScanButton();
                hideLoadButton();
                showConfigureButton();
                break;

            case LOAD:
                refreshProgressBar.setVisibility(INVISIBLE);

                mainTextView.setVisibility(VISIBLE);
                mainTextView.setText(getString(R.string.click_to_load_solar_output));

                detailTextView.setVisibility(VISIBLE);
                detailTextView.setText("solar panel (" + getSolarCustomerId().get() + ")");

                showScanButton();
                showLoadButton();
                hideConfigureButton();
                break;

            case LOADING:
                refreshProgressBar.setVisibility(VISIBLE);

                mainTextView.setVisibility(VISIBLE);
                mainTextView.setText(getString(R.string.loading_solar_output));

                detailTextView.setVisibility(VISIBLE);
                detailTextView.setText("solar panel (" + getSolarCustomerId().get() + ")");

                hideScanButton();
                hideLoadButton();
                hideConfigureButton();
                break;

            case LOADED:
                refreshProgressBar.setVisibility(INVISIBLE);

                mainTextView.setVisibility(VISIBLE);
                StringBuilder sbuf = new StringBuilder()
                        .append("current: ")
                        .append(currentPowerOutput.getCurrentPowerInWatts())
                        .append(" watts, ")

                        .append("lifetime: ")
                        .append(currentPowerOutput.getLifeTimeEnergyInWattHours())
                        .append(" wattHours.");
                mainTextView.setText(sbuf.toString());

                detailTextView.setVisibility(VISIBLE);
                detailTextView.setText("solar panel (" + getSolarCustomerId().get() + ")");

                showScanButton();
                showLoadButton();
                hideConfigureButton();
                break;
        }
    }

    private void showLoadButton() {
        loadFAB.setVisibility(View.VISIBLE);
        loadFAB.setOnClickListener(viewClicked -> {

            userState = LOADING;
            loadSolarOutput();
            updateStatusViews();
        });
    }

    private void hideLoadButton() {
        loadFAB.setVisibility(View.INVISIBLE);
    }

    private void showScanButton() {
        scanFAB.setVisibility(View.VISIBLE);
        scanFAB.setOnClickListener(viewClicked -> {

            userState = SCANNING;
            scanForNearbyPanel();
            updateStatusViews();
        });
    }

    private void hideScanButton() {
        scanFAB.setVisibility(View.INVISIBLE);
    }

    private void showConfigureButton() {
        configureFAB.setVisibility(View.VISIBLE);
        configureFAB.setOnClickListener(viewClicked -> {

            userState = SCAN;
            launchConfigureFragment();
        });
    }

    private void hideConfigureButton() {
        configureFAB.setVisibility(View.INVISIBLE);
    }
    //endregion

    //region background calls
    private void scanForNearbyPanel() {

        panelScanProvider.scanForNearbyPanel().subscribe(new MaybeObserver<PanelInfo>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                MainActivity.this.disposable = disposable;
            }

            @Override
            public void onSuccess(PanelInfo panelInfo) {
                if (panelInfo.getCustomerId().isPresent() && panelInfo.getCustomerId().get().length() == 6) {
                    Log.d(TAG, "Panel found with customerId configured (" + panelInfo.getCustomerId().get() + ").");

                    SolarMonitorApp.getInstance().setSolarCustomerId(panelInfo.getCustomerId().get());

                    MainActivity.this.disposable = null;
                    MainActivity.this.currentPowerOutput = null;

                    userState = LOAD;
                    updateStatusViews();
                } else {
                    Log.d(TAG, "Panel found, but it needs to be configured.");

                    MainActivity.this.disposable = null;
                    MainActivity.this.currentPowerOutput = null;

                    userState = CONFIGURE;
                    updateStatusViews();
                }
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(MainActivity.this, getString(R.string.error_please_try_again), Toast.LENGTH_SHORT).show();

                if (getSolarCustomerId().isSet()) {
                    userState = LOAD;
                } else {
                    userState = SCAN;
                }
                updateStatusViews();
            }

            @Override
            public void onComplete() {
                Toast.makeText(MainActivity.this, getString(R.string.no_nearby_panels_were_found), Toast.LENGTH_SHORT).show();

                if (getSolarCustomerId().isSet()) {
                    userState = LOAD;
                } else {
                    userState = SCAN;
                }
                updateStatusViews();
            }
        });
    }

    private void loadSolarOutput() {
        if (getSolarCustomerId().isSet()) {

            solarOutputProvider.getSolarOutput(getSolarCustomerId().get()).subscribe(new SingleObserver<PowerOutput>() {
                @Override
                public void onSubscribe(Disposable disposable) {
                    MainActivity.this.disposable = disposable;
                }

                @Override
                public void onSuccess(PowerOutput currentPowerOutput) {
                    MainActivity.this.disposable = null;
                    MainActivity.this.currentPowerOutput = currentPowerOutput;

                    userState = LOADED;
                    updateStatusViews();
                }

                @Override
                public void onError(Throwable e) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_please_try_again), Toast.LENGTH_SHORT).show();

                    userState = LOAD;
                    updateStatusViews();
                }
            });
        }
    }
    //endregion

    private Preference<String> getSolarCustomerId() {
        return SolarMonitorApp.getInstance().getSolarCustomerId();
    }
}
