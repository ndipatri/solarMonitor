package com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LastDayData {

    @SerializedName("energy")
    @Expose
    var energy: Double? = null

}
