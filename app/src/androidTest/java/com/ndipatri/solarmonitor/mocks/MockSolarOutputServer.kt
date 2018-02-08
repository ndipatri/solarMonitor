package com.ndipatri.solarmonitor.mocks

import com.google.gson.Gson

import java.io.IOException
import java.net.MalformedURLException
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.TimeUnit

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

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
class MockSolarOutputServer {

    private val mockWebServer: MockWebServer

    private val DEFAULT_BODY_RESPONSE_DELAY_MILLIS: Long = 1000

    var mockSolarOutputServerURL: String? = null
        private set

    /**
     * For Weather Server, we will assume all requests are GET requests...
     */
    internal var requestToMockResponseMap: MutableMap<String, ArrayList<MockResponse>> = HashMap()
    internal val dispatcher: Dispatcher = object : Dispatcher() {

        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {

            val requestURL = request.path

            val orderedResponses = requestToMockResponseMap[requestURL]
            return if (null == orderedResponses || orderedResponses.isEmpty()) {
                MockResponse().setResponseCode(404)
            } else {
                orderedResponses.removeAt(0)
            }
        }
    }

    init {
        mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)
    }

    fun enqueueDesiredSolarOutputResponse(responseDTO: Any, siteId: String, apiKey: String) {
        enqueueDesiredAPIResponse("/site/$siteId/overview.json?api_key=$apiKey", responseDTO, 200, DEFAULT_BODY_RESPONSE_DELAY_MILLIS)
    }

    /**
     * These will be 'played back' in the order in which they are enqueued.
     *
     * @param responseDTO
     */
    private fun enqueueDesiredAPIResponse(requestPath: String, responseDTO: Any?, responseCode: Int, bodyDelayMillis: Long) {

        val mockResponse = MockResponse().setResponseCode(responseCode)
        if (null != responseDTO) {
            val gson = Gson()
            val jsonResponseString = gson.toJson(responseDTO)
            mockResponse.setBody(jsonResponseString).setBodyDelay(bodyDelayMillis, TimeUnit.MILLISECONDS)
        }

        var orderedResponses: MutableList<MockResponse>? = requestToMockResponseMap[requestPath]
        if (null == orderedResponses) {
            orderedResponses = ArrayList()
            requestToMockResponseMap[requestPath] = orderedResponses
        }
        orderedResponses.add(mockResponse)
    }

    @Throws(MalformedURLException::class)
    fun beginUsingMockServer() {

        // Ask the server for its URL. You'll need this to make HTTP requests.
        mockSolarOutputServerURL = mockWebServer.url("/").toString()

        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    fun endUsingMockServer() {
        mockWebServer.shutdown()
    }
}
