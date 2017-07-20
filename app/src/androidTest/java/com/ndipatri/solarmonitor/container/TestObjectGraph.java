package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.activities.MainActivity;
import com.ndipatri.solarmonitor.MainActivityInstrumentationTest;
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules={ServiceModule.class,
                    HardwareModule.class})
public interface TestObjectGraph extends ObjectGraph {
    void inject(ConfigurePanelDialogFragment fragment);
    void inject(MainActivity activity);
    void inject(MainActivityInstrumentationTest test);

    final class Initializer {
        public static TestObjectGraph init(Context context) {
            return DaggerTestObjectGraph.builder()
                    .serviceModule(new ServiceModule(context))
                    .hardwareModule(new HardwareModule(context))
                    .build();
        }
    }
}
