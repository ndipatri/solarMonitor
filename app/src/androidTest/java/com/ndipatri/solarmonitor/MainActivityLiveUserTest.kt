package com.ndipatri.solarmonitor

import android.content.Intent
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.IdlingRegistry
import android.support.test.rule.ActivityTestRule
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.container.LiveTestObjectGraph
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.utils.RxJavaUsesAsyncTaskSchedulerRule
import com.ndipatri.solarmonitor.utils.AACUsesIdlingResourceRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class MainActivityLiveUserTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java, true, false)

    // This is the coolest thing ever.  We are configuring our test thread (this thread) to block
    // while the background thread is running in our target application. (only those background
    // operations that are using RxJava's IO and Computation schedulers, that is)
    //
    // This is necessary for this test when we are 'loading' solar output using SolarOutputProvider.
    // This Provider uses RxJava/Retrofit to retrieve solar output from our live RESTful endpoint.
    @Rule
    @JvmField
    var asyncTaskSchedulerRule = RxJavaUsesAsyncTaskSchedulerRule()

    @Rule
    @JvmField
    val executorRule = AACUsesIdlingResourceRule()

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
        // The PanelProvider actually does BLE scanning in a way that is non-blocking.  Our background
        // thread subscribes to hotObservable, so we need to wrap in IdlingRegistry.. RxJava thread
        // trickery isn't enough.
        IdlingRegistry.getInstance().register(panelProvider.idlingResource)

        clearState()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        clearState()
    }

    private fun clearState() {
        // clear out any remaining state.
        panelProvider.deleteAllPanels().subscribe()
        customerProvider.deleteAllCustomers().subscribe()
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
    //
    @Test
    @Throws(Exception::class)
    fun scanningAndLoading() {

        activityRule.launchActivity(Intent())

        // For Here we depend solely on the RxJavaUsesAsyncTaskSchedulerRule because our RxJava background process
        // is hitting the remote RESTful endpoint. (We don't need IdlingResource)

        // we know what panelId our real beacon is emitting.
        assertFindingPanelViews("480557")

        // we cannot predict the real solar output right now.
        assertLoadingSolarOutputViews("Current \\((.*?)/hour\\)\\, Lifetime\\((.*?)\\)", "480557")
    }
}
