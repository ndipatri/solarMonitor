package com.ndipatri.solarmonitor.activities

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.providers.customer.Customer
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput
import io.reactivex.MaybeObserver
import io.reactivex.SingleObserver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.*
import kotlinx.coroutines.rx2.await
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
        panelProvider.getStoredPanel().subscribe(object : MaybeObserver<Panel> {
            override fun onSuccess(storedPanel: Panel) {

                // For now, we pretend we scanned for the stored panel. The user
                // can scan for new panels as they wish
                this@MainActivityViewModel.scannedPanel = storedPanel

                userMessage.value = this@MainActivityViewModel.getApplication<Application>().getString(R.string.using_stored_panel)
                userState.value = USER_STATE.LOAD
            }

            override fun onComplete() {
                userState.value = USER_STATE.IDLE
            }

            override fun onSubscribe(disposable: Disposable) {
                this@MainActivityViewModel.compositeDisposable?.add(disposable)
            }

            override fun onError(e: Throwable) {
                userState.value = USER_STATE.IDLE
            }
        })
    }

    open fun scanForNearbyPanel() {

        userState.value = USER_STATE.SCANNING

        panelProvider.scanForNearbyPanel().subscribe(object : MaybeObserver<Panel> {
            override fun onSubscribe(disposable: Disposable) {
                this@MainActivityViewModel.compositeDisposable?.add(disposable)
            }

            override fun onSuccess(scannedPanel: Panel) {
                this@MainActivityViewModel.scannedPanel = scannedPanel

                if (scannedPanel.id?.length == 6) {
                    Log.d(TAG, "Panel found with id configured ${scannedPanel.id}.")

                    userState.value = USER_STATE.LOAD
                } else {
                    Log.d(TAG, "Panel found, but it needs to be configured.")

                    userState.value = USER_STATE.CONFIGURE
                }
            }

            override fun onError(e: Throwable) {
                userMessage.value = this@MainActivityViewModel.getApplication<Application>().getString(R.string.error_please_try_again)
                Log.e(TAG, "Exception while scanning for panel.", e)

                resetToSteadyState()
            }

            override fun onComplete() {
                userMessage.value = this@MainActivityViewModel.getApplication<Application>().getString(R.string.no_nearby_panels_were_found)

                resetToSteadyState()
            }
        })
    }

    open fun loadSolarOutput() {

        scannedPanel?.also {

            viewModelScope.launch {

                // we're still on main thread here.

                userState.setValue(USER_STATE.LOADING)

                try {

                    // This is a suspendable function that is 'main safe' so nothing to do here
                    // but to call it.
                    var powerOutputDeferred = solarOutputProvider.getSolarOutput(it.id)

                    // Here we're taking advantage of coroutine/rx2 bridge to convert Single to Deferred,
                    // and then waiting on that deferred.  While we wait, our current stack frame is
                    // suspended and the main thread is made available.
                    var customer = customerProvider.findCustomerForPanel(it.id).await()

                    val currencyFormat = NumberFormat.getCurrencyInstance()

                    var currentProduction = "unavailable"
                    powerOutputDeferred.currentPowerInWatts?.let {
                        // We'll just assume the production of current output for one hour
                        currentProduction = currencyFormat.format(it / 1000 * customer.dollarsPerkWh)
                    }

                    var lifetimeProduction = "unavailable"
                    powerOutputDeferred.lifetimePowerInWattHours?.let {
                        lifetimeProduction = currencyFormat.format(it / 1000 * customer.dollarsPerkWh)
                    }

                    this@MainActivityViewModel
                            .powerOutputMessage.value = "Current ($currentProduction/hour), Annual ($lifetimeProduction)"

                    userState.value = USER_STATE.LOADED
                } catch (e: Throwable) {
                    userMessage.value = this@MainActivityViewModel.getApplication<Application>().getString(R.string.error_please_try_again)
                    Log.e(TAG, "Exception while loading output.", e)

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