
package com.ndipatri.solarmonitor.services.solar.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LastMonthData {

    @SerializedName("energy")
    @Expose
    private Double energy;

    public Double getEnergy() {
        return energy;
    }

    public void setEnergy(Double energy) {
        this.energy = energy;
    }

}
