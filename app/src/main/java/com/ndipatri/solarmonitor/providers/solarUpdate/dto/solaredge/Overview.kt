package com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Overview {

    @SerializedName("lastUpdateTime")
    @Expose
    var lastUpdateTime: String? = null
    @SerializedName("lifeTimeData")
    @Expose
    var lifeTimeData: LifeTimeData? = null
    @SerializedName("lastYearData")
    @Expose
    var lastYearData: LastYearData? = null
    @SerializedName("lastMonthData")
    @Expose
    var lastMonthData: LastMonthData? = null
    @SerializedName("lastDayData")
    @Expose
    var lastDayData: LastDayData? = null
    @SerializedName("currentPower")
    @Expose
    var currentPower: CurrentPower? = null
    @SerializedName("measuredBy")
    @Expose
    var measuredBy: String? = null

}
