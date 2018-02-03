package com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CurrentPower {

    @SerializedName("power")
    @Expose
    var power: Double? = null

}
