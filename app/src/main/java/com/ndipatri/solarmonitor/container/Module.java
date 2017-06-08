package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.services.SolarOutputService;
import com.ndipatri.solarmonitor.services.SolarOutputServiceImpl;

import javax.inject.Singleton;

import dagger.Provides;

@dagger.Module
public class Module {

    Context context;

    public Module(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context providesContext() {
        return context;
    }

    @Provides
    SolarOutputService providesSolarOutputService() {
        return new SolarOutputServiceImpl();
    }
}
