package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.R
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
    internal fun providesSolarOutputService(context: Context): SolarOutputProvider {

        val apiKey = context.resources.getString(R.string.solarEdgeApiKey)

        return SolarOutputProvider(apiKey)
    }
}
