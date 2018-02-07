package com.ndipatri.solarmonitor


import android.app.Application
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.solarmonitor.container.ObjectGraph

class SolarMonitorApp : Application() {

    init {
        instance = this
    }

    val objectGraph = ObjectGraph.Initializer.init(this)

    private lateinit var sharedPreferences: RxSharedPreferences

    val solarCustomerId: Preference<String>
        get() = sharedPreferences.getString("SOLAR_CUSTOMER_ID")

    fun setSolarCustomerId(newSolarCustomerId: String) {
        solarCustomerId.set(newSolarCustomerId)
    }

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(this))
        GoogleProximity.initialize(this, false)
    }

    companion object {
        lateinit var instance: SolarMonitorApp
            private set
    }
}
