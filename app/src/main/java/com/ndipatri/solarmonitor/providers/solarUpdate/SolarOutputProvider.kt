package com.ndipatri.solarmonitor.providers.solarUpdate


import android.util.Log

import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge.GetOverviewResponse

import java.util.concurrent.TimeUnit

import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class SolarOutputProvider(val apiKey: String) {

    private val solarOutputRESTEndpoint: Single<SolarOutputRESTInterface>
        get() = Single.create { subscribe: SingleEmitter<SolarOutputRESTInterface> ->

            val retrofitBuilder = Retrofit.Builder()
                    .baseUrl(API_ENDPOINT_BASE_URL)
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

    fun getSolarOutput(customerId: String): Single<PowerOutput> {
        return solarOutputRESTEndpoint
                .flatMap { endpoint -> endpoint.getOverview(customerId, apiKey) }
                .flatMap { getOverviewResponse ->
                    Single.create({ subscriber ->

                        val currentPower = getOverviewResponse.overview!!.currentPower!!.power
                        val lifeTimeEnergy = getOverviewResponse.overview!!.lifeTimeData!!.energy

                        subscriber.onSuccess(PowerOutput(currentPower, lifeTimeEnergy))
                    } as SingleOnSubscribe<PowerOutput>)
                }

                .timeout(SOLAR_OUTPUT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    internal interface SolarOutputRESTInterface {

        @GET("site/{siteId}/overview.json")
        fun getOverview(@Path("siteId") siteId: String, @Query("api_key") apiKey: String): Single<GetOverviewResponse>
    }

    companion object {

        val SOLAR_OUTPUT_TIMEOUT_SECONDS = 10

        var API_ENDPOINT_BASE_URL = "https://monitoringapi.solaredge.com/"
    }
}
