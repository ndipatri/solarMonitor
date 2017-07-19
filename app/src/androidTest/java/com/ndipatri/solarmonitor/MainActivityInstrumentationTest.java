package com.ndipatri.solarmonitor;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.ndipatri.solarmonitor.activities.MainActivity;
import com.ndipatri.solarmonitor.container.MockObjectGraph;
import com.ndipatri.solarmonitor.mocks.MockSolarOutputServer;
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo;
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider;
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.CurrentPower;
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.GetOverviewResponse;
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.LifeTimeData;
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.Overview;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Observer;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.PositionAssertions.isAbove;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentationTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    // This is the coolest thing ever.  We are configuring our test thread (this thread) to block
    // while the background thread is running in our target application.
    @Rule
    public final AsyncTaskSchedulerRule asyncTaskSchedulerRule = new AsyncTaskSchedulerRule();

    @Inject SolarOutputProvider solarOutputService;
    @Inject PanelScanProvider bluetoothService;

    @Before
    public void setUp() throws Exception {

        // Context of the app under test.
        SolarMonitorApp solarMonitorApp = (SolarMonitorApp) getInstrumentation().getTargetContext().getApplicationContext();

        // clear out any remaining state.
        solarMonitorApp.getSolarCustomerId().delete();
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
    public void findingPanel_realExternalCollaborators() throws Exception {

        activityRule.launchActivity(new Intent());

        // we know what panelId our real beacon is emitting.
        assertFindingPanelViews("480557");
    }

    @Test
    public void loadingSolarOutput_realExternalCollaborators() throws Exception {

        activityRule.launchActivity(new Intent());

        // we cannot predict the real solar output right now.
        assertLoadingSolarOutputViews(null, null, "480557");
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
    public void loadingSolarOutput_mockExternalCollaborators() throws Exception {

        // Context of the app under test.
        SolarMonitorApp solarMonitorApp = (SolarMonitorApp) getInstrumentation().getTargetContext().getApplicationContext();

        // We can bootstrap the target application with a MockObjectGraph(Dagger) which will use real service
        // layer but a mock hardware layer.
        MockObjectGraph mockObjectGraph = MockObjectGraph.Initializer.init(solarMonitorApp);
        solarMonitorApp.setObjectGraph(mockObjectGraph);
        mockObjectGraph.inject(this);

        // Now that the hardware layer has been mocked, configure it...
        String mockPanelDesc = "nicks panel";
        String mockPanelId = "998877";
        configureMockHardware(mockPanelDesc, mockPanelId);

        // Configure MockWebServer to provide mock RESTful endpoint
        Double mockSolarOutput = 123D;
        Double mockLifetimeOutput = 456D;
        configureMockEndpoint(mockSolarOutput, mockLifetimeOutput, mockPanelId, solarOutputService.getApiKey());

        activityRule.launchActivity(new Intent());

        /**
         * Ok, now to actually do some testing!
         */

        // We know the panelId our mock hardware beacon is emitting, and we know the
        // output that will be returned from our mocked RESTful endpoint.
        assertLoadingSolarOutputViews(mockSolarOutput, mockLifetimeOutput, mockPanelId);
    }

    private void configureMockEndpoint(Double currentPowerValue, Double lifetimeEnergyValue, String solarCustomerId, String solarApiKey) throws MalformedURLException {
        // We deploy a MockWebServer to the same virtual machine as our
        // target APK
        MockSolarOutputServer mockSolarOutputServer = new MockSolarOutputServer();

        GetOverviewResponse getOverviewResponse = new GetOverviewResponse();

        CurrentPower currentPower = new CurrentPower();
        currentPower.setPower(currentPowerValue);

        LifeTimeData lifeTimeData = new LifeTimeData();
        lifeTimeData.setEnergy(lifetimeEnergyValue);

        Overview overview = new Overview();
        overview.setCurrentPower(currentPower);
        overview.setLifeTimeData(lifeTimeData);

        getOverviewResponse.setOverview(overview);

        mockSolarOutputServer.
                enqueueDesiredSolarOutputResponse(getOverviewResponse,
                        solarCustomerId,
                        solarApiKey);

        mockSolarOutputServer.beginUsingMockServer();

        // This is the only way in which our Test APK deviates from production.  We need to
        // point our service to the mock endpoint (mockWebServer)
        SolarOutputProvider.API_ENDPOINT_BASE_URL = mockSolarOutputServer.getMockSolarOutputServerURL();
    }

    private void configureMockHardware(String desiredPanelDesc, String desiredPanelId) {
        when(bluetoothService.scanForNearbyPanel()).thenReturn(new Observable<PanelInfo>() {
            @Override
            protected void subscribeActual(Observer<? super PanelInfo> observer) {
                PanelInfo panelInfo = new PanelInfo(desiredPanelDesc, desiredPanelId);

                observer.onNext(panelInfo);
                observer.onComplete();
            }
        });
    }

    private void assertFindingPanelViews(String expectedPanelId) {
        onView(withText("Click to find nearby Solar panel.")).check(matches(isCompletelyDisplayed()));

        onView(withId(R.id.beaconScanFAB)).check(matches(isDisplayed())).perform(click());

        // No need to wait for real hardware to scan for panel.. because our test thread is
        // blocked on app's background thread

        onView(withText("Click to load Solar Output ...")).check(matches(isDisplayed())).check(isAbove(withText("solar panel (" + expectedPanelId + ")")));
    }

    private void assertLoadingSolarOutputViews(Double expectedSolarOutput, Double expectedLifetimeOutput, String expectedPanelId) {
        onView(withId(R.id.beaconScanFAB)).check(matches(isDisplayed())).perform(click());

        // No need to wait for real hardware to scan for panel.. because our test thread is
        // blocked on app's background thread

        onView(withId(R.id.solarUpdateFAB)).check(matches(isDisplayed())).perform(click());

        // No need to wait for real network call to get solar output.. because our test thread is
        // blocked on app's background thread

        Matcher<String> expectedSolarOutputStringMatcher =
                (expectedSolarOutput == null ? endsWith("wattHours.") : is("current: " + expectedSolarOutput + " watts, lifetime: " + expectedLifetimeOutput + " wattHours."));

        onView(withText(expectedSolarOutputStringMatcher))
                .check(matches(isDisplayed()))
                .check(isAbove(withText("solar panel (" + expectedPanelId + ")")));
    }
}
