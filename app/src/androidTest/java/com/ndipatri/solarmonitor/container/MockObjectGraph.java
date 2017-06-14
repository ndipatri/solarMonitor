package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.MainActivityInstrumentationTest;
import com.ndipatri.solarmonitor.activities.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules={ServiceModule.class,
                    MockHardwareModule.class})
public interface MockObjectGraph extends ObjectGraph {
    void inject(MainActivity activity);
    void inject(MainActivityInstrumentationTest test);

    final class Initializer {
        public static MockObjectGraph init(Context context) {
            return DaggerMockObjectGraph.builder()
                    .serviceModule(new ServiceModule(context))
                    .mockHardwareModule(new MockHardwareModule(context))
                    .build();
        }
    }
}
