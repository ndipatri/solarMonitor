package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider

import javax.inject.Singleton

import dagger.Provides

import org.mockito.Mockito.mock

@dagger.Module
class MockHardwareModule(internal var context: Context) {

    @Provides
    @Singleton
    internal fun providesBluetoothService(context: Context): PanelScanProvider {
        return mock(PanelScanProvider::class.java)
    }
}
