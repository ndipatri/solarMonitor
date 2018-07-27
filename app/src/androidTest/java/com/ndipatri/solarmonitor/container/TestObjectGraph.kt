package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.MainActivityInstrumentationTest
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragment

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = arrayOf(ServiceModule::class, HardwareModule::class))
interface TestObjectGraph : ObjectGraph {
    override fun inject(fragment: ConfigDialogFragment)
    override fun inject(activity: MainActivity)
    fun inject(test: MainActivityInstrumentationTest)

    object Initializer {
        fun init(context: Context): TestObjectGraph {
            return DaggerTestObjectGraph.builder()
                    .serviceModule(ServiceModule(context))
                    .hardwareModule(HardwareModule(context))
                    .build()
        }
    }
}
