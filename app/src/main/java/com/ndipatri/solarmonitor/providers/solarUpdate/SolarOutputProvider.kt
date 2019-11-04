package com.ndipatri.solarmonitor.providers.solarUpdate


import androidx.test.espresso.IdlingRegistry
import com.jakewharton.espresso.OkHttp3IdlingResource
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.GetOverviewResponse
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class SolarOutputProvider(val apiKey: String) {


    var deferredSolarOutputRESTInterface: Deferred<SolarOutputRESTInterface>? = null
    private fun getSolarOutputRESTEndpoint(scope: CoroutineScope): Deferred<SolarOutputRESTInterface> {

        if (deferredSolarOutputRESTInterface == null) {

            // This will launch when first 'await()' is called.  All subsequent 'await()' calls
            // will return same deferred value with no background call.
            deferredSolarOutputRESTInterface = scope.async(context = Dispatchers.IO,
                                                           start = CoroutineStart.LAZY) {

                var okHttpClient = OkHttpClient()
                IdlingRegistry.getInstance().register(OkHttp3IdlingResource.create("okhttp", okHttpClient));

                Retrofit.Builder().apply {
                    baseUrl(API_ENDPOINT_BASE_URL)
                            .addCallAdapterFactory(CoroutineCallAdapterFactory())
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                            .addConverterFactory(GsonConverterFactory.create())
                    client(okHttpClient)
                }.build().create(SolarOutputRESTInterface::class.java)
            }
        }

        return deferredSolarOutputRESTInterface as Deferred<SolarOutputRESTInterface>
    }

    suspend fun getSolarOutput(customerId: String): PowerOutput {

        var currentPower: Double? = null
        var lifeTimeEnergy: Double? = null

        // Much like 'withContext()' this launches a new coroutine with a time constraint.
        withTimeout(SOLAR_OUTPUT_TIMEOUT_SECONDS*1000L) {

            // Here we are getting a Deferred and then waiting on that.
            // Note that while we are waiting, this stack frame is saved and while
            // the background rxJava call is running on background, the current
            // thread is released.
            var endpoint = getSolarOutputRESTEndpoint(this).await()

            // Since Retrofit is no longer returning a Single, this call is straightforward...
            var getOverviewResponse = endpoint.getOverview(customerId, apiKey)

            // Since we waited, we are now back on original calling thread and we have
            // our data from above asynchronous calls.
            currentPower = getOverviewResponse.overview!!.currentPower!!.power
            lifeTimeEnergy = getOverviewResponse.overview!!.lifeTimeData!!.energy
        }

        return PowerOutput(currentPower, lifeTimeEnergy)
    }

    interface SolarOutputRESTInterface {

        @GET("site/{siteId}/overview.json")
        suspend fun getOverview(@Path("siteId") siteId: String, @Query("api_key") apiKey: String): GetOverviewResponse
    }

    companion object {

        val SOLAR_OUTPUT_TIMEOUT_SECONDS = 5

        var API_ENDPOINT_BASE_URL = "https://monitoringapi.solaredge.com/"
    }
}
