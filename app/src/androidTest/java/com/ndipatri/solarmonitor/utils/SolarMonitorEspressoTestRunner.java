package com.ndipatri.solarmonitor.utils;

import android.support.test.runner.AndroidJUnitRunner;

import com.squareup.rx2.idler.Rx2Idler;

import io.reactivex.plugins.RxJavaPlugins;

public final class SolarMonitorEspressoTestRunner extends AndroidJUnitRunner {
    @Override public void onStart() {

        RxJavaPlugins.setInitIoSchedulerHandler(Rx2Idler.create("RxJava 2.x IO Scheduler"));
        RxJavaPlugins.setInitComputationSchedulerHandler( Rx2Idler.create("RxJava 2.x Computation Scheduler"));
        RxJavaPlugins.setInitNewThreadSchedulerHandler(Rx2Idler.create("RxJava 2.x Computation Scheduler"));

        super.onStart();
    }
}
