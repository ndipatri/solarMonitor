package com.ndipatri.solarmonitor

import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import androidx.test.InstrumentationRegistry.getInstrumentation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import android.view.View
import com.ndipatri.solarmonitor.activities.MainActivity
import com.ndipatri.solarmonitor.activities.MainActivityViewModel
import com.ndipatri.solarmonitor.container.MainActivityViewModelFactory
import com.ndipatri.solarmonitor.container.UITestObjectGraph
import com.ndipatri.solarmonitor.container.modules.MockMainActivityViewModelFactory
import com.ndipatri.solarmonitor.utils.Matchers.isBitmapTheSame
import com.ndipatri.solarmonitor.utils.AACUsesIdlingResourceRule
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import javax.inject.Inject

class MainActivityUnitTest {

    @Rule
    @JvmField
    var activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java, true, false)

    @Rule
    @JvmField
    val executorRule = AACUsesIdlingResourceRule()

    @Inject
    lateinit var viewModelFactory: MainActivityViewModelFactory
    lateinit var mockViewModel: MainActivityViewModel

    lateinit var solarMonitorApp: SolarMonitorApp

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Here Espresso lets us access target application
        solarMonitorApp = getInstrumentation().targetContext.applicationContext as SolarMonitorApp
        solarMonitorApp.shouldCheckForBluetoothPermissions = false

        // With this 'UI' ObjectGraph, we inject a mock ViewMode, keeping everything else intact
        // so we can test using a real MainActivity class.
        val uiTestObjectGraph = UITestObjectGraph.Initializer.init(solarMonitorApp)
        solarMonitorApp.objectGraph = uiTestObjectGraph

        // Here we give this test class access to the same ObjectGraph.. so we can configure
        // or mock ViewModel
        uiTestObjectGraph.inject(this)

        mockViewModel = (viewModelFactory as MockMainActivityViewModelFactory).mockMainActivityViewModel
        mockViewModel.userState = MutableLiveData<MainActivityViewModel.USER_STATE>().also { it.postValue(MainActivityViewModel.USER_STATE.IDLE) }

        mockViewModel.userMessage = MutableLiveData()
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
            mockViewModel.userState.setValue(MainActivityViewModel.USER_STATE.SCANNING)
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


    @Test
    @Throws(Exception::class)
    fun userMessage() {

        activityRule.launchActivity(Intent())

        mockViewModel.userMessage.postValue("hi this is a message")

        onView(withText("hi this is a message")).inRoot(withDecorView(not<View>(`is`<View>(activityRule.activity.getWindow().getDecorView())))).check(matches(isDisplayed()))
    }
}
