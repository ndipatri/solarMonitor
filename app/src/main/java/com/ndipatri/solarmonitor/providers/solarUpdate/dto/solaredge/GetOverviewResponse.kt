package com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class GetOverviewResponse {

    @SerializedName("overview")
    @Expose
    var overview: Overview? = null

}
