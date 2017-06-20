package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.services.BluetoothService;

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
    BluetoothService providesBluetoothService(Context context) {
        return mock(BluetoothService.class);
    }
}
