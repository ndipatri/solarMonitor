package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.services.SolarOutputService;
import com.ndipatri.solarmonitor.services.SolarOutputServiceImpl;

import javax.inject.Singleton;

import dagger.Provides;

import static org.mockito.Mockito.mock;

@dagger.Module
public class TestModule {

    Context context;

    public TestModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context providesContext() {
        return context;
    }

    @Provides
    @Singleton
    SolarOutputService providesSolarOutputService() {
        return mock(SolarOutputServiceImpl.class);
    }
}
