package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider

import javax.inject.Singleton

import dagger.Provides

@dagger.Module
class HardwareModule(internal var context: Context) {

    @Provides
    @Singleton
    internal fun providesBluetoothService(context: Context): PanelProvider {
        return PanelProvider(context)
    }
}
