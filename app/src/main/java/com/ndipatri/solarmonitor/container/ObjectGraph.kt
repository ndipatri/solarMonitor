package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.container.DaggerObjectGraph
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.viewModels.ConfigurePanelViewModel
import com.ndipatri.solarmonitor.viewModels.MainActivityViewModel

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = arrayOf(ServiceModule::class, HardwareModule::class))
interface ObjectGraph {
    fun inject(thingy: MainActivityViewModel)
    fun inject(thingy: ConfigurePanelViewModel)
    fun inject(fragment: ConfigurePanelDialogFragment)
    fun inject(thingy: PanelProvider)

    object Initializer {
        fun init(context: Context): ObjectGraph {
            return DaggerObjectGraph.builder()
                    .serviceModule(ServiceModule(context))
                    .hardwareModule(HardwareModule(context))
                    .build()
        }
    }
}
