package com.ndipatri.solarmonitor.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context that automatically is cancelled when UI is destroyed
 */
class UiLifecycleCoroutineScope : CoroutineScope, LifecycleObserver {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onCreate() {
        job = Job()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun destroy() = job.cancel()
}