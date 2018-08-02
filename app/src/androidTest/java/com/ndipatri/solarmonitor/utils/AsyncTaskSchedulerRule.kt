package com.ndipatri.solarmonitor.utils

import android.os.AsyncTask

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

import io.reactivex.Scheduler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers

class AsyncTaskSchedulerRule : TestRule {

    private val asyncTaskScheduler = Schedulers.from(AsyncTask.THREAD_POOL_EXECUTOR)

    override fun apply(base: Statement, d: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                RxJavaPlugins.setIoSchedulerHandler { scheduler -> asyncTaskScheduler }
                RxJavaPlugins.setComputationSchedulerHandler { scheduler -> asyncTaskScheduler }
                RxJavaPlugins.setNewThreadSchedulerHandler { scheduler -> asyncTaskScheduler }

                try {
                    base.evaluate()
                } finally {
                    RxJavaPlugins.reset()
                }
            }
        }
    }
}
