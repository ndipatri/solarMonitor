package com.ndipatri.solarmonitor.activities

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import android.os.Bundle
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.container.ObjectGraph
import com.ndipatri.solarmonitor.providers.customer.Customer
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class MainActivityViewModelTest {

    // This is in case postValue() is used with LiveData: the action will not be scheduled
    // but will be instead done immediately (as if setValue() was called).
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

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
        `when`(mockContext.objectGraph).thenReturn(mock(ObjectGraph::class.java))

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

    @Test
    fun stupidActivityTest() {
        var mainActivity = MainActivity()

        mainActivity.onCreate(mock(Bundle::class.java))
    }

    @Test
    fun idleState() {

        // Confirm that the steady state is IDLE
        assertEquals(MainActivityViewModel.USER_STATE.IDLE, viewModel.userState.value)

        // Confirm that as soon as this state is observed, it is delivered IDLE
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }

    @Test
    fun scanningState_waitingForScanResults() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber -> {}})

        viewModel.scanForNearbyPanel()

        assertEquals(MainActivityViewModel.USER_STATE.SCANNING, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.SCANNING)
    }

    @Test
    fun scanningState_results_configuredPanelFound() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we scan for panel, we will get an immediate response
            subscriber.onSuccess(Panel("123456"))
        })

        viewModel.scanForNearbyPanel()

        assertEquals(MainActivityViewModel.USER_STATE.LOAD, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.LOAD)
    }

    @Test
    fun scanningState_results_unconfiguredPanelFound() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we scan for panel, we will get an immediate response
            subscriber.onSuccess(Panel("someWrongValue"))
        })

        viewModel.scanForNearbyPanel()

        assertEquals(MainActivityViewModel.USER_STATE.CONFIGURE, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.CONFIGURE)
    }

    @Test
    fun scanningState_results_noPanelFound_storedPanelExists() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we scan for panel, we will indicate not panel was found
            subscriber.onComplete()
        })

        // In the case of no nearby panel, we then try to load a persisted panel that
        // we've scanned in the past... so let's set that up
        `when`(mockPanelProvider.getStoredPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we look for stored panel, we will indicate that one was stored...
            subscriber.onSuccess(Panel("123"))
        })

        `when`(mockContext.getString(R.string.no_nearby_panels_were_found)).thenReturn("first test message")
        `when`(mockContext.getString(R.string.using_stored_panel)).thenReturn("second test message")


        viewModel.scanForNearbyPanel()

        verify(mockUserMessageObserver).onChanged("first test message")
        verify(mockUserMessageObserver).onChanged("second test message")

        // Since no panel was found, but a stored one was ...
        assertEquals(MainActivityViewModel.USER_STATE.LOAD, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.LOAD)
    }

    @Test
    fun scanningState_results_noPanelFound_storedPanelDoesNotExist() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we scan for panel, we will indicate not panel was found
            subscriber.onComplete()
        })

        // In the case of no nearby panel, we then try to load a persisted panel that
        // we've scanned in the past... so let's set that up
        `when`(mockPanelProvider.getStoredPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we look for stored panel, we will indicate not panel was stored either...
            subscriber.onComplete()
        })

        `when`(mockContext.getString(R.string.no_nearby_panels_were_found)).thenReturn("test message")


        viewModel.scanForNearbyPanel()

        verify(mockUserMessageObserver).onChanged("test message")

        // Since no panel was found and no stored panel exists
        assertEquals(MainActivityViewModel.USER_STATE.IDLE, viewModel.userState.value)
        verify(mockUserStateObserver, times(2)).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }

    @Test
    fun scanningState_results_error() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber ->

            subscriber.onError(Exception())
        })

        // In the case of a scan error, we then try to load a persisted panel that
        // we've scanned in the past... so let's set that up
        `when`(mockPanelProvider.getStoredPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we look for stored panel, we will indicate not panel was stored either...
            subscriber.onComplete()
        })

        `when`(mockContext.getString(R.string.error_please_try_again)).thenReturn("test message")

        viewModel.scanForNearbyPanel()

        verify(mockUserMessageObserver).onChanged("test message")

        // Since no panel was found and no stored panel exists
        assertEquals(MainActivityViewModel.USER_STATE.IDLE, viewModel.userState.value)
        verify(mockUserStateObserver, times(2)).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }

    @Test
    fun loadingState_waitingForScanResults() {
        // we'll assume this was done in previous step
        var scannedPanel = Panel("123")
        viewModel.scannedPanel = scannedPanel

        `when`(mockSolarOutputProvider.getSolarOutput(ArgumentMatchers.anyString())).thenReturn(Single.create { subscriber -> {}})
        `when`(mockCustomerProvider.findCustomerForPanel(ArgumentMatchers.anyString())).thenReturn(Single.create { subscriber -> {}})

        viewModel.loadSolarOutput()

        assertEquals(MainActivityViewModel.USER_STATE.LOADING, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.LOADING)
    }

    @Test
    fun loadingState_results_success() {
        // we'll assume this was done in previous step
        var scannedPanel = Panel("123")
        viewModel.scannedPanel = scannedPanel

        `when`(mockSolarOutputProvider.getSolarOutput(ArgumentMatchers.anyString())).thenReturn(Single.create { subscriber ->

            // assume we can receive power output from provider ...
            subscriber.onSuccess(PowerOutput(1230.0, 4560.0))
        })

        `when`(mockCustomerProvider.findCustomerForPanel("123")).thenReturn(Single.just(Customer("Customer 123", .13671)))

        viewModel.loadSolarOutput()

        assertEquals(MainActivityViewModel.USER_STATE.LOADED, viewModel.userState.value)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.LOADED)



        var expectedPowerOutputMessage = "Current ($0.17/hour), Lifetime($0.62)"
        assertEquals(expectedPowerOutputMessage, viewModel.powerOutputMessage.value)
        verify(mockPowerOutputMessage).onChanged(expectedPowerOutputMessage)
    }

    @Test
    fun loadingState_results_error() {
        // we'll assume this was done in previous step
        var scannedPanel = Panel("123")
        viewModel.scannedPanel = scannedPanel

        `when`(mockSolarOutputProvider.getSolarOutput(ArgumentMatchers.anyString())).thenReturn(Single.create { subscriber ->

            subscriber.onError(Exception())
        })

        // upon any failure, we always try to load stored panel ... so make that fail too
        `when`(mockPanelProvider.getStoredPanel()).thenReturn(Maybe.create {subscriber ->

            subscriber.onError(Exception())
        })

        `when`(mockCustomerProvider.findCustomerForPanel("123")).thenReturn(Single.just(Customer("Customer 123", .13671)))

        `when`(mockContext.getString(R.string.error_please_try_again)).thenReturn("test message")

        viewModel.loadSolarOutput()

        verify(mockUserMessageObserver).onChanged("test message")

        assertEquals(MainActivityViewModel.USER_STATE.IDLE, viewModel.userState.value)
        verify(mockUserStateObserver, times(2)).onChanged(MainActivityViewModel.USER_STATE.IDLE)   }
}

