package com.ndipatri.solarmonitor.providers.panelScan;


import android.content.Context;
import android.util.Log;

import com.ndipatri.iot.googleproximity.GoogleProximity;
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

    protected Context context;

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

        scanForNearbyPanelSubject = MaybeSubject.create();

        // A beacon with this namespace is, by definition, a panel
        String beaconNamespaceId = context.getResources().getString(R.string.beaconNamespaceId);

        GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId)
                .firstElement() // once is good enough
                .doFinally(() -> GoogleProximity.getInstance().stopBeaconScanning())
                .subscribe(new MaybeObserver<Beacon>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(Beacon beacon) {
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
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Error scanning for beacons.", e);

                        scanForNearbyPanelSubject.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "Couldn't find beacon.");

                        // scan found no panel
                        scanForNearbyPanelSubject.onComplete();
                    }
                });

        return scanForNearbyPanelSubject
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public Completable updateNearbyPanel(PanelInfo configPanelInfo) {

        // A beacon with this namespace is, by definition, a panel
        String beaconNamespaceId = context.getResources().getString(R.string.beaconNamespaceId);

        return GoogleProximity.getInstance().scanForNearbyBeacon(beaconNamespaceId)
                .firstElement() // once is good enough
                .doFinally(() -> GoogleProximity.getInstance().stopBeaconScanning())
                .flatMapCompletable(beacon -> GoogleProximity.getInstance().updateBeacon(beacon, configPanelInfo.getAttachment()))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }
}
