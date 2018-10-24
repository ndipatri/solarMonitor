package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.MainActivityLiveUserTest
import com.ndipatri.solarmonitor.activities.MainActivityViewModel
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragment
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragmentViewModel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = arrayOf(ServiceModule::class, HardwareModule::class, ViewModelModule::class))
interface LiveTestObjectGraph : ObjectGraph {
    override fun inject(thingy: MainActivity)
    override fun inject(thingy: MainActivityViewModel)
    override fun inject(thingy: ConfigDialogFragmentViewModel)
    override fun inject(fragment: ConfigDialogFragment)
    override fun inject(thingy: PanelProvider)

    fun inject(userTest: MainActivityLiveUserTest)

    object Initializer {
        fun init(context: Context): LiveTestObjectGraph {
            return DaggerLiveTestObjectGraph.builder()
                    .serviceModule(ServiceModule(context))
                    .hardwareModule(HardwareModule(context))
                    .viewModelModule(ViewModelModule(context))
                    .build()
        }
    }
}
