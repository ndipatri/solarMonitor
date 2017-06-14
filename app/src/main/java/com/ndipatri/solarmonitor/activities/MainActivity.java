package com.ndipatri.solarmonitor.activities;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.services.BluetoothService;
import com.ndipatri.solarmonitor.services.SolarOutputService;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    protected @Inject SolarOutputService solarOutputService;
    protected @Inject BluetoothService bluetoothService;

    protected @BindView(R.id.refreshProgressBar) ProgressBar refreshProgressBar;
    protected @BindView(R.id.dialogTextView) TextView dialogTextView;
    protected @BindView(R.id.bluetoothStatus) TextView bluetoothStatus;

    private Disposable bluetoothStatusDisposable;
    private Disposable solarOutputDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((SolarMonitorApp) getApplication()).getObjectGraph().inject(this);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        refreshProgressBar.setVisibility(View.INVISIBLE);
        dialogTextView.setText("Click to load Solar Output ...");

        updateBluetoothStatus();

        findViewById(R.id.fab).setOnClickListener(viewClicked -> {
            Snackbar.make(viewClicked, "Retrieving latest solar output ...", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            refreshProgressBar.setVisibility(View.VISIBLE);
            dialogTextView.setVisibility(View.INVISIBLE);
            updateSolarOutput();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateBluetoothStatus();
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

    private void updateBluetoothStatus() {
        bluetoothService.getSomething().subscribe(new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable bluetoothStatusDisposable) {
                MainActivity.this.bluetoothStatusDisposable = bluetoothStatusDisposable;
            }

            @Override
            public void onSuccess(String status) {
                bluetoothStatus.setText(status);
                MainActivity.this.bluetoothStatusDisposable = null;
            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    private void updateSolarOutput() {
        solarOutputService.getSolarOutputInWatts(SolarMonitorApp.getInstance().getSolarCustomerId()).subscribe(new SingleObserver<Double>() {
            @Override
            public void onSubscribe(Disposable d) {
                MainActivity.this.solarOutputDisposable = solarOutputDisposable;
            }

            @Override
            public void onSuccess(Double solarOutputInWatts) {
                refreshProgressBar.setVisibility(View.INVISIBLE);

                dialogTextView.setVisibility(View.VISIBLE);

                String outputString = solarOutputInWatts.toString() + " watts";
                dialogTextView.setText(outputString);

                MainActivity.this.solarOutputDisposable = null;
            }

            @Override
            public void onError(Throwable e) {
                // we're not going to worry about this for now.
            }
        });
    }
}
