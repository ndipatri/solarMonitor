package com.ndipatri.solarmonitor.services;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BluetoothService {

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

        .delay(1000, TimeUnit.MILLISECONDS) // simulate scan delay until we actually do bluetooth
        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }
}
