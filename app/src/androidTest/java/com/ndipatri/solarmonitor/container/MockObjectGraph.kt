package com.ndipatri.solarmonitor.container

import android.content.Context

import com.ndipatri.solarmonitor.MainActivityInstrumentationTest
import com.ndipatri.solarmonitor.activities.MainActivity

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = arrayOf(ServiceModule::class, MockHardwareModule::class))
interface MockObjectGraph : ObjectGraph {
    override fun inject(activity: MainActivity)
    fun inject(test: MainActivityInstrumentationTest)

    object Initializer {
        fun init(context: Context): MockObjectGraph {
            return DaggerMockObjectGraph.builder()
                    .serviceModule(ServiceModule(context))
                    .mockHardwareModule(MockHardwareModule(context))
                    .build()
        }
    }
}
