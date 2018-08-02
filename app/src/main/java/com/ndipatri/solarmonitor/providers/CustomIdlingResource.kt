package com.ndipatri.solarmonitor.providers

import android.support.test.espresso.IdlingResource
import android.util.Log

open class CustomIdlingResource(var resourceName: String) : IdlingResource {

    override fun getName(): String {
        return resourceName
    }

    @Volatile
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    private var isIdle = IS_IDLE

    override fun isIdleNow(): Boolean {
        return isIdle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

    @Synchronized
    fun updateIdleState(isIdle: Boolean) {
        this.isIdle = isIdle

        Log.d(TAG, "CustomerProviderIdlingResource: Update IdleState($isIdle)")

        if (isIdle && null != resourceCallback) {
            resourceCallback!!.onTransitionToIdle()
        }
    }

    companion object {

        val IS_IDLE = true
        val IS_NOT_IDLE = false

        private val TAG = CustomIdlingResource::class.java.getSimpleName()
    }
}