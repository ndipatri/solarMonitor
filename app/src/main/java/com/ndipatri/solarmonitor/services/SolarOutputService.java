package com.ndipatri.solarmonitor.services;


import android.util.Log;

import com.ndipatri.solarmonitor.dto.GetOverviewResponse;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class SolarOutputService {

    public static final int SOLAR_OUTPUT_TIMEOUT_SECONDS = 10;

    public static String API_ENDPOINT_BASE_URL = "https://monitoringapi.solaredge.com/";

    private String apiKey;

    public SolarOutputService(String apiKey) {
        this.apiKey = apiKey;
    }

    private Single<SolarOutputRESTInterface> getSolarOutputRESTEndpoint() {

        return Single.create((SingleEmitter<SolarOutputRESTInterface> subscribe) -> {

            Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                    .baseUrl(API_ENDPOINT_BASE_URL)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                    .addConverterFactory(GsonConverterFactory.create());

            try {
                subscribe.onSuccess(retrofitBuilder.build().create(SolarOutputRESTInterface.class));
            } catch (Exception ex) {
                Log.e("SolarOutputService", "Exception while getting endpoint.", ex);
            }
        })

        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        .cache();
    }

    public Single<Double> getSolarOutputInWatts(String customerId) {
        return getSolarOutputRESTEndpoint()
                .flatMap(endpoint -> endpoint.getOverview(customerId, apiKey))
                .flatMap(getOverviewResponse -> Single.create((SingleOnSubscribe<Double>) subscriber -> {

                    Double currentPower = getOverviewResponse.getOverview().getCurrentPower().getPower();
                    subscriber.onSuccess(currentPower);
                }))

                // NJD TODO - comfort delay - remove
                .delay(5000, TimeUnit.MILLISECONDS)

                .timeout(SOLAR_OUTPUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    interface SolarOutputRESTInterface {

        @GET("site/{siteId}/overview.json")
        Single<GetOverviewResponse> getOverview(@Path("siteId") String siteId, @Query("api_key") String apiKey);
    }

    public String getApiKey() {
        return apiKey;
    }
}
