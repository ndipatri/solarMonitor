package com.ndipatri.solarmonitor.services;


import android.content.Context;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

public class BluetoothService {

    public BluetoothService(Context context){}

    public Single<String> getSomething() {
        return Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> subscriber) throws Exception {
                // NJD TODO - actually do something here...
                subscriber.onSuccess("bluetooth found!");
            }
        });
    }
}
