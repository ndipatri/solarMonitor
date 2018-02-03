package com.ndipatri.solarmonitor


import android.app.Application
import android.preference.PreferenceManager
import android.support.test.espresso.IdlingResource

import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.solarmonitor.container.ObjectGraph
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo

class SolarMonitorApp : Application() {

    var objectGraph: ObjectGraph? = null
        get() {
            if (field == null) {
                this.objectGraph = ObjectGraph.Initializer.init(this)
            }

            return field
        }

    private var sharedPreferences: RxSharedPreferences? = null

    val solarCustomerId: Preference<String>
        get() = sharedPreferences!!.getString("SOLAR_CUSTOMER_ID")

    init {
        SolarMonitorApp.instance = this
    }

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(this))

        GoogleProximity.initialize(this, false)
    }

    fun setSolarCustomerId(solarCustomerId: String) {
        solarCustomerId.set(solarCustomerId)
    }

    companion object {

        var instance: SolarMonitorApp? = null
            private set
    }
}
