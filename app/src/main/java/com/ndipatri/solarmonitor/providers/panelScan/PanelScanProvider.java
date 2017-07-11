package com.ndipatri.solarmonitor.providers.panelScan;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.primitives.Bytes;
import com.ndipatri.iot.googleproximity.GoogleProximity;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.utils.PanelScanner;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.concurrent.TimeUnit;

import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class PanelScanProvider {

    private static final String TAG = PanelScanner.class.getSimpleName();

    public static final int NEARBY_PANEL_SCAN_TIMEOUT_SECONDS = 10;

    protected BeaconManager beaconManager;

    protected Context context;

    private final Subject<PanelInfo> beaconFoundSubject = PublishSubject.create();

    private boolean isConnectedToBeaconService = false;
    private boolean isScanning = false;

    private Region scanRegion;

    public PanelScanProvider(Context context) {
        this.context = context;

        initializeRegion();
        initializeBeaconManager();
    }

    public Observable<PanelInfo> scanForNearbyPanel() {

        startBeaconScanning();

        return beaconFoundSubject

            // comfort delay
            .delay(3000, TimeUnit.MILLISECONDS) // simulate scan delay until we actually do bluetooth

            .timeout(NEARBY_PANEL_SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
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

    private void startBeaconScanning() {
        Log.d(TAG, "Starting AltBeacon discovery...");

        isScanning = true;

        // if not connected to service, scanning will start once
        // we are connected...
        if (isConnectedToBeaconService) {
            startAltBeaconMonitoring();
        }
    }

    private void stopBeaconScanning() {
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
        Log.d(TAG, "Region entered '" + region + "'.");

        GoogleProximity.getInstance().retrieveAttachment(getAdvertiseId(region)).subscribe(new MaybeObserver<String[]>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onSuccess(String[] attachment) {
                Log.d(TAG, "Found attachment for region '" + region + "'.");

                beaconFoundSubject.onNext(new PanelInfo(attachment));

                stopBeaconScanning();
                beaconFoundSubject.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Error retrieving attachment for region '" + region + "'.  Doing nothing.", e);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "No attachment exists for region '" + region + "'.");

                beaconFoundSubject.onNext(new PanelInfo());

                stopBeaconScanning();
                beaconFoundSubject.onComplete();
            }
        });
    }

    public byte[] getAdvertiseId(Region region) {

        Identifier namespaceId = region.getId1();
        String namespaceIdHex = namespaceId.toHexString().substring(2);

        Identifier instanceId = region.getId2();
        String instanceIdHex = instanceId.toHexString().substring(2);

        byte[] namespaceBytes = new byte[0];
        try {
            namespaceBytes = Hex.decodeHex(namespaceIdHex.toCharArray());
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        byte[] instanceBytes = new byte[0];
        try {
            instanceBytes = Hex.decodeHex(instanceIdHex.toCharArray());
        } catch (DecoderException e) {
            e.printStackTrace();
        }

        return Bytes.concat(namespaceBytes, instanceBytes);
    }
}
