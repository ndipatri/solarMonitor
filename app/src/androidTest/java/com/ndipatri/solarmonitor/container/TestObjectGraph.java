package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.activities.MainActivity;
import com.ndipatri.solarmonitor.MainActivityInstrumentationTest;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules={TestModule.class})
public interface TestObjectGraph extends ObjectGraph {
    void inject(MainActivity activity);
    void inject(MainActivityInstrumentationTest test);

    final class Initializer {
        public static TestObjectGraph init(Context context) {
            return DaggerTestObjectGraph.builder()
                    .testModule(new TestModule(context))
                    .build();
        }
    }
}
