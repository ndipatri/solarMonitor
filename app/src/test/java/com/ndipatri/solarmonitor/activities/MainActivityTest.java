package com.ndipatri.solarmonitor.activities;


import android.view.View;
import android.widget.TextView;

import com.ndipatri.solarmonitor.BuildConfig;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.dto.PowerOutput;
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo;
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class MainActivityTest {

    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private PanelScanProvider mockPanelScanProvider;
    private SolarOutputProvider mockSolarOutputProvider;

    @Before
    public void setUp() {

        // This calls MainActivity constructor (where default ObjectGraph is injected)
        // here we could pass in a canned 'Intent' to start this activity
        controller = Robolectric.buildActivity(MainActivity.class);
        activity = controller.get();

        // Now we mock out collaborators
        mockPanelScanProvider = mock(PanelScanProvider.class);
        activity.setPanelScanProvider(mockPanelScanProvider);

        mockSolarOutputProvider = mock(SolarOutputProvider.class);
        activity.setSolarOutputProvider(mockSolarOutputProvider);
    }

    @Test
    public void testInitializedProperly() {
        // Creates, starts, resumes activity ...
        controller.setup();

        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.refreshProgressBar).getVisibility());
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.detailTextView).getVisibility());

        // by default, we don't know of any panels, so we don't expose
        // solarUpdateFAB
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.solarUpdateFAB).getVisibility());

        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.mainTextView).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.beaconScanFAB).getVisibility());

        assertEquals(activity.getText(R.string.click_to_find_nearby_solar_panel),
                ((TextView) activity.findViewById(R.id.mainTextView)).getText());
    }

    @Test
    public void testClickOnBeaconScanFAB_waitingForScanResults() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        // Because our scan response is delayed, we will be waiting for results...
        when(mockPanelScanProvider.scanForNearbyPanel()).thenReturn(Observable.just(new PanelInfo("Nicks Solar Panels", "12345")).delay(1000, TimeUnit.MILLISECONDS));

        // Creates, starts, resumes activity ...
        controller.setup();

        activity.findViewById(R.id.beaconScanFAB).performClick();

        // Because we've configured our scan response to be delayed above, we will be waiting for results...

        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.detailTextView).getVisibility());
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.solarUpdateFAB).getVisibility());

        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.refreshProgressBar).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.mainTextView).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.beaconScanFAB).getVisibility());

        assertEquals(activity.getText(R.string.finding_nearby_solar_panel),
                ((TextView) activity.findViewById(R.id.mainTextView)).getText());
    }

    @Test
    public void testClickOnBeaconScanFAB_scanResults() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        // The 'computation' scheduler is used by the above RxJava 'delay' operator
        TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.reset();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);

        when(mockPanelScanProvider.scanForNearbyPanel()).thenReturn(Observable.just(new PanelInfo("Nicks Solar Panels", "12345")).delay(1, TimeUnit.SECONDS));

        // Creates, starts, resumes activity ...
        controller.setup();

        activity.findViewById(R.id.beaconScanFAB).performClick();

        // Because scan response is delayed above, we're in a 'finding' state for a bit ...
        assertEquals(activity.getText(R.string.finding_nearby_solar_panel),
                ((TextView) activity.findViewById(R.id.mainTextView)).getText());

        // Advance the 'computation' scheduler so the above delayed nearby panel even will finally
        // be emitted.
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        // Confirm that bluetooth scan is finished...
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.refreshProgressBar).getVisibility());

        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.mainTextView).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.detailTextView).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.beaconScanFAB).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.solarUpdateFAB).getVisibility());

        assertEquals("solar panel (12345)",
                ((TextView) activity.findViewById(R.id.detailTextView)).getText());
        assertEquals(activity.getText(R.string.click_to_load_solar_output),
                ((TextView) activity.findViewById(R.id.mainTextView)).getText());

    }

     @Test
     public void testClickOnSolarUpdateFAB_waitingForUpdate() {
         // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
         // and onResume() our activity.

         SolarMonitorApp.getInstance().getSolarCustomerId().set("54321");
         when(mockSolarOutputProvider.getSolarOutput("54321")).thenReturn(Single.just(new PowerOutput(123D, 456D)).delay(1, TimeUnit.SECONDS));

         // Creates, starts, resumes activity ...
         controller.setup();

         assertEquals(activity.getText(R.string.click_to_load_solar_output),
                 ((TextView) activity.findViewById(R.id.mainTextView)).getText());

         // because we haven't advanced 'computational' scheduler, the above 'getSolarOutputInWatts()' will be pending
         activity.findViewById(R.id.solarUpdateFAB).performClick();

         assertEquals(View.VISIBLE,
                 activity.findViewById(R.id.refreshProgressBar).getVisibility());
         assertEquals(View.VISIBLE,
                 activity.findViewById(R.id.mainTextView).getVisibility());
         assertEquals(View.VISIBLE,
                 activity.findViewById(R.id.beaconScanFAB).getVisibility());

         assertEquals("solar panel (54321)",
                 ((TextView) activity.findViewById(R.id.detailTextView)).getText());
         assertEquals(activity.getText(R.string.loading_solar_output),
                 ((TextView) activity.findViewById(R.id.mainTextView)).getText());
     }

    @Test
    public void testClickOnSolarUpdateFAB_updateResults() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        SolarMonitorApp.getInstance().getSolarCustomerId().set("54321");
        when(mockSolarOutputProvider.getSolarOutput("54321")).thenReturn(Single.just(new PowerOutput(123D, 456D)));

        // Creates, starts, resumes activity ...
        controller.setup();

        // because we haven't delayed our output results above, they will return immediately, and
        // on main thread.
        activity.findViewById(R.id.solarUpdateFAB).performClick();

        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.refreshProgressBar).getVisibility());

        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.mainTextView).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.beaconScanFAB).getVisibility());

        assertEquals("solar panel (54321)",
                ((TextView) activity.findViewById(R.id.detailTextView)).getText());
        assertEquals("current: 123.0 watts, lifetime: 456.0 wattsHours.",
                ((TextView) activity.findViewById(R.id.mainTextView)).getText());
    }

    @Test
    public void testClickOnBeaconScanFAB_waitingForScanResults_timeout() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        when(mockPanelScanProvider.scanForNearbyPanel())
                .thenReturn(Observable.create(subscriber -> subscriber.onError(new TimeoutException())));

        // Creates, starts, resumes activity ...
        controller.setup();

        activity.findViewById(R.id.beaconScanFAB).performClick();

        assertEquals(activity.getText(R.string.error_please_try_again), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testClickOnSolarUpdateFAB_waitingForUpdate_timeout() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        SolarMonitorApp.getInstance().getSolarCustomerId().set("54321");
        when(mockSolarOutputProvider.getSolarOutput("54321"))
                .thenReturn(Single.create(subscriber -> subscriber.onError(new TimeoutException())));

        // Creates, starts, resumes activity ...
        controller.setup();

        activity.findViewById(R.id.solarUpdateFAB).performClick();

        assertEquals(activity.getText(R.string.error_please_try_again), ShadowToast.getTextOfLatestToast());
    }
}

