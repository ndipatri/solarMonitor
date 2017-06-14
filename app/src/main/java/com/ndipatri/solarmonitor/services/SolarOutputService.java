package com.ndipatri.solarmonitor.services;


import com.ndipatri.solarmonitor.dto.GetOverviewResponse;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class SolarOutputService {

    private SolarOutputRESTInterface interfaceImpl;

    public static String API_ENDPOINT_BASE_URL = "https://monitoringapi.solaredge.com/";

    private String apiKey;

    public SolarOutputService(String apiKey) {
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl(API_ENDPOINT_BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create());

        this.apiKey = apiKey;
        interfaceImpl = retrofitBuilder.build().create(SolarOutputRESTInterface.class);
    }

    public Single<Double> getSolarOutputInWatts(String customerId) {
        return interfaceImpl.getOverview(customerId, apiKey)
                .flatMap(new Function<GetOverviewResponse, SingleSource<Double>>() {
                    @Override
                    public SingleSource<Double> apply(GetOverviewResponse getOverviewResponse) throws Exception {
                        Double currentPower = getOverviewResponse.getOverview().getCurrentPower().getPower();

                        return Single.just(currentPower);
                    }
                })
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
