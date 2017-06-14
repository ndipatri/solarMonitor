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

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentationTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

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

        Thread.sleep(6000000);
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

        // By creating the graph here BEFORE it's used by the activity, we can inject our mock collaborators.
        TestObjectGraph testObjectGraph = TestObjectGraph.Initializer.init(solarMonitorApp);
        solarMonitorApp.setObjectGraph(testObjectGraph);
        testObjectGraph.inject(this);

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
                                                  solarMonitorApp.getSolarCustomerId(),
                                                  solarOutputService.getApiKey());

        mockSolarOutputServer.beginUsingMockServer();

        // This is the only way in which our Test APK deviates from production.  We need to
        // point our service to the mock endpoint (mockWebServer)
        SolarOutputService.API_ENDPOINT_BASE_URL = mockSolarOutputServer.getMockSolarOutputServerURL();

        activityRule.launchActivity(new Intent());

        Thread.sleep(6000000);
    }

    // Here we are injecting a 'mock' ObjectGraph which gives us the chance to mock out
    // some hardware components that our app depends upon.  The service layer in this
    // mock ObjectGraph is still the real implementation.
    //
    // Pros - Allows us to run tests on a device that might not have all required production
    // components (e.g. emulators do not have Bluetooth).  This is useful for IOT testing.
    //
    // Cons - Can be incorrectly used to replace proper unit testing.  Unit tests are a
    // much faster way to test front-end components.  So we still use the real service
    // layer here!
    @Test
    public void retrieveSolarOutput_realService_mockHardware_realEndpoint() throws Exception {

        // Context of the app under test.
        SolarMonitorApp solarMonitorApp = (SolarMonitorApp) getInstrumentation().getTargetContext().getApplicationContext();

        // By creating the graph here BEFORE it's used by the activity, we can inject our mock collaborators.
        MockObjectGraph mockObjectGraph = MockObjectGraph.Initializer.init(solarMonitorApp);
        solarMonitorApp.setObjectGraph(mockObjectGraph);
        mockObjectGraph.inject(this);

        // Now that we've injected our test ObjectGraph, we can configure our mocks...
        when(bluetoothService.getSomething()).thenReturn(Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> subscriber) throws Exception {
                subscriber.onSuccess("mock bluetooth here!");
            }
        }));

        activityRule.launchActivity(new Intent());

        Thread.sleep(6000000);
    }
}
