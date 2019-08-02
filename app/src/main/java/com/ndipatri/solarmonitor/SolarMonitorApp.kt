package com.ndipatri.solarmonitor


import android.app.Application
import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.solarmonitor.container.ObjectGraph

open class SolarMonitorApp : Application() {

    init {
        instance = this
    }

    var objectGraph = ObjectGraph.Initializer.init(this)

    var shouldCheckForBluetoothPermissions = true

    override fun onCreate() {
        super.onCreate()

        GoogleProximity.initialize(this, false)
    }

    companion object {
        lateinit var instance: SolarMonitorApp
    }
}
