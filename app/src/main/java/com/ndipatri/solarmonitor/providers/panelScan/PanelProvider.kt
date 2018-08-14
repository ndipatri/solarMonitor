package com.ndipatri.solarmonitor.providers.panelScan


import android.content.Context
import android.util.Log
import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.iot.googleproximity.utils.BeaconScanHelper
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.persistence.AppDatabase
import com.ndipatri.solarmonitor.providers.CustomIdlingResource
import com.ndipatri.solarmonitor.providers.customer.Customer
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.MaybeSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class PanelProvider(var context: Context) {

    init {
        SolarMonitorApp.instance.objectGraph.inject(this)
    }

    open val idlingResource = CustomIdlingResource("panelProviderResource")

    val panelDao = AppDatabase.getInstance(context).scannedPanelDao()

    @Inject
    lateinit var customerProvider: CustomerProvider

    /**
     * Possible outcomes:
     *
     *
     * 1. found no panel (onComplete() without onSuccess())
     * 2. found new panel (onSuccess() with new Panel having default values)
     * 3. found configured panel (onSuccess() with existing Panel)
     * 4. threw error (onError())
     *
     *
     * This implementation is a bit complex.. I tried to use a simple chain of operators, but I
     * couldn't distinguish #2 and #3
     * should maybe try again sometime when i'm smarter...
     */
    private var scanForNearbyPanelSubject: MaybeSubject<Panel>? = null

    open fun scanForNearbyPanel(): Maybe<Panel> {

        // We call an external BLE scanning library which we know eventually returns
        // an async response on either success or failure... For this reason, we cannot
        // depend on RxPlugins for test thread synchronization.  So we use IdlingResource
        // We can safely do this (e.g. we won't hang the test thread) because we know this
        // library has a timeout eventually.
        idlingResource.updateIdleState(CustomIdlingResource.IS_NOT_IDLE)

        scanForNearbyPanelSubject = MaybeSubject.create()

        // A beacon with this namespace is, by definition, a panel
        val beaconNamespaceId = context.resources.getString(R.string.beaconNamespaceId)

        GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally { GoogleProximity.getInstance().stopBeaconScanning() }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : MaybeObserver<BeaconScanHelper.BeaconUpdate> {

                    override fun onSubscribe(d: Disposable) {}

                    override fun onSuccess(beaconUpdate: BeaconScanHelper.BeaconUpdate) {
                        if (beaconUpdate.beacon.isPresent) {
                            val beacon = beaconUpdate.beacon.get()

                            Log.d(TAG, "Beacon found.. $beacon'. Retrieving panel info ...")

                            // NJD TODO - eek, embedded callback.. must fix!!!!! (notice multiple !'s)
                            GoogleProximity.getInstance().retrieveAttachment(beacon)
                                    // I have to do this becuase i'm doing this horrible callback-hell
                                    // thing and this onSuccess() really needs to be in the background.
                                    // ultimately it should be part of a flatMap
                                    .observeOn(Schedulers.io())
                                    .subscribe(object : MaybeObserver<Array<String>> {
                                override fun onSubscribe(d: Disposable) {}

                                override fun onSuccess(attachment: Array<String>) {

                                    var panelDescription = attachment[0]

                                    var retrievedAttachment: String = attachment[1]

                                    var panelId = retrievedAttachment?: NEW_PANEL_ID

                                    // Presumably goes out to cloud to get Customer associated with
                                    // this Panel.
                                    customerProvider.findCustomerForPanel(panelId = panelId)
                                            // I have to do this becuase i'm doing this horrible callback-hell
                                            // thing and this onSuccess() really needs to be in the background.
                                            // ultimately it should be part of a flatMap
                                            .observeOn(Schedulers.io())
                                            .subscribe(object: SingleObserver<Customer> {
                                        override fun onSuccess(panelCustomer: Customer) {

                                            var customerPanel = Panel(panelId, panelDescription, panelCustomer.id)

                                            // NJD TODO - normally you cannot do such operations on UI thread
                                            // but until i fix this callback hell, this needs to be here.
                                            panelDao.insertOrReplacePanel(customerPanel)

                                            // scan found configured panel
                                            scanForNearbyPanelSubject!!.onSuccess(customerPanel)
                                        }

                                        override fun onSubscribe(d: Disposable?) {
                                        }

                                        override fun onError(e: Throwable?) {
                                            Log.e(TAG, "Error retrieving customer.", e)
                                        }
                                    })
                                }

                                override fun onError(e: Throwable) {

                                    Log.e(TAG, "Error retrieving attachment.", e)
                                    // couldn't retrieve panel information..
                                    scanForNearbyPanelSubject!!.onError(e)
                                }

                                override fun onComplete() {

                                    // scan found new panel
                                    scanForNearbyPanelSubject!!.onSuccess(Panel(id = "1111", customerId = null))
                                }
                            })
                        } else {
                            Log.e(TAG, "Couldn't find beacon.")
                            scanForNearbyPanelSubject!!.onComplete()
                        }
                    }

                    override fun onError(e: Throwable) {
                        Log.e(TAG, "No beacon found.", e)

                        scanForNearbyPanelSubject!!.onError(e)
                    }

                    override fun onComplete() {
                        Log.d(TAG, "Couldn't find beacon.")

                        // scan found no panel
                        scanForNearbyPanelSubject!!.onComplete()
                    }
                })

        return scanForNearbyPanelSubject!!
                .doFinally { idlingResource.updateIdleState(CustomIdlingResource.IS_IDLE) }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    open fun updateNearbyPanel(configPanel: Panel): Completable {

        idlingResource.updateIdleState(CustomIdlingResource.IS_NOT_IDLE)

        // A beacon with this namespace is, by definition, a panel
        val beaconNamespaceId = context.resources.getString(R.string.beaconNamespaceId)

        return GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally { GoogleProximity.getInstance().stopBeaconScanning() }

                // We're getting the first 'beaconUpdate' element emitted,
                // which would include a beacon.
                .flatMapCompletable { beaconUpdate -> GoogleProximity.getInstance().updateBeacon(beaconUpdate.beacon.get(), arrayOf(configPanel.description, configPanel.id)) }
                .doFinally { idlingResource.updateIdleState(CustomIdlingResource.IS_IDLE) }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    open fun eraseNearbyPanel(): Completable {

        idlingResource.updateIdleState(CustomIdlingResource.IS_NOT_IDLE)

        // A beacon with this namespace is, by definition, a panel
        val beaconNamespaceId = context.resources.getString(R.string.beaconNamespaceId)

        return GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally { GoogleProximity.getInstance().stopBeaconScanning() }

                // We're getting the first 'beaconUpdate' element emitted,
                // which would include a beacon.
                .flatMapCompletable { beaconUpdate -> GoogleProximity.getInstance().updateBeacon(beaconUpdate.beacon.get(), arrayOf(NEW_PANEL_DESCRIPTION, NEW_PANEL_ID)) }
                .doFinally { idlingResource.updateIdleState(CustomIdlingResource.IS_IDLE) }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }


    open fun getStoredPanel(): Maybe<Panel> {
        return Maybe.create(object: MaybeOnSubscribe<Panel> {
            override fun subscribe(subscriber: MaybeEmitter<Panel>) {
                panelDao.getStoredPanel()?.let { subscriber.onSuccess(it) } ?: let { subscriber.onComplete() }
            }
        })
        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    open fun deleteAllPanels(): Completable {
        return Completable.create { subscriber ->
            panelDao.deleteAllPanels()
            subscriber.onComplete()
        }.also {
            it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }
    }

    companion object {

        private val TAG = PanelProvider::class.java!!.getSimpleName()

        // How long we wait to find a panel before declaring none present.
        private val PANEL_SCAN_TIMEOUT_SECONDS = 10
    }
}