package com.ndipatri.solarmonitor.providers.solarUpdate


import android.util.Log
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.GetOverviewResponse
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withTimeout
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class SolarOutputProvider(val apiKey: String) {

    private val solarOutputRESTEndpoint: Single<SolarOutputRESTInterface> by lazy {

        Single.create { subscribe: SingleEmitter<SolarOutputRESTInterface> ->

            val retrofitBuilder = Retrofit.Builder()
                    .baseUrl(API_ENDPOINT_BASE_URL)
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                    .addConverterFactory(GsonConverterFactory.create())
            try {
                subscribe.onSuccess(retrofitBuilder.build().create(SolarOutputRESTInterface::class.java!!))
            } catch (ex: Exception) {
                Log.e("SolarOutputProvider", "Exception while getting endpoint.", ex)
            }
        }

        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        .cache()
    }

    suspend fun getSolarOutput(customerId: String): PowerOutput {

        var currentPower: Double? = null
        var lifeTimeEnergy: Double? = null

        // Much like 'withContext()' this launches a new coroutine with a time constraint.
        withTimeout(SOLAR_OUTPUT_TIMEOUT_SECONDS*1000L) {

            // Here we are converting a Single to a Deferred and then waiting on that.
            // Note that while we are waiting, this stack frame is saved and while
            // the background rxJava call is running on background, the current
            // thread is released.
            var endpoint = solarOutputRESTEndpoint.await()

            // Retrofit is already returning a Deferred so we just have to wait. Otherwise,
            // same as above.
            var getOverviewResponse = endpoint.getOverview(customerId, apiKey).await()

            // Since we waited, we are now back on original calling thread and we have
            // our data from above asynchronous calls.
            currentPower = getOverviewResponse.overview!!.currentPower!!.power
            lifeTimeEnergy = getOverviewResponse.overview!!.lifeTimeData!!.energy
        }

        return PowerOutput(currentPower, lifeTimeEnergy)
    }

    internal interface SolarOutputRESTInterface {

        @GET("site/{siteId}/overview.json")
        fun getOverview(@Path("siteId") siteId: String, @Query("api_key") apiKey: String): Deferred<GetOverviewResponse>
    }

    companion object {

        val SOLAR_OUTPUT_TIMEOUT_SECONDS = 10

        var API_ENDPOINT_BASE_URL = "https://monitoringapi.solaredge.com/"
    }
}
