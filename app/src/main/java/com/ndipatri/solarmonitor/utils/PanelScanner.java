package com.ndipatri.solarmonitor.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

public class PanelScanner {

    private static final String TAG = PanelScanner.class.getSimpleName();

    protected BeaconManager beaconManager;

    protected Context context;

    private boolean isConnectedToBeaconService = false;
    private boolean isScanning = false;

    private Region scanRegion;

    public PanelScanner(Context context) {
        this.context = context;

        initializeRegion();
        initializeBeaconManager();
    }

    protected void initializeBeaconManager() {

        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            BluetoothAdapter mBluetoothAdapter = null;
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            mBluetoothAdapter.enable();

            beaconManager = BeaconManager.getInstanceForApplication(context);
            beaconManager.setForegroundScanPeriod(5000);
            beaconManager.setBackgroundScanPeriod(5000);

            // Detect the main identifier (UID) frame:
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));

            beaconManager.bind(beaconConsumer);
        }
    }

    private void initializeRegion() {
        Identifier nicksBeaconNamespaceId = Identifier.parse(context.getResources().getString(R.string.beaconNamespaceId));
        scanRegion = new Region("nicks-beacon-region", nicksBeaconNamespaceId, null, null);
    }

    public void startBeaconDiscovery() {
        Log.d(TAG, "Starting AltBeacon discovery...");

        isScanning = true;

        // if not connected to service, scanning will start once
        // we are connected...
        if (isConnectedToBeaconService) {
            startAltBeaconMonitoring();
        }
    }

    public void stopBeaconDiscovery() {
        Log.d(TAG, "Stopping AltBeacon discovery...");

        beaconManager.removeAllMonitorNotifiers();
        try {
            if (beaconManager.isBound(beaconConsumer)) {
                beaconManager.stopMonitoringBeaconsInRegion(scanRegion);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "BLE scan service not yet bound.", e);
        }

        isScanning = false;
    }

    private void startAltBeaconMonitoring() {
        beaconManager.addMonitorNotifier(monitorNotifier);
        try {
            beaconManager.startMonitoringBeaconsInRegion(scanRegion);
        } catch (RemoteException e) {
            Log.e(TAG, "BLE scan service not yet bound.", e);
        }
    }

    private BeaconConsumer beaconConsumer = new BeaconConsumer() {
        @Override
        public void onBeaconServiceConnect() {
            Log.d(TAG, "onBeaconServiceConnected(): Connected!");

            isConnectedToBeaconService = true;

            if (isScanning && beaconManager.getMonitoringNotifiers().isEmpty()) {
                // we're supposed to be monitoring but we had to wake for
                // service connection... so start monitoring now.
                startAltBeaconMonitoring();
            }
        }

        @Override
        public Context getApplicationContext() {
            return SolarMonitorApp.getInstance().getApplicationContext();
        }

        @Override
        public void unbindService(ServiceConnection serviceConnection) {
            SolarMonitorApp.getInstance().getApplicationContext().unbindService(serviceConnection);
        }

        @Override
        public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
            return SolarMonitorApp.getInstance().getApplicationContext().bindService(intent, serviceConnection, i);
        }
    };

    private MonitorNotifier monitorNotifier = new MonitorNotifier() {
        @Override
        public void didEnterRegion(Region region) {
            Log.d(TAG, "Region entered= '" + region + "'.");

            // Start ranging this beacon....
            regionEntered(region);
        }

        @Override
        public void didExitRegion(Region region) {
            Log.d(TAG, "Region exited= '" + region + "'.");
        }

        @Override
        public void didDetermineStateForRegion(int regionState, Region region) {
            if (regionState == MonitorNotifier.INSIDE) {
                regionEntered(region);
            }
        }

    };

    private void regionEntered(Region region) {

        Identifier instanceId = region.getId2();
        String instanceIdString = instanceId.toString();

    }
}


