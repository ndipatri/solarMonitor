package com.ndipatri.solarmonitor;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.ndipatri.solarmonitor.activities.MainActivity;
import com.ndipatri.solarmonitor.container.MockObjectGraph;
import com.ndipatri.solarmonitor.container.TestObjectGraph;
import com.ndipatri.solarmonitor.dto.CurrentPower;
import com.ndipatri.solarmonitor.dto.GetOverviewResponse;
import com.ndipatri.solarmonitor.dto.Overview;
import com.ndipatri.solarmonitor.services.BluetoothService;
import com.ndipatri.solarmonitor.services.SolarOutputService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.PositionAssertions.isAbove;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentationTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Rule
    public final AsyncTaskSchedulerRule asyncTaskSchedulerRule = new AsyncTaskSchedulerRule();

    @Inject SolarOutputService solarOutputService;
    @Inject BluetoothService bluetoothService;

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
    public void retrieveSolarOutput_realService_realHardware_realEndpoint() throws Exception {

        activityRule.launchActivity(new Intent());

        onView(withText("Click to load Solar Output ...")).check(matches(isDisplayed())).check(isAbove(withText("real bluetooth found!")));

        onView(withId(R.id.solarUpdateFAB)).check(matches(isDisplayed())).perform(click());

        onView(withText("real bluetooth found!")).check(matches(not(isDisplayed())));
    }

    // Mock Endpoint
    //
    // Here we use our production ObjectGraph with our real service and hardware layer.  This service
    // layer requires a remote RESTful endpoint. We use MockWebServer to create a
    // real endpoint.
    //
    // Pros - Allows to automated 'integration testing' with a deterministic, configurable endpoint.
    //        It's possible for the real endpoint to not yet exist.  If a test fails, you know the
    //        code is to blame and not the endpoint (reduced test scope)
    //
    // Cons - MockWebServer requires configuration, mock endpoint might diverge from actual
    //        endpoint design or implementation.
    //
    @Test
    public void retrieveSolarOutput_realService_realHardware_mockEndpoint() throws Exception {

        // Context of the app under test.
        SolarMonitorApp solarMonitorApp = (SolarMonitorApp) getInstrumentation().getTargetContext().getApplicationContext();

        // We need access to the target application's production ObjectGraph so we can instrument our
        // MockWebServer
        TestObjectGraph testObjectGraph = TestObjectGraph.Initializer.init(solarMonitorApp);
        solarMonitorApp.setObjectGraph(testObjectGraph);
        testObjectGraph.inject(this);

        configureMockEndpoint(solarMonitorApp.getSolarCustomerId(), solarOutputService.getApiKey());

        activityRule.launchActivity(new Intent());

        /**
         * Ok, now to actually do some testing!
         */
        onView(withText("Click to load Solar Output ...")).check(matches(isDisplayed())).check(isAbove(withText("real bluetooth found!")));

        onView(withId(R.id.solarUpdateFAB)).check(matches(isDisplayed())).perform(click());

        onView(withText("real bluetooth found!")).check(matches(not(isDisplayed())));

        onView(withText("123.0 watts")).check(matches(isDisplayed()));
    }

    // Here we are injecting a 'mock' ObjectGraph which gives us the chance to mock out
    // some hardware components that our app depends upon.  The service layer in this
    // mock ObjectGraph is still the real implementation and therefore needs the MockWebServer.
    //
    // Pros - Allows us to run repeatable tests on a device that might not have all required production
    // components (e.g. emulators do not have Bluetooth).  This is useful for IOT testing.
    //
    // Cons - Can be incorrectly used to replace proper unit testing.  Unit tests are a
    // much faster way to test front-end components.  So we still use the real service
    // layer here!
    @Test
    public void retrieveSolarOutput_realService_mockHardware_mockEndpoint() throws Exception {

        // Context of the app under test.
        SolarMonitorApp solarMonitorApp = (SolarMonitorApp) getInstrumentation().getTargetContext().getApplicationContext();

        // We can load the target application with a MockObjectGraph which will use real service
        // layer but a mock hardware layer.
        MockObjectGraph mockObjectGraph = MockObjectGraph.Initializer.init(solarMonitorApp);
        solarMonitorApp.setObjectGraph(mockObjectGraph);
        mockObjectGraph.inject(this);

        configureMockEndpoint(solarMonitorApp.getSolarCustomerId(), solarOutputService.getApiKey());
        configureMockHardware();

        activityRule.launchActivity(new Intent());

        /**
         * Ok, now to actually do some testing!
         */
        onView(withText("Click to load Solar Output ...")).check(matches(isDisplayed())).check(isAbove(withText("mock bluetooth found!")));

        onView(withId(R.id.solarUpdateFAB)).check(matches(isDisplayed())).perform(click());

        onView(withText("mock bluetooth found!")).check(matches(not(isDisplayed())));

        onView(withText("123.0 watts")).check(matches(isDisplayed()));
    }

    private void configureMockEndpoint(String solarCustomerId, String solarApiKey) throws MalformedURLException {
        // We deploy a MockWebServer to the same virtual machine as our
        // target APK
        MockSolarOutputServer mockSolarOutputServer = new MockSolarOutputServer();

        GetOverviewResponse getOverviewResponse = new GetOverviewResponse();

        CurrentPower currentPower = new CurrentPower();
        currentPower.setPower(123D);

        Overview overview = new Overview();
        overview.setCurrentPower(currentPower);

        getOverviewResponse.setOverview(overview);

        mockSolarOutputServer.
                enqueueDesiredSolarOutputResponse(getOverviewResponse,
                        solarCustomerId,
                        solarApiKey);

        mockSolarOutputServer.beginUsingMockServer();

        // This is the only way in which our Test APK deviates from production.  We need to
        // point our service to the mock endpoint (mockWebServer)
        SolarOutputService.API_ENDPOINT_BASE_URL = mockSolarOutputServer.getMockSolarOutputServerURL();
    }

    private void configureMockHardware() {
        when(bluetoothService.getNearbySolarCustomerId()).thenReturn(Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> subscriber) throws Exception {
                subscriber.onSuccess("mock bluetooth found!");
            }
        }));
    }
}
