package com.ndipatri.solarmonitor;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Proper usage of this class:
 *
 * 1. Instantiate
 * 2. Enqueue desired responses for each expected request.
 * 3. beginUsingMockServer()
 * 4. conduct testing using this mock server.
 * 5. endUsingMockServer()
 *
 */
public class MockSolarOutputServer {

    private MockWebServer mockWebServer;

    private long DEFAULT_BODY_RESPONSE_DELAY_MILLIS = 1000;

    private String mockSolarOutputServerURL = null;

    public MockSolarOutputServer() {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);
    }

    public void enqueueDesiredSolarOutputResponse(Object responseDTO, String siteId, String apiKey) {
        enqueueDesiredAPIResponse("/site/" + siteId + "/overview.json?api_key=" + apiKey, responseDTO, 200, DEFAULT_BODY_RESPONSE_DELAY_MILLIS);
    }

    /**
     * These will be 'played back' in the order in which they are enqueued.
     *
     * @param responseDTO
     */
    private void enqueueDesiredAPIResponse(final String requestPath, final Object responseDTO, int responseCode, long bodyDelayMillis) {

        MockResponse mockResponse = new MockResponse().setResponseCode(responseCode);
        if (null != responseDTO) {
            Gson gson = new Gson();
            String jsonResponseString = gson.toJson(responseDTO);
            mockResponse.setBody(jsonResponseString).setBodyDelay(bodyDelayMillis, TimeUnit.MILLISECONDS);
        }

        List<MockResponse> orderedResponses = requestToMockResponseMap.get(requestPath);
        if (null == orderedResponses) {
            orderedResponses = new ArrayList<>();
            requestToMockResponseMap.put(requestPath, orderedResponses);
        }
        orderedResponses.add(mockResponse);
    }

    public void beginUsingMockServer() throws MalformedURLException {

        // Ask the server for its URL. You'll need this to make HTTP requests.
        mockSolarOutputServerURL = mockWebServer.url("/").toString();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void endUsingMockServer() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * For Weather Server, we will assume all requests are GET requests...
     */
    Map<String, List<MockResponse>> requestToMockResponseMap = new HashMap<>();
    final Dispatcher dispatcher = new Dispatcher() {

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

            String requestURL = request.getPath();

            List<MockResponse> orderedResponses = requestToMockResponseMap.get(requestURL);
            if (null == orderedResponses || orderedResponses.isEmpty()) {
                return new MockResponse().setResponseCode(404);
            } else {
                return orderedResponses.remove(0);
            }
        }
    };

    public String getMockSolarOutputServerURL() {
        return mockSolarOutputServerURL;
    }
}
