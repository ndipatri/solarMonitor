package com.ndipatri.solarmonitor.container

import android.content.Context
import com.ndipatri.solarmonitor.MainActivityMockTest
import com.ndipatri.solarmonitor.MainActivityUITest
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.activities.MainActivityViewModel
import com.ndipatri.solarmonitor.container.modules.MockHardwareModule
import com.ndipatri.solarmonitor.container.modules.MockViewModelModule
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragment
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragmentViewModel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import dagger.Component
import javax.inject.Singleton

// NJD TODO - can we get rid of ServiceModule here?
@Singleton
@Component(modules = arrayOf(ServiceModule::class, HardwareModule::class, MockViewModelModule::class))
interface UITestObjectGraph : ObjectGraph {
    override fun inject(thingy: MainActivity)
    override fun inject(thingy: MainActivityViewModel)
    override fun inject(thingy: ConfigDialogFragmentViewModel)
    override fun inject(fragment: ConfigDialogFragment)
    override fun inject(thingy: PanelProvider)

    fun inject(test: MainActivityMockTest)
    fun inject(test: MainActivityUITest)

    object Initializer {
        fun init(context: Context): UITestObjectGraph {
            return DaggerUITestObjectGraph.builder()
                    .serviceModule(ServiceModule(context))
                    .hardwareModule(HardwareModule(context))
                    .mockViewModelModule(MockViewModelModule(context))
                    .build()
        }
    }
}
