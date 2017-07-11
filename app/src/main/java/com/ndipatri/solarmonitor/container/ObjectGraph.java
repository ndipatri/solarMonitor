package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.activities.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules={ServiceModule.class,
                    HardwareModule.class})
public interface ObjectGraph {
    void inject(MainActivity thingy);

    final class Initializer {
        public static ObjectGraph init(Context context) {
            return DaggerObjectGraph.builder()
                    .serviceModule(new ServiceModule(context))
                    .hardwareModule(new HardwareModule(context))
                    .build();
        }
    }
}
