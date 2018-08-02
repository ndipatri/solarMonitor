package com.ndipatri.solarmonitor.container

import android.arch.lifecycle.ViewModel
import android.content.Context
import com.ndipatri.solarmonitor.activities.MainActivity

import com.ndipatri.solarmonitor.fragments.ConfigDialogFragment
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragmentViewModel
import com.ndipatri.solarmonitor.activities.MainActivityViewModel

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = arrayOf(ServiceModule::class, HardwareModule::class, ViewModelModule::class))
interface ObjectGraph {
    fun inject(thingy: MainActivity)
    fun inject(thingy: MainActivityViewModel)
    fun inject(thingy: ConfigDialogFragmentViewModel)
    fun inject(fragment: ConfigDialogFragment)
    fun inject(thingy: PanelProvider)

    object Initializer {
        fun init(context: Context): ObjectGraph {
            return DaggerObjectGraph.builder()
                    .serviceModule(ServiceModule(context))
                    .hardwareModule(HardwareModule(context))
                    .viewModelModule(ViewModelModule(context))
                    .build()
        }
    }
}
