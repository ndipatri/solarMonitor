package com.ndipatri.solarmonitor.services;

import io.reactivex.Single;

public interface SolarOutputService {
    Single<String> getSolarOutput(String customerId);
}
