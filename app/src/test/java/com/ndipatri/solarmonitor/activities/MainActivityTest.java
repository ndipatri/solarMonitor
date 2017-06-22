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

import io.reactivex.Single;

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
        activity.bluetoothService = mockBluetoothService;

        mockSolarOutputService = mock(SolarOutputService.class);
        activity.solarOutputService = mockSolarOutputService;
    }

    @Test
    public void testInitializedProperly() {
        // We need to configure our mocks for this test BEFORE we create, start, resume
        // our activity.
        when(mockBluetoothService.getNearbySolarCustomerId()).thenReturn(Single.just("12345"));
        when(mockSolarOutputService.getSolarOutputInWatts("12345")).thenReturn(Single.just(123D));

        // Creates, starts, resumes activity ...
        controller.setup();

        assertEquals(View.INVISIBLE,
                     activity.findViewById(R.id.refreshProgressBar).getVisibility());

        assertEquals(activity.getText(R.string.click_to_load_solar_output),
                     ((TextView)activity.findViewById(R.id.dialogTextView)).getText());
    }
}
