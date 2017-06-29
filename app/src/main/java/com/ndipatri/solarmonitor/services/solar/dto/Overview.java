
package com.ndipatri.solarmonitor.services.solar.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Overview {

    @SerializedName("lastUpdateTime")
    @Expose
    private String lastUpdateTime;
    @SerializedName("lifeTimeData")
    @Expose
    private LifeTimeData lifeTimeData;
    @SerializedName("lastYearData")
    @Expose
    private LastYearData lastYearData;
    @SerializedName("lastMonthData")
    @Expose
    private LastMonthData lastMonthData;
    @SerializedName("lastDayData")
    @Expose
    private LastDayData lastDayData;
    @SerializedName("currentPower")
    @Expose
    private CurrentPower currentPower;
    @SerializedName("measuredBy")
    @Expose
    private String measuredBy;

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public LifeTimeData getLifeTimeData() {
        return lifeTimeData;
    }

    public void setLifeTimeData(LifeTimeData lifeTimeData) {
        this.lifeTimeData = lifeTimeData;
    }

    public LastYearData getLastYearData() {
        return lastYearData;
    }

    public void setLastYearData(LastYearData lastYearData) {
        this.lastYearData = lastYearData;
    }

    public LastMonthData getLastMonthData() {
        return lastMonthData;
    }

    public void setLastMonthData(LastMonthData lastMonthData) {
        this.lastMonthData = lastMonthData;
    }

    public LastDayData getLastDayData() {
        return lastDayData;
    }

    public void setLastDayData(LastDayData lastDayData) {
        this.lastDayData = lastDayData;
    }

    public CurrentPower getCurrentPower() {
        return currentPower;
    }

    public void setCurrentPower(CurrentPower currentPower) {
        this.currentPower = currentPower;
    }

    public String getMeasuredBy() {
        return measuredBy;
    }

    public void setMeasuredBy(String measuredBy) {
        this.measuredBy = measuredBy;
    }

}
