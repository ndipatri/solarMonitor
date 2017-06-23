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

import com.f2prateek.rx.preferences2.Preference;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.services.BluetoothService;
import com.ndipatri.solarmonitor.services.SolarOutputService;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {

    @Inject SolarOutputService solarOutputService;
    @Inject BluetoothService bluetoothService;

    @BindView(R.id.refreshProgressBar) ProgressBar refreshProgressBar;
    @BindView(R.id.mainTextView) TextView mainTextView;
    @BindView(R.id.detailTextView) TextView detailTextView;
    @BindView(R.id.solarUpdateFAB) FloatingActionButton solarUpdateFAB;
    @BindView(R.id.beaconScanFAB) FloatingActionButton beaconScanFAB;

    private Disposable bluetoothStatusDisposable;
    private Disposable solarOutputDisposable;

    private Double currentWattage;

    public MainActivity() {
        SolarMonitorApp.getInstance().getObjectGraph().inject(this);
    }

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
        }

        return super.onOptionsItemSelected(item);
    }

    public void setSolarOutputService(SolarOutputService solarOutputService) {
        this.solarOutputService = solarOutputService;
    }

    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        solarUpdateFAB.setOnClickListener(viewClicked -> updateSolarOutput());
        beaconScanFAB.setOnClickListener(viewClicked -> scanForNearbyPanels());

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateStatusViews();

        /**
        Snackbar.make(refreshProgressBar, getString(R.string.welcome_back), Snackbar.LENGTH_SHORT)
                .setAction("Action", null).show();
         **/

        updateSolarOutput();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (null != bluetoothStatusDisposable) {
            bluetoothStatusDisposable.dispose();
        }
        if (null != solarOutputDisposable) {
            solarOutputDisposable.dispose();
        }
    }

    private void updateStatusViews() {

        if (null != bluetoothStatusDisposable) {
            // scanning for nearby panels

            refreshProgressBar.setVisibility(VISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(INVISIBLE);

            mainTextView.setText(getString(R.string.finding_nearby_solar_panels));
        } else
        if (null != solarOutputDisposable) {
            // loading solar output

            refreshProgressBar.setVisibility(VISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(VISIBLE);

            mainTextView.setText(getString(R.string.retrieving_solar_output));
            detailTextView.setText("solar customer (" + getSolarCustomerId().get() + ")");
        } else
        if (!getSolarCustomerId().isSet()) {
            // no customerId set

            refreshProgressBar.setVisibility(INVISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(INVISIBLE);

            mainTextView.setText(getString(R.string.click_to_find_nearby_solar_panels));
        } else
        if (null != currentWattage) {
            // existing wattage available

            refreshProgressBar.setVisibility(INVISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(VISIBLE);

            String outputString = currentWattage.toString() + " watts";
            mainTextView.setText(outputString);
            detailTextView.setText("solar customer (" + getSolarCustomerId().get() + ")");
        } else {
            // wattage not available

            refreshProgressBar.setVisibility(INVISIBLE);
            mainTextView.setVisibility(VISIBLE);
            detailTextView.setVisibility(VISIBLE);

            mainTextView.setText(getString(R.string.click_to_load_solar_output));
            detailTextView.setText("solar customer (" + getSolarCustomerId().get() + ")");
        }

        if (getSolarCustomerId().isSet()) {
            solarUpdateFAB.setVisibility(VISIBLE);
        } else {
            solarUpdateFAB.setVisibility(INVISIBLE);
        }
    }

    private void scanForNearbyPanels() {

        bluetoothService.searchForNearbyPanels().subscribe(new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable bluetoothStatusDisposable) {
                MainActivity.this.bluetoothStatusDisposable = bluetoothStatusDisposable;
                updateStatusViews();
            }

            @Override
            public void onSuccess(String foundCustomerId) {
                SolarMonitorApp.getInstance().setSolarCustomerId(foundCustomerId);

                MainActivity.this.bluetoothStatusDisposable = null;
                MainActivity.this.currentWattage = null;

                updateStatusViews();
            }

            @Override
            public void onError(Throwable e) {
                Log.e("MainActivity", "Exception while scanning for nearby solar panels.", e);
            }
        });
    }

    private void updateSolarOutput() {
        if (getSolarCustomerId().isSet()) {

            solarOutputService.getSolarOutputInWatts(getSolarCustomerId().get()).subscribe(new SingleObserver<Double>() {
                @Override
                public void onSubscribe(Disposable solarOutputDisposable) {
                    MainActivity.this.solarOutputDisposable = solarOutputDisposable;
                    updateStatusViews();
                }

                @Override
                public void onSuccess(Double solarOutputInWatts) {
                    MainActivity.this.solarOutputDisposable = null;
                    MainActivity.this.currentWattage = solarOutputInWatts;

                    updateStatusViews();
                }

                @Override
                public void onError(Throwable e) {
                    Log.e("MainActivity", "Exception while retrieving solar output.", e);
                }
            });
        }
    }

    private Preference<String> getSolarCustomerId() {
        return SolarMonitorApp.getInstance().getSolarCustomerId();
    }
}
