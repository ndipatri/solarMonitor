package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;

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
    PanelScanProvider providesBluetoothService(Context context) {
        return new PanelScanProvider(context);
    }
}
