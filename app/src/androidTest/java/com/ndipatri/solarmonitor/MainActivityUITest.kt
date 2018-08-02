package com.ndipatri.solarmonitor

import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.PositionAssertions.isAbove
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.view.View
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.activities.MainActivityViewModel
import com.ndipatri.solarmonitor.container.MainActivityViewModelFactory
import com.ndipatri.solarmonitor.container.modules.MockMainActivityViewModelFactory
import com.ndipatri.solarmonitor.container.UITestObjectGraph
import com.ndipatri.solarmonitor.utils.MockSolarOutputServer
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.CurrentPower
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.GetOverviewResponse
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.LifeTimeData
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.Overview
import com.ndipatri.solarmonitor.utils.AsyncTaskSchedulerRule
import com.ndipatri.solarmonitor.utils.Matchers.isBitmapTheSame
import io.reactivex.Maybe
import org.hamcrest.CoreMatchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import java.net.MalformedURLException
import javax.inject.Inject

class MainActivityUITest {

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
    lateinit var viewModelFactory: MainActivityViewModelFactory
    lateinit var mockViewModel: MainActivityViewModel

    lateinit var solarMonitorApp: SolarMonitorApp

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Context of the app under test.
        solarMonitorApp = getInstrumentation().targetContext.applicationContext as SolarMonitorApp
        solarMonitorApp.shouldCheckForHardwarePermissions = false

        // We can bootstrap the target application with a UITestObjectGraph(Dagger) which will
        // inject mock ViewModels so the only real code is the UI component under test
        val uiTestObjectGraph = UITestObjectGraph.Initializer.init(solarMonitorApp)
        solarMonitorApp.objectGraph = uiTestObjectGraph
        uiTestObjectGraph.inject(this)

        mockViewModel = (viewModelFactory as MockMainActivityViewModelFactory).mockMainActivityViewModel
        mockViewModel.userState = MutableLiveData<MainActivityViewModel.USER_STATE>().also { it.postValue(MainActivityViewModel.USER_STATE.IDLE) }
    }

    @Test
    @Throws(Exception::class)
    fun idleState() {

        activityRule.launchActivity(Intent())

        onView(withText("Click to find nearby solar panel.")).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.detailTextView)).check(matches(not<View>(isDisplayed())))
        onView(withId(R.id.refreshProgressBar)).check(matches(not<View>(isDisplayed())))
        onView(withId(R.id.scanFAB)).check(matches(isCompletelyDisplayed())).check(matches(isBitmapTheSame(android.R.drawable.ic_menu_compass)))
        onView(withId(R.id.loadFAB)).check(matches(not<View>(isDisplayed())))
        onView(withId(R.id.configureFAB)).check(matches(not<View>(isDisplayed())))
    }

    @Test
    @Throws(Exception::class)
    fun scanningState() {

        `when`(mockViewModel.scanForNearbyPanel()).then {
            mockViewModel.userState.postValue(MainActivityViewModel.USER_STATE.SCANNING)
        }

        activityRule.launchActivity(Intent())

        onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

        onView(withText("Finding nearby solar panel ...")).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.detailTextView)).check(matches(not<View>(isDisplayed())))
        onView(withId(R.id.refreshProgressBar)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.scanFAB)).check(matches(not<View>(isDisplayed())))
        onView(withId(R.id.loadFAB)).check(matches(not<View>(isDisplayed())))
        onView(withId(R.id.configureFAB)).check(matches(not<View>(isDisplayed())))
    }

    // NJD TODO - need to cover remaining states...
}
