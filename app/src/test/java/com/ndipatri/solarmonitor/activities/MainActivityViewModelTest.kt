package com.ndipatri.solarmonitor.activities

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.container.ObjectGraph
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import io.reactivex.Maybe
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
    private lateinit var mockPanelProvider: PanelProvider

    @Before
    fun setup() {

        // Fake out Dagger - we will be manually injecting any required collaborators as mocks
        // in this test suite
        SolarMonitorApp.instance = mockContext
        `when`(mockContext.objectGraph).thenReturn(mock(ObjectGraph::class.java))

        viewModel = MainActivityViewModel(mockContext)

        viewModel.panelProvider = mockPanelProvider

        viewModel.userState.observeForever(mockUserStateObserver)
        viewModel.userMessage.observeForever(mockUserMessageObserver)
    }

    @Test
    fun idleState() {

        // Confirm that the steady state is IDLE
        assertEquals(viewModel.userState.value, MainActivityViewModel.USER_STATE.IDLE)

        // Confirm that as soon as this state is observed, it is delivered IDLE
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }

    @Test
    fun scanningState_waitingForScanResults() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber -> {}})

        viewModel.scanForNearbyPanel()

        assertEquals(viewModel.userState.value, MainActivityViewModel.USER_STATE.SCANNING)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.SCANNING)
    }

    @Test
    fun scanningState_results_configuredPanelFound() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we scan for panel, we will get an immediate response
            subscriber.onSuccess(Panel("123456"))
        })

        viewModel.scanForNearbyPanel()

        assertEquals(viewModel.userState.value, MainActivityViewModel.USER_STATE.LOAD)
        verify(mockUserStateObserver).onChanged(MainActivityViewModel.USER_STATE.LOAD)
    }

    @Test
    fun scanningState_results_unconfiguredPanelFound() {
        `when`(mockPanelProvider.scanForNearbyPanel()).thenReturn(Maybe.create {subscriber ->

            // Now when we scan for panel, we will get an immediate response
            subscriber.onSuccess(Panel("someWrongValue"))
        })

        viewModel.scanForNearbyPanel()

        assertEquals(viewModel.userState.value, MainActivityViewModel.USER_STATE.CONFIGURE)
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
        assertEquals(viewModel.userState.value, MainActivityViewModel.USER_STATE.LOAD)
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
        assertEquals(viewModel.userState.value, MainActivityViewModel.USER_STATE.IDLE)
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
        assertEquals(viewModel.userState.value, MainActivityViewModel.USER_STATE.IDLE)
        verify(mockUserStateObserver, times(2)).onChanged(MainActivityViewModel.USER_STATE.IDLE)
    }

    // NJD TODO - need to complete testing 'loading' code

}

