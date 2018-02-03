package com.ndipatri.solarmonitor.providers.panelScan


import android.content.Context
import android.support.test.espresso.IdlingResource
import android.util.Log

import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.iot.googleproximity.utils.BeaconScanHelper
import com.ndipatri.solarmonitor.R

import org.altbeacon.beacon.Beacon

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.MaybeObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.MaybeSubject

class PanelScanProvider(protected var context: Context) {

    private val idlingResource = PanelScanProviderIdlingResource()

    /**
     * Possible outcomes:
     *
     *
     * 1. found no panel (onComplete() without onSuccess())
     * 2. found new panel (onSuccess() with new PanelInfo having default values)
     * 3. found configured panel (onSuccess() with existing PanelInfo)
     * 4. threw error (onError())
     *
     *
     * This implementation is a bit complex.. I tried to use a simple chain of operators, but I
     * couldn't distinguish #2 and #3
     * should maybe try again sometime when i'm smarter...
     */
    private var scanForNearbyPanelSubject: MaybeSubject<PanelInfo>? = null

    fun scanForNearbyPanel(): Maybe<PanelInfo> {

        idlingResource.updateIdleState(PanelScanProviderIdlingResource.IS_NOT_IDLE)

        scanForNearbyPanelSubject = MaybeSubject.create()

        // A beacon with this namespace is, by definition, a panel
        val beaconNamespaceId = context.resources.getString(R.string.beaconNamespaceId)

        GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally { GoogleProximity.getInstance().stopBeaconScanning() }
                .subscribe(object : MaybeObserver<BeaconScanHelper.BeaconUpdate> {

                    override fun onSubscribe(d: Disposable) {}

                    override fun onSuccess(beaconUpdate: BeaconScanHelper.BeaconUpdate) {
                        if (beaconUpdate.beacon.isPresent) {
                            val beacon = beaconUpdate.beacon.get()

                            Log.d(TAG, "Beacon found.. $beacon'. Retrieving panel info ...")

                            GoogleProximity.getInstance().retrieveAttachment(beacon).subscribe(object : MaybeObserver<Array<String>> {
                                override fun onSubscribe(d: Disposable) {}

                                override fun onSuccess(attachment: Array<String>) {

                                    // scan found configured panel
                                    scanForNearbyPanelSubject!!.onSuccess(PanelInfo(attachment))
                                }

                                override fun onError(e: Throwable) {

                                    // couldn't retrieve panel information..
                                    scanForNearbyPanelSubject!!.onError(e)
                                }

                                override fun onComplete() {

                                    // scan found new panel
                                    scanForNearbyPanelSubject!!.onSuccess(PanelInfo())
                                }
                            })
                        } else {
                            Log.e(TAG, "Couldn't find beacon.")
                            scanForNearbyPanelSubject!!.onError(Exception("Region left."))
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
                .doFinally { idlingResource.updateIdleState(PanelScanProviderIdlingResource.IS_IDLE) }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun updateNearbyPanel(configPanelInfo: PanelInfo): Completable {

        idlingResource.updateIdleState(PanelScanProviderIdlingResource.IS_NOT_IDLE)

        // A beacon with this namespace is, by definition, a panel
        val beaconNamespaceId = context.resources.getString(R.string.beaconNamespaceId)

        return GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally { GoogleProximity.getInstance().stopBeaconScanning() }

                // We're getting the first 'beaconUpdate' element emitted,
                // which would include a beacon.
                .flatMapCompletable { beaconUpdate -> GoogleProximity.getInstance().updateBeacon(beaconUpdate.beacon.get(), configPanelInfo.attachment) }
                .doFinally { idlingResource.updateIdleState(PanelScanProviderIdlingResource.IS_IDLE) }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun getIdlingResource(): IdlingResource {
        return idlingResource
    }

    class PanelScanProviderIdlingResource : IdlingResource {

        @Volatile
        private var resourceCallback: IdlingResource.ResourceCallback? = null

        private var isIdle = IS_IDLE

        override fun getName(): String {
            return this.javaClass.getName()
        }

        override fun isIdleNow(): Boolean {
            return isIdle
        }

        override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
            this.resourceCallback = resourceCallback
        }

        @Synchronized
        fun updateIdleState(isIdle: Boolean) {
            this.isIdle = isIdle

            Log.d(TAG, "PanelScanProviderIdlingResource: Update IdleState($isIdle)")

            if (isIdle && null != resourceCallback) {
                resourceCallback!!.onTransitionToIdle()
            }
        }

        companion object {

            val IS_IDLE = true
            val IS_NOT_IDLE = false
        }
    }

    companion object {

        private val TAG = PanelScanProvider::class.java!!.getSimpleName()

        // How long we wait to find a panel before declaring none present.
        private val PANEL_SCAN_TIMEOUT_SECONDS = 10
    }
}
