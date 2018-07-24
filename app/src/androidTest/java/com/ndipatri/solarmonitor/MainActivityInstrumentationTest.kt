package com.ndipatri.solarmonitor

import android.content.Intent
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.PositionAssertions.isAbove
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.container.MockObjectGraph
import com.ndipatri.solarmonitor.container.TestObjectGraph
import com.ndipatri.solarmonitor.mocks.MockSolarOutputServer
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.CurrentPower
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.GetOverviewResponse
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.LifeTimeData
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.Overview
import io.reactivex.Maybe
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.endsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import java.net.MalformedURLException
import javax.inject.Inject

class MainActivityInstrumentationTest {

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
    lateinit var solarOutputProvider: SolarOutputProvider

    @Inject
    lateinit var panelProvider: PanelProvider

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Context of the app under test.
        val solarMonitorApp = getInstrumentation().targetContext.applicationContext as SolarMonitorApp

        // clear out any remaining state.
        solarMonitorApp.solarCustomerId.delete()
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
    fun findingPanel_realExternalCollaborators() {

        // Context of the app under test.
        val solarMonitorApp = getInstrumentation().targetContext.applicationContext as SolarMonitorApp

        // We bootstrap the production ObjectGraph and inject it into this test class so we can access
        // production components
        val testObjectGraph = TestObjectGraph.Initializer.init(solarMonitorApp)
        solarMonitorApp.objectGraph = testObjectGraph
        testObjectGraph.inject(this)

        // For the IdlingResource feature, we need to instrument the real component, unfortunately.
        val idlingResource = panelProvider.idlingResource
        Espresso.registerIdlingResources(idlingResource)

        activityRule.launchActivity(Intent())

        // we know what panelId our real beacon is emitting.
        assertFindingPanelViews("480557")
    }

    @Test
    @Throws(Exception::class)
    fun loadingSolarOutput_realExternalCollaborators() {

        activityRule.launchActivity(Intent())

        // we cannot predict the real solar output right now.
        assertLoadingSolarOutputViews(null, null, "480557")
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
    fun loadingSolarOutput_mockExternalCollaborators() {

        // Context of the app under test.
        val solarMonitorApp = getInstrumentation().targetContext.applicationContext as SolarMonitorApp

        // We can bootstrap the target application with a MockObjectGraph(Dagger) which will use real service
        // layer but a mock hardware layer.
        val mockObjectGraph = MockObjectGraph.Initializer.init(solarMonitorApp)
        solarMonitorApp.objectGraph = mockObjectGraph
        mockObjectGraph.inject(this)

        // Now that the hardware layer has been mocked, configure it...
        val mockPanelDesc = "nicks panel"
        val mockPanelId = "998877"
        configureMockHardware(mockPanelDesc, mockPanelId)

        // Configure MockWebServer to provide mock RESTful endpoint
        val mockSolarOutput = 123.0
        val mockLifetimeOutput = 456.0
        configureMockEndpoint(mockSolarOutput, mockLifetimeOutput, mockPanelId, solarOutputProvider!!.apiKey)

        activityRule.launchActivity(Intent())

        /**
         * Ok, now to actually do some testing!
         */

        // We know the panelId our mock hardware beacon is emitting, and we know the
        // output that will be returned from our mocked RESTful endpoint.
        assertLoadingSolarOutputViews(mockSolarOutput, mockLifetimeOutput, mockPanelId)
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
        `when`(panelProvider.scanForNearbyPanel()).thenReturn(Maybe.create { subscriber ->
            val panelInfo = Panel(desiredPanelDesc, desiredPanelId)

            subscriber.onSuccess(panelInfo)
        })
    }

    private fun assertFindingPanelViews(expectedPanelId: String) {
        onView(withText("Click to find nearby solar panel.")).check(matches(isCompletelyDisplayed()))

        // NJD TODO - first one fails.. don't know why
        onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

        onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

        // No need to wait for real hardware to scan for panel.. because our test thread is
        // blocked on app's background thread

        onView(withText("Click to load solar output ...")).check(matches(isDisplayed())).check(isAbove(withText("solar panel ($expectedPanelId)")))
    }

    private fun assertLoadingSolarOutputViews(expectedSolarOutput: Double?, expectedLifetimeOutput: Double?, expectedPanelId: String) {

        onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

        // No need to wait for real hardware to scan for panel.. because our test thread is
        // blocked on app's background thread

        onView(withId(R.id.loadFAB)).check(matches(isDisplayed())).perform(click())

        // No need to wait for real network call to get solar output.. because our test thread is
        // blocked on app's background thread

        val expectedSolarOutputStringMatcher = if (expectedSolarOutput == null) endsWith("wattHours.") else `is`("current: $expectedSolarOutput watts, lifetime: $expectedLifetimeOutput wattHours.")

        onView(withText(expectedSolarOutputStringMatcher))
                .check(matches(isDisplayed()))
                .check(isAbove(withText("solar panel ($expectedPanelId)")))
    }
}
