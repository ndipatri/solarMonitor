package com.ndipatri.solarmonitor.providers.panelScan;


import android.content.Context;
import android.support.annotation.Nullable;
import android.support.test.espresso.IdlingResource;
import android.util.Log;

import com.ndipatri.iot.googleproximity.GoogleProximity;
import com.ndipatri.iot.googleproximity.utils.BeaconScanHelper;
import com.ndipatri.solarmonitor.R;

import org.altbeacon.beacon.Beacon;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.MaybeSubject;

public class PanelScanProvider {

    private static final String TAG = PanelScanProvider.class.getSimpleName();

    // How long we wait to find a panel before declaring none present.
    private static final int PANEL_SCAN_TIMEOUT_SECONDS = 10;

    protected Context context;

    private PanelScanProviderIdlingResource idlingResource = new PanelScanProviderIdlingResource();

    public PanelScanProvider(Context context) {
        this.context = context;
    }

    /**
     * Possible outcomes:
     * <p>
     * 1. found no panel (onComplete() without onSuccess())
     * 2. found new panel (onSuccess() with new PanelInfo having default values)
     * 3. found configured panel (onSuccess() with existing PanelInfo)
     * 4. threw error (onError())
     * <p>
     * This implementation is a bit complex.. I tried to use a simple chain of operators, but I
     * couldn't distinguish #2 and #3
     * should maybe try again sometime when i'm smarter...
     **/
    private MaybeSubject<PanelInfo> scanForNearbyPanelSubject;

    public Maybe<PanelInfo> scanForNearbyPanel() {

        idlingResource.updateIdleState(PanelScanProviderIdlingResource.IS_NOT_IDLE);

        scanForNearbyPanelSubject = MaybeSubject.create();

        // A beacon with this namespace is, by definition, a panel
        String beaconNamespaceId = context.getResources().getString(R.string.beaconNamespaceId);

        GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally(() -> GoogleProximity.getInstance().stopBeaconScanning())
                .subscribe(new MaybeObserver<BeaconScanHelper.BeaconUpdate>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(BeaconScanHelper.BeaconUpdate beaconUpdate) {
                        if (beaconUpdate.getBeacon().isPresent()) {
                            Beacon beacon = beaconUpdate.getBeacon().get();

                            Log.d(TAG, "Beacon found.. " + beacon + "'. Retrieving panel info ...");

                            GoogleProximity.getInstance().retrieveAttachment(beacon).subscribe(new MaybeObserver<String[]>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                }

                                @Override
                                public void onSuccess(String[] attachment) {

                                    // scan found configured panel
                                    scanForNearbyPanelSubject.onSuccess(new PanelInfo(attachment));
                                }

                                @Override
                                public void onError(Throwable e) {

                                    // couldn't retrieve panel information..
                                    scanForNearbyPanelSubject.onError(e);
                                }

                                @Override
                                public void onComplete() {

                                    // scan found new panel
                                    scanForNearbyPanelSubject.onSuccess(new PanelInfo());
                                }
                            });
                        } else {
                            Log.e(TAG, "Couldn't find beacon.");
                            scanForNearbyPanelSubject.onError(new Exception("Region left."));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "No beacon found.", e);

                        scanForNearbyPanelSubject.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "Couldn't find beacon.");

                        // scan found no panel
                        scanForNearbyPanelSubject.onComplete();
                    }
                });

        return scanForNearbyPanelSubject
                .doFinally(() -> idlingResource.updateIdleState(PanelScanProviderIdlingResource.IS_IDLE))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public Completable updateNearbyPanel(PanelInfo configPanelInfo) {

        idlingResource.updateIdleState(PanelScanProviderIdlingResource.IS_NOT_IDLE);

        // A beacon with this namespace is, by definition, a panel
        String beaconNamespaceId = context.getResources().getString(R.string.beaconNamespaceId);

        return GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId, PANEL_SCAN_TIMEOUT_SECONDS)
                .firstElement() // once is good enough
                .doFinally(() -> GoogleProximity.getInstance().stopBeaconScanning())

                // We're getting the first 'beaconUpdate' element emitted,
                // which would include a beacon.
                .flatMapCompletable(beaconUpdate -> GoogleProximity.getInstance().updateBeacon(beaconUpdate.getBeacon().get(), configPanelInfo.getAttachment()))
                .doFinally(() -> idlingResource.updateIdleState(PanelScanProviderIdlingResource.IS_IDLE))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public IdlingResource getIdlingResource() {
        return idlingResource;
    }

    public static class PanelScanProviderIdlingResource implements IdlingResource {

        public static final boolean IS_IDLE = true;
        public static final boolean IS_NOT_IDLE = false;

        @Nullable
        private volatile ResourceCallback resourceCallback;

        private boolean isIdle = IS_IDLE;

        @Override
        public String getName() {
            return this.getClass().getName();
        }

        @Override
        public boolean isIdleNow() {
            return isIdle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
            this.resourceCallback = resourceCallback;
        }

        public synchronized void updateIdleState(boolean isIdle) {
            this.isIdle = isIdle;

            Log.d(TAG, "PanelScanProviderIdlingResource: Update IdleState(" + isIdle + ")");

            if (isIdle && null != resourceCallback) {
                resourceCallback.onTransitionToIdle();
            }
        }
    }
}
