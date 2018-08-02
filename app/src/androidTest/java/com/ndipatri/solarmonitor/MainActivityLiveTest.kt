package com.ndipatri.solarmonitor

import android.content.Intent
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.PositionAssertions.isAbove
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.container.LiveTestObjectGraph
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.CurrentPower
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.GetOverviewResponse
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.LifeTimeData
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.Overview
import com.ndipatri.solarmonitor.utils.AsyncTaskSchedulerRule
import com.ndipatri.solarmonitor.utils.MockSolarOutputServer
import io.reactivex.Maybe
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.endsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import java.net.MalformedURLException
import javax.inject.Inject

class MainActivityLiveTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java, true, false)

    // This is the coolest thing ever.  We are configuring our test thread (this thread) to block
    // while the background thread is running in our target application. (only those background
    // operations that are using RxJava's IO and Computation schedulers, that is)
    @Rule
    @JvmField
    var asyncTaskSchedulerRule = AsyncTaskSchedulerRule()

    @Inject
    lateinit var panelProvider: PanelProvider

    @Inject
    lateinit var customerProvider: CustomerProvider

    lateinit var solarMonitorApp: SolarMonitorApp

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Context of the app under test.
        solarMonitorApp = getInstrumentation().targetContext.applicationContext as SolarMonitorApp

        // We bootstrap the production ObjectGraph and inject it into this test class so we can access
        // production components
        val lifeTestObjectGraph = LiveTestObjectGraph.Initializer.init(solarMonitorApp)
        solarMonitorApp.objectGraph = lifeTestObjectGraph
        lifeTestObjectGraph.inject(this)

        // For the IdlingResource feature, we need to instrument the real component, unfortunately.
        IdlingRegistry.getInstance().register(customerProvider.idlingResource, panelProvider.idlingResource)

        clearState()
    }

    private fun clearState() {
        // clear out any remaining state.
        panelProvider.deleteAllPanels()
        customerProvider.deleteAllCustomers()
    }

    // Live Testing
    //
    // Here we use our production ObjectGraph with our real service and hardware layer.  This service
    // layer requires remote RESTful endpoints.  The hardware layer requires a real Bluetooth
    // stack to be present.
    //
    // Pros - no configuration, allows for automated 'live testing' and testing use real live system and data
    // Cons - live endpoint and hardware need to be in a known state.  If a test fails, your scope is so large
    // it doesn't really tell you much, necessarily, about your code itself.
    @Test
    @Throws(Exception::class)
    fun scanningAndLoading() {

        activityRule.launchActivity(Intent())

        // we know what panelId our real beacon is emitting.
        assertFindingPanelViews("480557")

        // we cannot predict the real solar output right now.
        //assertLoadingSolarOutputViews("Current (\$0.17/hour), Lifetime(\$0.62)", "480557")
    }
}
