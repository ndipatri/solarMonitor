package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.activities.MainActivity;
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules={ServiceModule.class,
                    HardwareModule.class})
public interface ObjectGraph {
    void inject(MainActivity thingy);
    void inject(ConfigurePanelDialogFragment fragment);

    final class Initializer {
        public static ObjectGraph init(Context context) {
            return DaggerObjectGraph.builder()
                    .serviceModule(new ServiceModule(context))
                    .hardwareModule(new HardwareModule(context))
                    .build();
        }
    }
}
