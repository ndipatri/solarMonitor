
package com.ndipatri.solarmonitor.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CurrentPower {

    @SerializedName("power")
    @Expose
    private Double power;

    public Double getPower() {
        return power;
    }

    public void setPower(Double power) {
        this.power = power;
    }

}
