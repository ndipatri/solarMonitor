package com.ndipatri.solarmonitor.services;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

public class BluetoothService {

    public BluetoothService(Context context){}

    public Single<String> getNearbySolarCustomerId() {
        return Single.create(subscriber -> {

            // NJD TODO - Placeholder for future work.
            // Presumably, this would look for a nearby beacon and read
            // the nearby Solar Customer Id.. For now, just load the adapter
            // so we use the hardware.. and then return canned data.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            subscriber.onSuccess("480557");
        });
    }
}
