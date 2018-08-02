package com.ndipatri.solarmonitor.activities


import android.view.View
import android.widget.TextView
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import junit.framework.Assert.assertEquals
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
class MainActivityViewModelTest {

//    private lateinit var controller: ActivityController<MainActivity>
//    private lateinit var activity: MainActivity
//    private lateinit var mockPanelProvider: PanelProvider
//    private lateinit var mockSolarOutputProvider: SolarOutputProvider
//
//    @Before
//    fun setUp() {
//
//        // This calls MainActivity constructor (where default ObjectGraph is injected)
//        // here we could pass in a canned 'Intent' to start this activity
//        controller = Robolectric.buildActivity(MainActivity::class.java)
//        activity = controller.get()
//
//        // Now we mock out collaborators
//        activity.panelProvider = mock<PanelProvider>(PanelProvider::class.java)
//        mockPanelProvider = activity.panelProvider
//
//        activity.solarOutputProvider = mock<SolarOutputProvider>(SolarOutputProvider::class.java)
//        mockSolarOutputProvider = activity.solarOutputProvider
//    }
//
//    @Test
//    fun testInitializedProperly() {
//        // Creates, starts, resumes activity ...
//        controller.setup()
//
//        assertEquals(View.GONE, activity.refreshProgressBar.visibility)
//        assertEquals(View.INVISIBLE, activity.detailTextView.visibility)
//
//        // by default, we don't know of any panels, so we don't expose
//        // loadFAB
//        assertEquals(View.INVISIBLE, activity.loadFAB.visibility)
//
//        assertEquals(View.VISIBLE, activity.mainTextView.visibility)
//        assertEquals(View.VISIBLE, activity.scanFAB.visibility)
//
//        assertEquals(activity.getText(R.string.click_to_find_nearby_solar_panel),
//                    ((activity.mainTextView) as TextView).text)
//    }
//
//    @Test
//    fun testClickOnBeaconScanFAB_waitingForScanResults() {
//        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
//        // and onResume() our activity.
//
//        // Because our scan response is delayed, we will be waiting for results...
//        `when`(mockPanelProvider
//                .scanForNearbyPanel())
//                    .thenReturn(Maybe.just(Panel("Nicks Solar Panels", "12345"))
//                        .delay(1000, TimeUnit.MILLISECONDS))
//
//        // Creates, starts, resumes activity ...
//        controller.setup()
//
//        activity.scanFAB.performClick()
//
//        // Because we've configured our scan response to be delayed above, we will be waiting for results...
//
//        assertEquals(View.INVISIBLE, activity.detailTextView.visibility)
//        assertEquals(View.INVISIBLE, activity.loadFAB.visibility)
//
//        assertEquals(View.VISIBLE, activity.refreshProgressBar.visibility)
//        assertEquals(View.VISIBLE, activity.mainTextView.visibility)
//        assertEquals(View.INVISIBLE, activity.scanFAB.visibility)
//
//        assertEquals(activity.getText(R.string.finding_nearby_solar_panel),
//                    ((activity.mainTextView) as TextView).text)
//    }
//
//    @Test
//    fun testClickOnBeaconScanFAB_scanResults() {
//        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
//        // and onResume() our activity.
//
//        // The 'computation' scheduler is used by the above RxJava 'delay' operator
//        val testScheduler = TestScheduler()
//        RxJavaPlugins.reset()
//        RxJavaPlugins.setComputationSchedulerHandler { scheduler -> testScheduler }
//        RxJavaPlugins.setIoSchedulerHandler { scheduler -> testScheduler }
//
//        `when`(mockPanelProvider
//            .scanForNearbyPanel())
//                .thenReturn(Maybe.just(Panel("Nicks Solar Panels", "123456"))
//                    .delay(1, TimeUnit.SECONDS))
//
//        // Creates, starts, resumes activity ...
//        controller.setup()
//
//        activity.scanFAB.performClick()
//
//        // Because scan response is delayed above, we're in a 'finding' state for a bit ...
//        assertEquals(activity.getText(R.string.finding_nearby_solar_panel),
//                     ((activity.mainTextView) as TextView).text)
//
//        // Advance the 'computation' scheduler so the above delayed nearby panel even will finally
//        // be emitted.
//        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
//
//        // Confirm that bluetooth scan is finished...
//        assertEquals(View.GONE, activity.refreshProgressBar.visibility)
//
//        assertEquals(View.VISIBLE, activity.mainTextView.visibility)
//        assertEquals(View.VISIBLE, activity.detailTextView.visibility)
//        assertEquals(View.VISIBLE, activity.scanFAB.visibility)
//        assertEquals(View.VISIBLE, activity.loadFAB.visibility)
//
//        assertEquals("solar panel (123456)", ((activity.detailTextView) as TextView).text)
//        assertEquals(activity.getText(R.string.click_to_load_solar_output),
//                    ((activity.mainTextView) as TextView).text)
//    }
//
//    @Test
//    fun testClickOnSolarUpdateFAB_waitingForUpdate() {
//        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
//        // and onResume() our activity.
//
//        SolarMonitorApp.instance.solarCustomerId.set("54321")
//        `when`(mockSolarOutputProvider
//            .getSolarOutput("54321"))
//                .thenReturn(Single.just(PowerOutput(123.0, 456.0))
//                    .delay(1, TimeUnit.SECONDS))
//
//        // Creates, starts, resumes activity ...
//        controller.setup()
//
//        assertEquals(activity.getText(R.string.click_to_load_solar_output),
//                    ((activity.mainTextView) as TextView).text)
//
//        // because we haven't advanced 'computational' scheduler, the above 'getSolarOutputInWatts()' will be pending
//        activity.loadFAB.performClick()
//
//        assertEquals(View.VISIBLE, activity.refreshProgressBar.visibility)
//        assertEquals(View.VISIBLE, activity.mainTextView.visibility)
//        assertEquals(View.INVISIBLE, activity.scanFAB.visibility)
//
//        assertEquals("solar panel (54321)", ((activity.detailTextView) as TextView).text)
//        assertEquals(activity.getText(R.string.loading_solar_output),
//                    ((activity.mainTextView) as TextView).text)
//    }
//
//    @Test
//    fun testClickOnSolarUpdateFAB_updateResults() {
//        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
//        // and onResume() our activity.
//
//        SolarMonitorApp.instance.solarCustomerId.set("54321")
//        `when`(mockSolarOutputProvider
//            .getSolarOutput("54321"))
//                .thenReturn(Single.just(PowerOutput(123.0, 456.0)))
//
//        // Creates, starts, resumes activity ...
//        controller.setup()
//
//        // because we haven't delayed our output results above, they will return immediately, and
//        // on main thread.
//        activity.loadFAB.performClick()
//
//        assertEquals(View.GONE, activity.refreshProgressBar.visibility)
//
//        assertEquals(View.VISIBLE, activity.mainTextView.visibility)
//        assertEquals(View.VISIBLE, activity.scanFAB.visibility)
//
//        assertEquals("solar panel (54321)", ((activity.detailTextView) as TextView).text)
//        assertEquals("current: 123.0 watts, lifetime: 456.0 wattHours.",
//                     ((activity.mainTextView) as TextView).text)
//    }
//
//    @Test
//    fun testClickOnBeaconScanFAB_waitingForScanResults_timeout() {
//        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
//        // and onResume() our activity.
//
//        `when`(mockPanelProvider
//            .scanForNearbyPanel())
//                .thenReturn(Maybe.create { subscriber -> subscriber.onError(TimeoutException()) })
//
//        // Creates, starts, resumes activity ...
//        controller.setup()
//
//        activity.scanFAB.performClick()
//
//        assertEquals(activity.getText(R.string.error_please_try_again), ShadowToast.getTextOfLatestToast())
//    }
//
//    @Test
//    fun testClickOnSolarUpdateFAB_waitingForUpdate_timeout() {
//        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
//        // and onResume() our activity.
//
//        SolarMonitorApp.instance.solarCustomerId.set("54321")
//
//        `when`(mockSolarOutputProvider
//            .getSolarOutput("54321"))
//                .thenReturn(Single.create { subscriber -> subscriber.onError(TimeoutException()) })
//
//        // Creates, starts, resumes activity ...
//        controller.setup()
//
//        activity.loadFAB.performClick()
//
//        assertEquals(activity.getText(R.string.error_please_try_again), ShadowToast.getTextOfLatestToast())
//    }
}

