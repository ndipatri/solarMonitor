package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;

import javax.inject.Singleton;

import dagger.Provides;

import static org.mockito.Mockito.mock;

@dagger.Module
public class MockHardwareModule {

    Context context;

    public MockHardwareModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    PanelScanProvider providesBluetoothService(Context context) {
        return mock(PanelScanProvider.class);
    }
}
