package com.ndipatri.solarmonitor.providers.panelScan


import android.content.Context
import android.util.Log
import androidx.test.espresso.idling.CountingIdlingResource
import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.iot.googleproximity.utils.BeaconScanHelper
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.persistence.AppDatabase
import com.ndipatri.solarmonitor.providers.customer.Customer
import com.ndipatri.solarmonitor.providers.customer.CustomerProvider
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.MaybeSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirst
import kotlinx.coroutines.withContext
import javax.inject.Inject

open class PanelProvider(var context: Context) {

    init {
        SolarMonitorApp.instance.objectGraph.inject(this)
    }

    open val idlingResource = CountingIdlingResource("panelProviderResource")

    val panelDao = AppDatabase.getInstance(context).scannedPanelDao()

    @Inject
    lateinit var customerProvider: CustomerProvider

    open suspend fun scanForNearbyPanel(): Panel? {

        var scannedPanel: Panel? = null

        // We call an external BLE scanning library which we know eventually returns
        // an async response on either success or failure... For this reason, we cannot
        // depend on RxPlugins for test thread synchronization.  So we use IdlingResource
        // We can safely do this (e.g. we won't hang the test thread) because we know this
        // library has a timeout eventually.
        idlingResource.increment()

        try {

            // A beacon with this namespace is, by definition, a panel
            val beaconNamespaceId = context.resources.getString(R.string.beaconNamespaceId)

            // Here we're taking advantage of coroutine/rx2 bridge to convert an Observable stream
            // to Deferred<BeaconUpdate>.. This particular operator 'awaitFirst()' will resume
            // once the first element has been emitted from stream.
            var beaconUpdate = GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS).awaitFirst()

            GoogleProximity.getInstance().stopBeaconScanning()

            if (beaconUpdate.beacon.isPresent) {
                val beacon = beaconUpdate.beacon.get()

                Log.d(TAG, "Beacon found.. $beacon'. Retrieving panel info ...")

                var beaconAttachments = GoogleProximity.getInstance().retrieveAttachment(beacon).await()

                beaconAttachments?.apply {

                    var panelDescription = this[0]

                    var retrievedAttachment: String = this[1]

                    var panelId = retrievedAttachment ?: NEW_PANEL_ID

                    // Presumably goes out to cloud to get Customer associated with
                    // this Panel.
                    var panelCustomer = customerProvider.findCustomerForPanel(panelId = panelId)

                    scannedPanel = Panel(panelId, panelDescription, panelCustomer.id)

                    panelDao.insertOrReplacePanel(scannedPanel!!)

                } ?: let {
                    // scan found new panel
                    scannedPanel = Panel(id = "1111", customerId = null)
                }
            }
        } finally {
            idlingResource.decrement()
        }

        return scannedPanel
    }

    open fun updateNearbyPanel(configPanel: Panel): Completable {

        idlingResource.increment()

        // A beacon with this namespace is, by definition, a panel
        val beaconNamespaceId = context.resources.getString(R.string.beaconNamespaceId)

        return GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally { GoogleProximity.getInstance().stopBeaconScanning() }

                // We're getting the first 'beaconUpdate' element emitted,
                // which would include a beacon.
                .flatMapCompletable { beaconUpdate -> GoogleProximity.getInstance().updateBeacon(beaconUpdate.beacon.get(), arrayOf(configPanel.description, configPanel.id)) }
                .doFinally { idlingResource.decrement() }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    open fun eraseNearbyPanel(): Completable {

        idlingResource.increment()

        // A beacon with this namespace is, by definition, a panel
        val beaconNamespaceId = context.resources.getString(R.string.beaconNamespaceId)

        return GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally { GoogleProximity.getInstance().stopBeaconScanning() }

                // We're getting the first 'beaconUpdate' element emitted,
                // which would include a beacon.
                .flatMapCompletable { beaconUpdate -> GoogleProximity.getInstance().updateBeacon(beaconUpdate.beacon.get(), arrayOf(NEW_PANEL_DESCRIPTION, NEW_PANEL_ID)) }
                .doFinally { idlingResource.decrement() }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }


    open fun getStoredPanel(): Maybe<Panel> {
        return Maybe.create(object: MaybeOnSubscribe<Panel> {
            override fun subscribe(subscriber: MaybeEmitter<Panel>) {
                panelDao.getStoredPanel()?.let { subscriber.onSuccess(it) } ?: subscriber.onComplete()
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
