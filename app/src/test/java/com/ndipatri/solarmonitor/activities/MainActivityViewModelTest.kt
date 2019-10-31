package com.ndipatri.solarmonitor.activities

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.container.ObjectGraph
import com.ndipatri.solarmonitor.providers.customer.Customer
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput
import com.ndipatri.solarmonitor.utils.CoroutinesTestRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeoutException


@RunWith(MockitoJUnitRunner::class)
class MainActivityViewModelTest {

    // This is in case postValue() is used with LiveData: the action will not be scheduled
    // but will be instead done immediately (as if setValue() was called).
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    //  This is to Dispatchers.main will return main thread of this jUnit test.
    @get:Rule
    var coroutinesTestRule = CoroutinesTestRule()

    // Object under test
    private lateinit var viewModel: MainActivityViewModel

    // All necessary mocks...
    @Mock
    private lateinit var mockContext: SolarMonitorApp

    @Mock
    private lateinit var mockUserStateObserver: Observer<MainActivityViewModel.USER_STATE>

    @Mock
    private lateinit var mockUserMessageObserver: Observer<String>

    @Mock
    private lateinit var mockPowerOutputMessage: Observer<String>

    @Mock
    private lateinit var mockPanelProvider: PanelProvider

    @Mock
    private lateinit var mockCustomerProvider: CustomerProvider

    @Mock
    private lateinit var mockSolarOutputProvider: SolarOutputProvider

    @Before
    fun setup() {

        // Fake out Dagger - we will be manually injecting any required collaborators as mocks
        // in this test suite
        SolarMonitorApp.instance = mockContext
        whenever(mockContext.objectGraph).thenReturn(mock(ObjectGraph::class.java))

        viewModel = MainActivityViewModel(mockContext)

        viewModel.panelProvider = mockPanelProvider
        viewModel.solarOutputProvider = mockSolarOutputProvider
        viewModel.customerProvider = mockCustomerProvider

        // 'observerForever' is how you observe LiveData when you don't have a
        // LifecycleAware object such as an Activity or Fragment.
        viewModel.userState.observeForever(mockUserStateObserver)
        viewModel.userMessage.observeForever(mockUserMessageObserver)
        viewModel.powerOutputMessage.observeForever(mockPowerOutputMessage)
    }

    @After
    fun teardown() {
        viewModel.userState.removeObserver(mockUserStateObserver)
        viewModel.userMessage.removeObserver(mockUserMessageObserver)
        viewModel.powerOutputMessage.removeObserver(mockPowerOutputMessage)
    }

    @Test
    fun idleState() {

        // Confirm that the steady state is IDLE
        assertEquals(MainActivityViewModel.USER_STATE.IDLE, viewModel.userState.value)

        // Confirm that as soon as this state is observed, it is delivered IDLE
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }

    @Test
    fun scanningState_waitingForResults() {

        // This guarantees that background work will be done not on test thread.
        // since we are not mocking any response, none will be given...
        Dispatchers.setMain(Dispatchers.IO)

        viewModel.scanForNearbyPanel()
        assertEquals(MainActivityViewModel.USER_STATE.SCANNING, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.SCANNING)
    }

    @Test
    fun scanningState_results_configuredPanelFound() {
        // although not necessary for this test, this launcher gives us extra control
        // over  our tests (e.g. time shifting)
        runBlockingTest {
            whenever(mockPanelProvider.scanForNearbyPanel()).thenReturn(

                // Now when we scan for panel, we will get an immediate response
                Panel("123456")
            )
        }

        viewModel.scanForNearbyPanel()

        assertEquals(MainActivityViewModel.USER_STATE.LOAD, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.LOAD)
    }

    @Test
    fun scanningState_results_unconfiguredPanelFound() {
        runBlockingTest {
            whenever(mockPanelProvider.scanForNearbyPanel()).thenReturn(

                    // Now when we scan for panel, we will get an immediate response
                    Panel("someWrongValue")
            )
        }

        viewModel.scanForNearbyPanel()

        assertEquals(MainActivityViewModel.USER_STATE.CONFIGURE, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.CONFIGURE)
    }

