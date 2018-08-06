package com.ndipatri.solarmonitor

import android.content.Intent
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.IdlingRegistry
import android.support.test.rule.ActivityTestRule
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.container.MockTestObjectGraph
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
import com.ndipatri.solarmonitor.utils.TaskExecutorWithIdlingResourceRule
import io.reactivex.Maybe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import java.net.MalformedURLException
import javax.inject.Inject

class MainActivityMockTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java, true, false)

    // This is the coolest thing ever.  We are configuring our test thread (this thread) to block
    // while the background thread is running in our target application. (only those background
    // operations that are using RxJava's IO and Computation schedulers, that is)
    //
    // This is necessary for this test when we are 'loading' solar output using SolarOutputProvider.
    // This Provider uses RxJava/Retrofit to retrieve solar output from mockWebServer RESTful endpoint.
    @Rule
    @JvmField
    var asyncTaskSchedulerRule = AsyncTaskSchedulerRule()

    @Rule
    @JvmField
    val executorRule = TaskExecutorWithIdlingResourceRule()

    @Inject
    lateinit var solarOutputProvider: SolarOutputProvider

    @Inject
    lateinit var panelProvider: PanelProvider

    @Inject
    lateinit var customerProvider: CustomerProvider

    lateinit var solarMonitorApp: SolarMonitorApp

    lateinit var mockPanelDesc: String
    lateinit var mockPanelId: String

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Context of the app under test.
        solarMonitorApp = getInstrumentation().targetContext.applicationContext as SolarMonitorApp
        solarMonitorApp.shouldCheckForHardwarePermissions = false

        // With this ObjectGraph, we mock our hardware dependency. (e.g. bluetooth)
        val mockTestObjectGraph = MockTestObjectGraph.Initializer.init(solarMonitorApp)
        solarMonitorApp.objectGraph = mockTestObjectGraph
        mockTestObjectGraph.inject(this)

        // For the IdlingResource feature, we need to instrument the real component, unfortunately.
        // This is necessary as this provider has an undisclosed background mechanism so we need
        // to wrap the call in IdlingRegistry code to be safe.
        IdlingRegistry.getInstance().register((customerProvider.idlingResource))

        clearState()

        // Now that the hardware layer has been mocked, configure it...
        mockPanelDesc = "nicks panel"
        mockPanelId = "998877"
        configureMockHardware(mockPanelDesc, mockPanelId)

        // Configure MockWebServer to provide mock RESTful endpoint, thus mocking our
        // other external dependency
        val mockSolarOutput = 1230.0
        val mockLifetimeOutput = 4560.0
        configureMockEndpoint(mockSolarOutput, mockLifetimeOutput, mockPanelId, solarOutputProvider!!.apiKey)
    }

    private fun clearState() {
        // clear out any remaining state.
        panelProvider.deleteAllPanels()
        customerProvider.deleteAllCustomers()
    }

    // Here we are injecting a 'mock' ObjectGraph which gives us the chance to mock out
    // some hardware components that our app depends upon.  The service layer in this
    // mock ObjectGraph is still the real implementation and therefore needs the MockWebServer.
    //
    // Pros - Allows us to run repeatable tests.  Allows us to run tests on a device that might not
    // have all required production components (e.g. emulators do not have Bluetooth).
    // This is useful for IOT testing.
    //
    // Cons - Can be incorrectly used to replace proper unit testing.  Unit tests are a
    // much faster way to test front-end components.
    @Test
    @Throws(Exception::class)
    fun scanningAndLoading() {

        activityRule.launchActivity(Intent())

        // we know what panelId our real beacon is emitting.
        assertFindingPanelViews(mockPanelId)

        // we cannot predict the real solar output right now.
        assertLoadingSolarOutputViews("Current \\(\\$0\\.17\\/hour\\)\\, Lifetime\\(\\$0\\.62\\)", mockPanelId)
    }

    @Throws(MalformedURLException::class)
    private fun configureMockEndpoint(currentPowerValue: Double?, lifetimeEnergyValue: Double?, solarCustomerId: String, solarApiKey: String) {

        val mockOverview = Overview().apply {
            currentPower = CurrentPower().apply { power = currentPowerValue }
            lifeTimeData = LifeTimeData().apply { energy = lifetimeEnergyValue }
        }

        val getOverviewResponse = GetOverviewResponse().apply { overview = mockOverview }

        // We deploy a MockWebServer to the same virtual machine as our
        // target APK
        val mockSolarOutputServer = MockSolarOutputServer().apply {
                enqueueDesiredSolarOutputResponse(getOverviewResponse,
                                                  solarCustomerId,
                                                  solarApiKey)

                beginUsingMockServer()
        }

        // This is the only way in which our Test APK deviates from production.  We need to
        // point our service to the mock endpoint (mockWebServer)
        SolarOutputProvider.API_ENDPOINT_BASE_URL = mockSolarOutputServer.mockSolarOutputServerURL!!
    }

    private fun configureMockHardware(desiredPanelDesc: String, desiredPanelId: String) {

        `when`(panelProvider.getStoredPanel()).thenReturn(Maybe.create { subscriber ->
            subscriber.onComplete() // no stored panel
        })

        `when`(panelProvider.scanForNearbyPanel()).thenReturn(Maybe.create { subscriber ->
            val panelInfo = Panel(desiredPanelId, desiredPanelDesc, "Customer ${desiredPanelId}")

            subscriber.onSuccess(panelInfo)
        })
    }
}
