package com.ndipatri.solarmonitor.activities;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment;
import com.ndipatri.solarmonitor.fragments.EnableBluetoothDialogFragment;
import com.ndipatri.solarmonitor.fragments.GrantFineLocationAccessDialogFragment;
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    @Inject
    protected PanelScanProvider panelScanProvider;

    protected EnableBluetoothDialogFragment enableBluetoothDialogFragment;
    protected GrantFineLocationAccessDialogFragment grantFineLocationAccessDialogFragment;

    private Disposable bluetoothStateChangeDisposable;
    private Disposable userRequestTimeoutDisposable;

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
        } else if (id == R.id.action_configure) {
            launchConfigureFragment();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

    protected void launchConfigureFragment() {
        ConfigurePanelDialogFragment dialog = new ConfigurePanelDialogFragment();
        dialog.show(getSupportFragmentManager().beginTransaction(), "configure panel dialog");
        getSupportFragmentManager().executePendingTransactions();
        dialog.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handleConfigureFragmentDismiss();
            }
        });
    }

    protected void handleConfigureFragmentDismiss() {}

    @Override
    protected void onResume() {
        super.onResume();

        beginUserPermissionCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (null != bluetoothStateChangeDisposable) {
            bluetoothStateChangeDisposable.dispose();
        }

        if (null != userRequestTimeoutDisposable) {
            userRequestTimeoutDisposable.dispose();
        }
    }

    private Disposable registerForBluetoothStateChangeBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);

        return RxBroadcast.fromBroadcast(this, filter).subscribe(intent -> {
            if (null != enableBluetoothDialogFragment) {
                beginUserPermissionCheck();
            }
        });
    }

    private void beginUserPermissionCheck() {
        // NJD TODO - need to get this sorted for lower than M devices...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                grantFineLocationAccessDialogFragment = new GrantFineLocationAccessDialogFragment();
                grantFineLocationAccessDialogFragment.show(getSupportFragmentManager().beginTransaction(), "grant location access dialog");
            } else {
                continueWithUserPermissionCheck();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch ((short) requestCode) {
            case GrantFineLocationAccessDialogFragment.PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                    continueWithUserPermissionCheck();
                } else {
                    Toast.makeText(this, "This application cannot run without Fine Location Access!", Toast.LENGTH_SHORT).show();
                    shutdownServices();
                }
                return;
            }
        }
    }

    private void continueWithUserPermissionCheck() {
        if (!panelScanProvider.isBluetoothSupported()) {
            Toast.makeText(this, "This application cannot run without Bluetooth support!", Toast.LENGTH_SHORT).show();
            shutdownServices();
        } else {
            if (!panelScanProvider.isBluetoothEnabled()) {
                enableBluetoothDialogFragment = new EnableBluetoothDialogFragment();
                enableBluetoothDialogFragment.show(getSupportFragmentManager().beginTransaction(), "enable bluetooth dialog");
            } else {
                finishUserPermissionCheck();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EnableBluetoothDialogFragment.REQUEST_ENABLE_BT:
                enableBluetoothDialogFragment = null;

                if (resultCode == RESULT_OK) {
                    finishUserPermissionCheck();
                } else {
                    Toast.makeText(this, "This application cannot run without Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                    shutdownServices();
                }
                break;
        }
    }

    private void shutdownServices() {

        // NJD TODO - stop any other background services here.

        finish();
    }

    protected void finishUserPermissionCheck() {
        bluetoothStateChangeDisposable = registerForBluetoothStateChangeBroadcast();

        // NJD TODO - start any other background services here...
    }
}
