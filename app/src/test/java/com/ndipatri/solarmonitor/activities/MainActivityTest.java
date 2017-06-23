package com.ndipatri.solarmonitor.activities;


import android.view.View;
import android.widget.TextView;

import com.ndipatri.solarmonitor.BuildConfig;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.services.BluetoothService;
import com.ndipatri.solarmonitor.services.SolarOutputService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import java.util.concurrent.TimeUnit;

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
    private BluetoothService mockBluetoothService;
    private SolarOutputService mockSolarOutputService;

    @Before
    public void setUp() {

        // This calls MainActivity constructor (where default ObjectGraph is injected)
        // here we could pass in a canned 'Intent' to start this activity
        controller = Robolectric.buildActivity(MainActivity.class);
        activity = controller.get();

        // Now we mock out collaborators
        mockBluetoothService = mock(BluetoothService.class);
        activity.setBluetoothService(mockBluetoothService);

        mockSolarOutputService = mock(SolarOutputService.class);
        activity.setSolarOutputService(mockSolarOutputService);
    }

    @Test
    public void testInitializedProperly() {
        // Creates, starts, resumes activity ...
        controller.setup();

        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.refreshProgressBar).getVisibility());
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.detailTextView).getVisibility());
        assertEquals(View.INVISIBLE,
                activity.findViewById(R.id.solarUpdateFAB).getVisibility());

        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.mainTextView).getVisibility());
        assertEquals(View.VISIBLE,
                activity.findViewById(R.id.beaconScanFAB).getVisibility());

        assertEquals(activity.getText(R.string.click_to_find_nearby_solar_panels),
                     ((TextView)activity.findViewById(R.id.mainTextView)).getText());
    }

    @Test
    public void testClickOnBeaconScanFAB_waitingForScan() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.
        when(mockBluetoothService.searchForNearbyPanels()).thenReturn(Single.just("12345").delay(1000, TimeUnit.MILLISECONDS));

        // Creates, starts, resumes activity ...
        controller.setup();

        activity.findViewById(R.id.beaconScanFAB).performClick();

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

        assertEquals(activity.getText(R.string.finding_nearby_solar_panels),
                ((TextView)activity.findViewById(R.id.mainTextView)).getText());
    }

    @Test
    public void testClickOnBeaconScanFAB_scanFinished() {
        // We need to configure our mocks for this test BEFORE we onCreate(), onStart(),
        // and onResume() our activity.

        TestScheduler testScheduler = new TestScheduler();

        // The 'computation' scheduler is used by the RxJava 'timeout' operator .. it's on
        // this thread that it wakes up and sees if the Observable has emitted.
        RxJavaPlugins.reset();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);

        when(mockBluetoothService.searchForNearbyPanels()).thenReturn(Single.just("12345").delay(10000, TimeUnit.MILLISECONDS));

        // Creates, starts, resumes activity ...
        controller.setup();

        activity.findViewById(R.id.beaconScanFAB).performClick();

        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS);

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

        assertEquals(activity.getText(R.string.click_to_load_solar_output),
                ((TextView)activity.findViewById(R.id.mainTextView)).getText());
        assertEquals("solar customer (12345)",
                ((TextView)activity.findViewById(R.id.detailTextView)).getText());

    }
}

