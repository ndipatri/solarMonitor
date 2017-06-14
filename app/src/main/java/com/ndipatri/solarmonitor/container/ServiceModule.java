package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.services.SolarOutputService;

import javax.inject.Singleton;

import dagger.Provides;

@dagger.Module
public class ServiceModule {

    Context context;

    public ServiceModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context providesContext() {
        return context;
    }

    @Provides
    SolarOutputService providesSolarOutputService(Context context) {

        String apiKey = context.getResources().getString(R.string.solarEdgeApiKey);

        return new SolarOutputService(apiKey);
    }
}
