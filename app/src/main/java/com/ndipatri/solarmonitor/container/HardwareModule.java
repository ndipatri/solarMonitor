package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.services.BluetoothService;

import javax.inject.Singleton;

import dagger.Provides;

@dagger.Module
public class HardwareModule {

    Context context;

    public HardwareModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    BluetoothService providesBluetoothService(Context context) {

        return new BluetoothService(context);
    }
}
