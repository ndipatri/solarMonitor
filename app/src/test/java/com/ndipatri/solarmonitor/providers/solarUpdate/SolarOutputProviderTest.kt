package com.ndipatri.solarmonitor.providers.solarUpdate

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.CurrentPower
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.GetOverviewResponse
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.LifeTimeData
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.Overview
import com.ndipatri.solarmonitor.utils.CoroutinesTestRule
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.junit.rules.ExpectedException



@RunWith(MockitoJUnitRunner::class)
class SolarOutputProviderTest {

    // This is in case postValue() is used with LiveData: the action will not be scheduled
    // but will be instead done immediately (as if setValue() was called).
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var exception = ExpectedException.none()

    //  This is to Dispatchers.main will return main thread of this jUnit test.
    @get:Rule
    var coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var mockSolarOutputRESTService: SolarOutputProvider.SolarOutputRESTInterface

    @Mock
    private lateinit var mockGetOverviewResponse: GetOverviewResponse

    @Mock
    private lateinit var mockOverview: Overview

    @Mock
    private lateinit var mockCurrentPower: CurrentPower

    @Mock
    private lateinit var mockLifeTimeData: LifeTimeData

    // Object under test
    private lateinit var solarOutputProvider: SolarOutputProvider

    @Before
    fun setup() {

        solarOutputProvider = SolarOutputProvider("456")
    }

    @Test
    fun solarOutput_happyPath()  {

        runBlockingTest {
            whenever(mockSolarOutputRESTService.getOverview("123", "456")).thenReturn(
                    mockGetOverviewResponse
            )
        }

        whenever(mockGetOverviewResponse.overview).thenReturn(
            mockOverview
        )
        whenever(mockOverview.currentPower).thenReturn(
                mockCurrentPower
        )
        whenever(mockCurrentPower.power).thenReturn(
                20.0
        )
        whenever(mockOverview.lifeTimeData).thenReturn(
                mockLifeTimeData
        )
        whenever(mockLifeTimeData.energy).thenReturn(
                3000.0
        )

        solarOutputProvider.solarOutputRESTInterface = mockSolarOutputRESTService

        runBlockingTest {
            var powerOutput  = solarOutputProvider.getSolarOutput("123")
            assertEquals(20.0, powerOutput.currentPowerInWatts)
            assertEquals(3000.0, powerOutput.lifetimePowerInWattHours)
        }
    }

    @Test
    fun solarOutput_endpointTimeout()  {

        runBlockingTest {
            Thread.sleep(10000)
            whenever(mockSolarOutputRESTService.getOverview("123", "456")).thenReturn(
                    mockGetOverviewResponse
            )
        }

        solarOutputProvider.solarOutputRESTInterface = mockSolarOutputRESTService

        runBlockingTest {
            exception.expect(TimeoutCancellationException::class.java)
            solarOutputProvider.getSolarOutput("123")
        }
    }
}