package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider

import javax.inject.Singleton

import dagger.Provides

@dagger.Module
class ServiceModule(internal var context: Context) {

    @Provides
    @Singleton
    internal fun providesContext(): Context {
        return context
    }

    @Provides
    internal fun providesSolarOutput(context: Context): SolarOutputProvider {

        val apiKey = context.resources.getString(R.string.solarEdgeApiKey)

        return SolarOutputProvider(apiKey)
    }

    @Provides
    @Singleton
    internal fun providesCustomer(context: Context): CustomerProvider {
        return CustomerProvider(context)
    }
}
