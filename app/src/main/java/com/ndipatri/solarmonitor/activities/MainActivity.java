package com.ndipatri.solarmonitor.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.rx.preferences2.Preference;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.dto.PowerOutput;
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo;
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Inject
    SolarOutputProvider solarOutputProvider;
    @Inject
    PanelScanProvider panelScanProvider;

    @BindView(R.id.refreshProgressBar)
    ProgressBar refreshProgressBar;
    @BindView(R.id.mainTextView)
    TextView mainTextView;
    @BindView(R.id.detailTextView)
    TextView detailTextView;
    @BindView(R.id.solarUpdateFAB)
    FloatingActionButton solarUpdateFAB;
    @BindView(R.id.beaconScanFAB)
    FloatingActionButton beaconScanFAB;

    private Disposable panelScanDisposable;
    private Disposable solarOutputDisposable;

    private PowerOutput currentPowerOutput;

    public MainActivity() {
        SolarMonitorApp.getInstance().getObjectGraph().inject(this);
    }

    //region menuSetup
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else
        if (id == R.id.action_configure) {


            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

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

        beaconScanFAB.setOnClickListener(viewClicked -> scanForNearbyPanels());
        solarUpdateFAB.setOnClickListener(viewClicked -> updateSolarOutput());

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateStatusViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (null != panelScanDisposable) {
            panelScanDisposable.dispose();
        }
        if (null != solarOutputDisposable) {
            solarOutputDisposable.dispose();
        }
    }
    //endregion

    private void updateStatusViews() {

        if (null != panelScanDisposable) {
            // scanning for nearby panels

            refreshProgressBar.setVisibility(VISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(INVISIBLE);

            mainTextView.setText(getString(R.string.finding_nearby_solar_panel));
        } else if (null != solarOutputDisposable) {
            // loading solar output

            refreshProgressBar.setVisibility(VISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(VISIBLE);

            mainTextView.setText(getString(R.string.loading_solar_output));
            detailTextView.setText("solar panel (" + getSolarCustomerId().get() + ")");
        } else if (!getSolarCustomerId().isSet()) {
            // no customerId set

            refreshProgressBar.setVisibility(INVISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(INVISIBLE);

            mainTextView.setText(getString(R.string.click_to_find_nearby_solar_panel));
        } else if (null != currentPowerOutput) {
            // existing wattage available

            refreshProgressBar.setVisibility(INVISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(VISIBLE);

            StringBuilder sbuf = new StringBuilder()
                    .append("current: ")
                    .append(currentPowerOutput.getCurrentPowerInWatts())
                    .append(" watts, ")

                    .append("lifetime: ")
                    .append(currentPowerOutput.getLifeTimeEnergyInWattHours())
                    .append(" wattHours.");

            mainTextView.setText(sbuf.toString());
            detailTextView.setText("solar panel (" + getSolarCustomerId().get() + ")");
        } else {
            // wattage not available

            refreshProgressBar.setVisibility(INVISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(VISIBLE);

            mainTextView.setText(getString(R.string.click_to_load_solar_output));
            detailTextView.setText("solar panel (" + getSolarCustomerId().get() + ")");
        }

        if (getSolarCustomerId().isSet()) {
            solarUpdateFAB.setVisibility(VISIBLE);
        } else {
            solarUpdateFAB.setVisibility(INVISIBLE);
        }
    }

    private void scanForNearbyPanels() {

        // NJD TODO - shoudl pass in Eddystone Beacon Namespace for our App here.. just
        // so the panelProvider is really generic... so it's not so bad when we swap it out
        // for mock testing.
        panelScanProvider.scanForNearbyPanel().subscribe(new Observer<PanelInfo>() {
            @Override
            public void onSubscribe(Disposable d) {
                MainActivity.this.panelScanDisposable = panelScanDisposable;
                updateStatusViews();
            }

            @Override
            public void onNext(PanelInfo panelInfo) {
                if (panelInfo.getCustomerId().isPresent()) {
                    Log.d(TAG, "Panel found with customerId configured.");

                    SolarMonitorApp.getInstance().setSolarCustomerId(panelInfo.getCustomerId().get());

                    MainActivity.this.panelScanDisposable = null;
                    MainActivity.this.currentPowerOutput = null;

                    updateStatusViews();
                } else {
                    Log.d(TAG, "Panel found, but it needs to be configured.");
                }
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(MainActivity.this, getString(R.string.error_please_try_again), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
                if (null != panelScanDisposable) {
                    panelScanDisposable.dispose();
                }

                panelScanDisposable = null;
            }
        });
    }

    private void updateSolarOutput() {
        if (getSolarCustomerId().isSet()) {

            solarOutputProvider.getSolarOutput(getSolarCustomerId().get()).subscribe(new SingleObserver<PowerOutput>() {
                @Override
                public void onSubscribe(Disposable solarOutputDisposable) {
                    MainActivity.this.solarOutputDisposable = solarOutputDisposable;
                    updateStatusViews();
                }

                @Override
                public void onSuccess(PowerOutput currentPowerOutput) {
                    MainActivity.this.solarOutputDisposable = null;
                    MainActivity.this.currentPowerOutput = currentPowerOutput;

                    updateStatusViews();
                }

                @Override
                public void onError(Throwable e) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_please_try_again), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private Preference<String> getSolarCustomerId() {
        return SolarMonitorApp.getInstance().getSolarCustomerId();
    }
}
