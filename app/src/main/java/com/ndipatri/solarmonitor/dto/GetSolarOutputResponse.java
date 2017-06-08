package com.ndipatri.solarmonitor.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GetSolarOutputResponse {

    @SerializedName("output")
    @Expose
    private String output;
    @SerializedName("units")
    @Expose
    private String units;

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

}