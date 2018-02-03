package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.container.DaggerObjectGraph
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = arrayOf(ServiceModule::class, HardwareModule::class))
interface ObjectGraph {
    fun inject(thingy: MainActivity)
    fun inject(fragment: ConfigurePanelDialogFragment)

    object Initializer {
        fun init(context: Context): ObjectGraph {
            return DaggerObjectGraph.builder()
                    .serviceModule(ServiceModule(context))
                    .hardwareModule(HardwareModule(context))
                    .build()
        }
    }
}
