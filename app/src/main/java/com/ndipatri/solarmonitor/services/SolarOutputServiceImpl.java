package com.ndipatri.solarmonitor.services;


import com.ndipatri.solarmonitor.dto.GetSolarOutputResponse;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public class SolarOutputServiceImpl implements SolarOutputService {

    private SolarOutputRESTInterface interfaceImpl;

    private static final String API_AUTH_HEADER = "abcdefg1234";

    public static String API_ENDPOINT_BASE_URL = "http://mysolarcompany.com";

    public SolarOutputServiceImpl() {
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl(API_ENDPOINT_BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create());

        interfaceImpl = retrofitBuilder.build().create(SolarOutputRESTInterface.class);
    }

    public Single<String> getSolarOutput(String customerId) {
        return interfaceImpl.getSolarOutput(customerId, API_AUTH_HEADER)
                .flatMap(new Function<GetSolarOutputResponse, SingleSource<String>>() {
                    @Override
                    public SingleSource<String> apply(GetSolarOutputResponse getSolarOutputResponse) throws Exception {
                        return Single.just(getSolarOutputResponse.getOutput() + " " + getSolarOutputResponse.getUnits());
                    }
                })
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("unused")
    public interface SolarOutputRESTInterface {
        @GET("getSolarOutput")
        Single<GetSolarOutputResponse> getSolarOutput(@Query("customerId") String customerId,
                                                      @Header("Authorization") String authorization);
    }
}
