package com.ndipatri.solarmonitor.container

import android.content.Context
import com.ndipatri.solarmonitor.MainActivityMockUserTest
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.activities.MainActivityViewModel
import com.ndipatri.solarmonitor.container.modules.MockHardwareModule
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragment
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragmentViewModel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(ServiceModule::class, MockHardwareModule::class, ViewModelModule::class))
interface MockTestObjectGraph : ObjectGraph {
    override fun inject(thingy: MainActivity)
    override fun inject(thingy: MainActivityViewModel)
    override fun inject(thingy: ConfigDialogFragmentViewModel)
    override fun inject(fragment: ConfigDialogFragment)
    override fun inject(thingy: PanelProvider)

    fun inject(userTest: MainActivityMockUserTest)

    object Initializer {
        fun init(context: Context): MockTestObjectGraph {
            return DaggerMockTestObjectGraph.builder()
                    .serviceModule(ServiceModule(context))
                    .mockHardwareModule(MockHardwareModule(context))
                    .viewModelModule(ViewModelModule(context))
                    .build()
        }
    }
}
