package com.ndipatri.solarmonitor.activities

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import android.widget.Toast
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
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import java.text.NumberFormat
import javax.inject.Inject

open class MainActivityViewModel(context: Application) : AndroidViewModel(context) {

    init {
        SolarMonitorApp.instance.objectGraph.inject(this)
    }

    @Inject
    lateinit var solarOutputProvider: SolarOutputProvider

    @Inject
    lateinit var panelProvider: PanelProvider

    @Inject
    lateinit var customProvider: CustomerProvider


    var userState = MutableLiveData<USER_STATE>().also { it.setValue(USER_STATE.IDLE) }

    var scannedPanel = MutableLiveData<Panel>()

    var powerOutputMessage = MutableLiveData<String>()

    private var disposable: Disposable? = null

    enum class USER_STATE {
        IDLE,
        SCANNING, // user is scanning for nearby panel
        CONFIGURE, // panel found but it needs to be configured.
        LOAD, // user has nearby configured panel, but hasn't loaded output
        LOADING, // user is loading solar output
        LOADED
    } // user is viewing loaded solar output


    fun resetToSteadyState() {
        panelProvider.getStoredPanel().subscribe(object : MaybeObserver<Panel> {
            override fun onSuccess(storedPanel: Panel) {

                // For now, we pretend we scanned for the stored panel. The user
                // can scan for new panels as they wish
                this@MainActivityViewModel.scannedPanel.value = storedPanel
                userState.value = USER_STATE.LOAD
            }

            override fun onComplete() {
                userState.value = USER_STATE.IDLE
            }

            override fun onSubscribe(disposable: Disposable) {
                this@MainActivityViewModel.disposable = disposable
            }

            override fun onError(e: Throwable?) {
                userState.value = USER_STATE.IDLE
            }
        })
    }

    fun scanForNearbyPanel() {

        userState.value = USER_STATE.SCANNING

        panelProvider.scanForNearbyPanel().subscribe(object : MaybeObserver<Panel> {
            override fun onSubscribe(disposable: Disposable) {
                this@MainActivityViewModel.disposable = disposable
            }

            override fun onSuccess(scannedPanel: Panel) {
                this@MainActivityViewModel.scannedPanel.value = scannedPanel

                if (scannedPanel.id?.length == 6) {
                    Log.d(TAG, "Panel found with id configured ${scannedPanel.id}.")

                    userState.value = USER_STATE.LOAD
                } else {
                    Log.d(TAG, "Panel found, but it needs to be configured.")

                    userState.value = USER_STATE.CONFIGURE
                }
            }

            override fun onError(e: Throwable) {
                Toast.makeText(this@MainActivityViewModel.getApplication(), this@MainActivityViewModel.getApplication<Application>().getString(R.string.error_please_try_again), Toast.LENGTH_SHORT).show()

                resetToSteadyState()
            }

            override fun onComplete() {
                Toast.makeText(this@MainActivityViewModel.getApplication(), this@MainActivityViewModel.getApplication<Application>().getString(R.string.no_nearby_panels_were_found), Toast.LENGTH_SHORT).show()

                resetToSteadyState()
            }
        })
    }

    fun loadSolarOutput() {
        scannedPanel.value?.apply {

            userState.setValue(USER_STATE.LOADING)

            solarOutputProvider.getSolarOutput(this.id)
                    .zipWith(customProvider.lookupCustomer(this.id),
                            BiFunction<PowerOutput, Customer, String> { powerOutput: PowerOutput, customer: Customer ->

                                val currencyFormat = NumberFormat.getCurrencyInstance()

                                var currentProduction = "unavailable"
                                powerOutput.currentPowerInWattHours?.let {
                                    currentProduction = currencyFormat.format(it/1000 * customer.dollarsPerkWh)
                                }

                                var lifetimeProduction = "unavailable"
                                powerOutput.lifetimePowerInWattHours?.let {
                                    lifetimeProduction = currencyFormat.format(it/1000 * customer.dollarsPerkWh)
                                }

                                "Current ($currentProduction/hour), Lifetime($lifetimeProduction)"
                            })
                    .subscribe(object : SingleObserver<String> {
                        override fun onSuccess(powerOutputMessage: String?) {
                            this@MainActivityViewModel.powerOutputMessage.value = powerOutputMessage

                            userState.value = USER_STATE.LOADED
                        }

                        override fun onSubscribe(d: Disposable?) {
                            this@MainActivityViewModel.disposable = disposable
                        }

                        override fun onError(e: Throwable?) {
                            Toast.makeText(this@MainActivityViewModel.getApplication(), this@MainActivityViewModel.getApplication<Application>().getString(R.string.error_please_try_again), Toast.LENGTH_SHORT).show()

                            resetToSteadyState()
                        }
                    })
        }
    }

    override fun onCleared() {
        super.onCleared()

        disposable?.dispose()
    }

    companion object {
        private val TAG = MainActivityViewModel::class.java.simpleName
    }
}