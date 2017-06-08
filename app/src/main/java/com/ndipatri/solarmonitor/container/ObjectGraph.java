package com.ndipatri.solarmonitor.container;

import android.content.Context;

import com.ndipatri.solarmonitor.activities.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules={Module.class})
public interface ObjectGraph {
    void inject(MainActivity activity);

    final class Initializer {
        public static ObjectGraph init(Context context) {
            return DaggerObjectGraph.builder()
                    .module(new Module(context))
                    .build();
        }
    }
}
