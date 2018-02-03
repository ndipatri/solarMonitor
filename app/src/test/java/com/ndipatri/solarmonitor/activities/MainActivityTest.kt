package com.ndipatri.solarmonitor.activities


import android.view.View
import android.widget.TextView

import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowToast

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler

import junit.framework.Assert.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private var controller: ActivityController<MainActivity>? = null
    private var activity: MainActivity? = null
    private var mockPanelScanProvider: PanelScanProvider? = null
    private var mockSolarOutputProvider: SolarOutputProvider? = null

    @Before
    fun setUp() {

        // This calls MainActivity constructor (where default ObjectGraph is injected)
        // here we could pass in a canned 'Intent' to start this activity
        controller = Robolectric.buildActivity(MainActivity::class.java!!)
        activity = controller!!.get()

        // Now we mock out collaborators
        mockPanelScanProvider = mock<PanelScanProvider>(PanelScanProvider::class.java)
        activity!!.setPanelScanProvider(mockPanelScanProvider)

        mockSolarOutputProvider = mock<SolarOutputProvider>(SolarOutputProvider::class.java)
        activity!!.setSolarOutputProvider(mockSolarOutputProvider)
    }

    @Test
    fun testInitializedProperly() {
        // Creates, starts, resumes activity ...
        controller!!.setup()

        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.refreshProgressBar).visibility)
        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.detailTextView).visibility)

        // by default, we don't know of any panels, so we don't expose
        // loadFAB
        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.loadFAB).visibility)

        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.mainTextView).visibility)
        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.scanFAB).visibility)

        assertEquals(activity!!.getText(R.string.click_to_find_nearby_solar_panel),
                (activity!!.findViewById(R.id.mainTextView) as TextView).text)
    }

    @Test
    fun testClickOnBeaconScanFAB_waitingForScanResults() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        // Because our scan response is delayed, we will be waiting for results...
        `when`(mockPanelScanProvider!!.scanForNearbyPanel()).thenReturn(Maybe.just(PanelInfo("Nicks Solar Panels", "12345")).delay(1000, TimeUnit.MILLISECONDS))

        // Creates, starts, resumes activity ...
        controller!!.setup()

        activity!!.findViewById(R.id.scanFAB).performClick()

        // Because we've configured our scan response to be delayed above, we will be waiting for results...

        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.detailTextView).visibility)
        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.loadFAB).visibility)

        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.refreshProgressBar).visibility)
        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.mainTextView).visibility)
        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.scanFAB).visibility)

        assertEquals(activity!!.getText(R.string.finding_nearby_solar_panel),
                (activity!!.findViewById(R.id.mainTextView) as TextView).text)
    }

    @Test
    fun testClickOnBeaconScanFAB_scanResults() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        // The 'computation' scheduler is used by the above RxJava 'delay' operator
        val testScheduler = TestScheduler()
        RxJavaPlugins.reset()
        RxJavaPlugins.setComputationSchedulerHandler { scheduler -> testScheduler }
        RxJavaPlugins.setIoSchedulerHandler { scheduler -> testScheduler }

        `when`(mockPanelScanProvider!!.scanForNearbyPanel()).thenReturn(Maybe.just(PanelInfo("Nicks Solar Panels", "123456")).delay(1, TimeUnit.SECONDS))

        // Creates, starts, resumes activity ...
        controller!!.setup()

        activity!!.findViewById(R.id.scanFAB).performClick()

        // Because scan response is delayed above, we're in a 'finding' state for a bit ...
        assertEquals(activity!!.getText(R.string.finding_nearby_solar_panel),
                (activity!!.findViewById(R.id.mainTextView) as TextView).text)

        // Advance the 'computation' scheduler so the above delayed nearby panel even will finally
        // be emitted.
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        // Confirm that bluetooth scan is finished...
        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.refreshProgressBar).visibility)

        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.mainTextView).visibility)
        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.detailTextView).visibility)
        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.scanFAB).visibility)
        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.loadFAB).visibility)

        assertEquals("solar panel (123456)",
                (activity!!.findViewById(R.id.detailTextView) as TextView).text)
        assertEquals(activity!!.getText(R.string.click_to_load_solar_output),
                (activity!!.findViewById(R.id.mainTextView) as TextView).text)

    }

    @Test
    fun testClickOnSolarUpdateFAB_waitingForUpdate() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        SolarMonitorApp.instance!!.solarCustomerId.set("54321")
        `when`(mockSolarOutputProvider!!.getSolarOutput("54321")).thenReturn(Single.just(PowerOutput(123.0, 456.0)).delay(1, TimeUnit.SECONDS))

        // Creates, starts, resumes activity ...
        controller!!.setup()

        assertEquals(activity!!.getText(R.string.click_to_load_solar_output),
                (activity!!.findViewById(R.id.mainTextView) as TextView).text)

        // because we haven't advanced 'computational' scheduler, the above 'getSolarOutputInWatts()' will be pending
        activity!!.findViewById(R.id.loadFAB).performClick()

        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.refreshProgressBar).visibility)
        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.mainTextView).visibility)
        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.scanFAB).visibility)

        assertEquals("solar panel (54321)",
                (activity!!.findViewById(R.id.detailTextView) as TextView).text)
        assertEquals(activity!!.getText(R.string.loading_solar_output),
                (activity!!.findViewById(R.id.mainTextView) as TextView).text)
    }

    @Test
    fun testClickOnSolarUpdateFAB_updateResults() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        SolarMonitorApp.instance!!.solarCustomerId.set("54321")
        `when`(mockSolarOutputProvider!!.getSolarOutput("54321")).thenReturn(Single.just(PowerOutput(123.0, 456.0)))

        // Creates, starts, resumes activity ...
        controller!!.setup()

        // because we haven't delayed our output results above, they will return immediately, and
        // on main thread.
        activity!!.findViewById(R.id.loadFAB).performClick()

        assertEquals(View.INVISIBLE,
                activity!!.findViewById(R.id.refreshProgressBar).visibility)

        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.mainTextView).visibility)
        assertEquals(View.VISIBLE,
                activity!!.findViewById(R.id.scanFAB).visibility)

        assertEquals("solar panel (54321)",
                (activity!!.findViewById(R.id.detailTextView) as TextView).text)
        assertEquals("current: 123.0 watts, lifetime: 456.0 wattHours.",
                (activity!!.findViewById(R.id.mainTextView) as TextView).text)
    }

    @Test
    fun testClickOnBeaconScanFAB_waitingForScanResults_timeout() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        `when`(mockPanelScanProvider!!.scanForNearbyPanel())
                .thenReturn(Maybe.create { subscriber -> subscriber.onError(TimeoutException()) })

        // Creates, starts, resumes activity ...
        controller!!.setup()

        activity!!.findViewById(R.id.scanFAB).performClick()

        assertEquals(activity!!.getText(R.string.error_please_try_again), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun testClickOnSolarUpdateFAB_waitingForUpdate_timeout() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        SolarMonitorApp.instance!!.solarCustomerId.set("54321")
        `when`(mockSolarOutputProvider!!.getSolarOutput("54321"))
                .thenReturn(Single.create { subscriber -> subscriber.onError(TimeoutException()) })

        // Creates, starts, resumes activity ...
        controller!!.setup()

        activity!!.findViewById(R.id.loadFAB).performClick()

        assertEquals(activity!!.getText(R.string.error_please_try_again), ShadowToast.getTextOfLatestToast())
    }
}

