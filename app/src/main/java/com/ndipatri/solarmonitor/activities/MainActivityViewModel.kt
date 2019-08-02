package com.ndipatri.solarmonitor.activities

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.UI_COMFORT_DELAY
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import io.reactivex.MaybeObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import javax.inject.Inject

/**
 * Notice this class imports nothing from 'android.widget': it implies no view technology
 */

open class MainActivityViewModel(context: Application) : AndroidViewModel(context) {

    var userState = MutableLiveData<USER_STATE>().also { it.setValue(USER_STATE.IDLE) }
    var scannedPanel: Panel? = null
    var powerOutputMessage = MutableLiveData<String>()
    var userMessage = MutableLiveData<String>()

    init {
        SolarMonitorApp.instance.objectGraph.inject(this)
    }

    @Inject
    lateinit var solarOutputProvider: SolarOutputProvider

    @Inject
    lateinit var panelProvider: PanelProvider

    @Inject
    lateinit var customerProvider: CustomerProvider

    private var compositeDisposable: CompositeDisposable? = null

    enum class USER_STATE {
        IDLE,
        SCANNING, // user is scanning for nearby panel
        CONFIGURE, // panel found but it needs to be configured.
        LOAD, // user has nearby configured panel, but hasn't loaded output
        LOADING, // user is loading solar output
        LOADED
    } // user is viewing loaded solar output

    open fun resetToSteadyState() {

        viewModelScope.launch {
            try {
                panelProvider.getStoredPanel()?.apply {
                    // For now, we pretend we scanned for the stored panel. The user
                    // can scan for new panels as they wish
                    this@MainActivityViewModel.scannedPanel = this

                    userMessage.value = this@MainActivityViewModel.getApplication<Application>().getString(R.string.using_stored_panel)
                    userState.value = USER_STATE.LOAD
                } ?: let {
                    userState.value = USER_STATE.IDLE
                }
            } catch (th: Throwable) {
                userState.value = USER_STATE.IDLE
            }
        }
    }

    open fun scanForNearbyPanel() {

        userState.value = USER_STATE.SCANNING

        viewModelScope.launch {

            // we're still on main thread here. But now that we're in a coroutine,
            // this code block itself can be suspended as we call suspend functions.

            try {
                // This is a suspendable function that is 'main safe' so nothing to do here
                // but to call it. 'main safe' means that if this function has to do some
                // 'background' work, it will switch threads itself either through
                // 'withContext' or 'async' or 'launch'
                var scannedPanel = panelProvider.scanForNearbyPanel()

                scannedPanel?.apply {
                    this@MainActivityViewModel.scannedPanel = scannedPanel

                    if (scannedPanel.id?.length == 6) {
                        Log.d(TAG, "Panel found with id configured ${scannedPanel.id}.")

                        userState.value = USER_STATE.LOAD
                    } else {
                        Log.d(TAG, "Panel found, but it needs to be configured.")

                        userState.value = USER_STATE.CONFIGURE
                    }
                } ?: let {
                    userMessage.value = this@MainActivityViewModel.getApplication<Application>()
                            .getString(R.string.no_nearby_panels_were_found)

                    resetToSteadyState()
                }
            } catch (e: Exception) {
                userMessage.value = this@MainActivityViewModel.getApplication<Application>().getString(R.string.error_please_try_again)
                Log.e(TAG, "Exception while scanning for panel.", e)

                resetToSteadyState()
            }
        }
    }

    open fun loadSolarOutput() {

        scannedPanel?.also {

            userState.value = USER_STATE.LOADING

            viewModelScope.launch {

                // we're still on main thread here. But now that we're in a coroutine,
                // this code block itself can be suspended as we call suspend functions.

                try {

                    // This is a suspendable function that is 'main safe' so nothing to do here
                    // but to call it. 'main safe' means that if this function has to do some
                    // 'background' work, it will switch threads itself either through
                    // 'withContext' or 'async' or 'launch'
                    var powerOutputDeferred = async {solarOutputProvider.getSolarOutput(it.id)}

                    // same as above.
                    var customerDeferred = async {customerProvider.findCustomerForPanel(it.id)}

                    val currencyFormat = NumberFormat.getCurrencyInstance()

                    var currentProduction = "unavailable"
                    powerOutputDeferred.await().currentPowerInWatts?.let {
                        // We'll just assume the production of current output for one hour
                        currentProduction = currencyFormat.format(it / 1000 * customerDeferred.await().dollarsPerkWh)
                    }

                    var lifetimeProduction = "unavailable"
                    powerOutputDeferred.await().lifetimePowerInWattHours?.let {
                        lifetimeProduction = currencyFormat.format(it / 1000 * customerDeferred.await().dollarsPerkWh)
                    }

                    this@MainActivityViewModel
                            .powerOutputMessage.value = "Current ($currentProduction/hour), Annual ($lifetimeProduction)"

                    userState.value = USER_STATE.LOADED
                } catch (e: Throwable) {
                    userMessage.value = this@MainActivityViewModel.getApplication<Application>().getString(R.string.error_please_try_again)
                    Log.e(TAG, "Exception while loading output.", e)

                    // This is another coroutine that will allow this code block to yield the main
                    // thread for a bit..
                    // NJD TODO - wtf.. why does following line cause test to fail:
                    // MainActivityViewModelTest.loadingState_results_error()
                    //delay(UI_COMFORT_DELAY)

                    // .. then after that delay, we update our UI state.
                    resetToSteadyState()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        compositeDisposable?.dispose()
    }

    companion object {
        private val TAG = MainActivityViewModel::class.java.simpleName
    }
}