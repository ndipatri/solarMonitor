package com.ndipatri.solarmonitor.services;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BluetoothService {

    public static final int NEARBY_PANEL_SCAN_TIMEOUT_SECONDS = 10;

    public BluetoothService(Context context){}

    public Single<String> searchForNearbyPanels() {
        return Single.create((SingleEmitter<String> subscriber) -> {

            // NJD TODO - Placeholder for future work.
            // Presumably, this would look for a nearby beacon and read
            // the nearby Solar Customer Id.. For now, just load the adapter
            // so we use the hardware.. and then return canned data.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            subscriber.onSuccess("480557");
        })

        // NJD TODO - remove when real BT scanning is done...
        .delay(5000, TimeUnit.MILLISECONDS) // simulate scan delay until we actually do bluetooth

        .timeout(NEARBY_PANEL_SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }
}