    @Test
    fun scanningState_results_noPanelFound_storedPanelExists() {
        runBlocking {
            whenever(mockPanelProvider.scanForNearbyPanel()).thenReturn(

                // Now when we scan for panel, we will indicate not panel was found
                null
            )
        }

        // In the case of no nearby panel, we then try to load a persisted panel that
        // we've scanned in the past... so let's set that up
        runBlocking {
            whenever(mockPanelProvider.getStoredPanel()).thenReturn(

                // Now when we look for stored panel, we will indicate that one was stored...
                Panel("123")
            )
        }

        whenever(mockContext.getString(R.string.no_nearby_panels_were_found)).thenReturn("first test message")
        whenever(mockContext.getString(R.string.using_stored_panel)).thenReturn("second test message")


        viewModel.scanForNearbyPanel()

        verify(mockUserMessageObserver).onChanged("first test message")
        verify(mockUserMessageObserver).onChanged("second test message")

        // Since no panel was found, but a stored one was ...
        assertEquals(MainActivityViewModel.USER_STATE.LOAD, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.LOAD)
    }

    @Test
    fun scanningState_results_noPanelFound_storedPanelDoesNotExist() {
        runBlocking {
            whenever(mockPanelProvider.scanForNearbyPanel()).thenReturn(

                // Now when we scan for panel, we will indicate not panel was found
                null
            )

            // In the case of no nearby panel, we then try to load a persisted panel that
            // we've scanned in the past... so let's set that up
            whenever(mockPanelProvider.getStoredPanel()).thenReturn(

                // Now when we look for stored panel, we will indicate not panel was stored either...
                null
            )
        }

        whenever(mockContext.getString(R.string.no_nearby_panels_were_found)).thenReturn("test message")

        viewModel.scanForNearbyPanel()

        verify(mockUserMessageObserver).onChanged("test message")

        // Since no panel was found and no stored panel exists
        assertEquals(MainActivityViewModel.USER_STATE.IDLE, viewModel.userState.value)
        verify(mockUserStateObserver, times(2)).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }

    @Test
    fun scanningState_results_error() {
        runBlockingTest {
            doAnswer { throw TimeoutException() }
                    .whenever(mockPanelProvider).scanForNearbyPanel()
        }

        runBlocking {
            // In the case of a scan error, we then try to load a persisted panel that
            // we've scanned in the past... so let's set that up
            whenever(mockPanelProvider.getStoredPanel()).thenReturn(

                // Now when we look for stored panel, we will indicate not panel was stored either...
                null
            )
        }

        whenever(mockContext.getString(R.string.error_please_try_again)).thenReturn("test message")

        viewModel.scanForNearbyPanel()

        verify(mockUserMessageObserver).onChanged("test message")

        // Since no panel was found and no stored panel exists
        assertEquals(MainActivityViewModel.USER_STATE.IDLE, viewModel.userState.value)
        verify(mockUserStateObserver, times(2)).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }

    @Test
    fun loadingState_results_success() {
        // we'll assume this was done in previous step
        var scannedPanel = Panel("123")
        viewModel.scannedPanel = scannedPanel

        runBlocking() {

            whenever(mockSolarOutputProvider.getSolarOutput(ArgumentMatchers.anyString())).thenReturn(

                    // assume we can receive power output from provider ...
                    PowerOutput(1230.0, 4560.0)
            )

            whenever(mockCustomerProvider.findCustomerForPanel("123")).thenReturn(
                    Customer("Customer 123", .13671)
            )
        }

        viewModel.loadSolarOutput()

        assertEquals(MainActivityViewModel.USER_STATE.LOADED, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.LOADED)

        var expectedPowerOutputMessage = "Current ($0.17/hour), Annual ($0.62)"
        assertEquals(expectedPowerOutputMessage, viewModel.powerOutputMessage.value)
        verify(mockPowerOutputMessage).onChanged(expectedPowerOutputMessage)
    }

    @Test
    fun loadingState_results_error() {

        // we'll assume this was done in previous step
        var scannedPanel = Panel("123")
        viewModel.scannedPanel = scannedPanel

        // This is the only way to make mocks throw Exceptions with kotlin
        runBlockingTest {
            doAnswer { throw Exception() }
                    .whenever(mockSolarOutputProvider)
                    .getSolarOutput(ArgumentMatchers.anyString())
        }

        runBlockingTest {
            // upon any failure, we always try to load stored panel ... so make that fail too
            doAnswer { throw Exception() }
                    .whenever(mockPanelProvider)
                    .getStoredPanel()
        }

        whenever(mockContext.getString(R.string.error_please_try_again)).thenReturn("error message")

        viewModel.loadSolarOutput()

        verify(mockUserMessageObserver).onChanged("error message")

        assertEquals(MainActivityViewModel.USER_STATE.IDLE, viewModel.userState.value)
        verify(mockUserStateObserver, times(2)).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }
}

