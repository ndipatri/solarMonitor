package com.ndipatri.solarmonitor;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.ndipatri.solarmonitor.activities.MainActivity;
import com.ndipatri.solarmonitor.container.TestObjectGraph;
import com.ndipatri.solarmonitor.dto.GetSolarOutputResponse;
import com.ndipatri.solarmonitor.services.SolarOutputService;
import com.ndipatri.solarmonitor.services.SolarOutputServiceImpl;

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

    @Inject
    SolarOutputService solarOutputService;

    // Here we are injecting a 'test' ObjectGraph which gives us the chance to inject
    // a mock service layer ... so our tests do NOT test our service layer
    @Test
    public void retrieveSolarOutput_mockService() throws Exception {

        // Context of the app under test.
        SolarMonitorApp solarMonitorApp = (SolarMonitorApp) getInstrumentation().getTargetContext().getApplicationContext();

        // By creating the graph here BEFORE it's used by the activity, we can inject our mock collaborators.
        TestObjectGraph testObjectGraph = TestObjectGraph.Initializer.init(solarMonitorApp);

        solarMonitorApp.setObjectGraph(testObjectGraph);

        testObjectGraph.inject(this);

        // Now that we've injected our test ObjectGraph, we can configure our mocks...
        when(solarOutputService.getSolarOutput("123")).thenReturn(Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> subscriber) throws Exception {
                subscriber.onSuccess("123 kW");
            }
        }));

        activityRule.launchActivity(new Intent());

        Thread.sleep(6000000);
    }

    // Here we use our production ObjectGraph with our real service layer.  This service
    // layer requires remote RESTful endpoints.. for this, we use MockWebServer.
    @Test
    public void retrieveSolarOutput_realService_mockEndpoint() throws Exception {

        // We deploy a MockWebServer to the same virtual machine as our
        // target APK
        MockSolarOutputServer mockSolarOutputServer = new MockSolarOutputServer();

        GetSolarOutputResponse getSolarOutputResponse = new GetSolarOutputResponse();
        getSolarOutputResponse.setOutput("331");
        getSolarOutputResponse.setUnits("kW");

        mockSolarOutputServer.enqueueDesiredSolarOutputResponse(getSolarOutputResponse);
        mockSolarOutputServer.beginUsingMockServer();

        // This is the only way in which our Test APK deviates from production.  We need to
        // point our service to the mock endpoint (mockWebServer)
        SolarOutputServiceImpl.API_ENDPOINT_BASE_URL = mockSolarOutputServer.getMockSolarOutputServerURL();

        activityRule.launchActivity(new Intent());

        Thread.sleep(6000000);
    }
}
