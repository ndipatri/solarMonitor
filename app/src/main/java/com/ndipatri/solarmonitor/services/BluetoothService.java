package com.ndipatri.solarmonitor.services;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

public class BluetoothService {

    public BluetoothService(Context context){}

    public Single<String> getSomething() {
        return Single.create(subscriber -> {

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            subscriber.onSuccess("real bluetooth found!");
        });
    }
}
