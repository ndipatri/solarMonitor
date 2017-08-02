package com.ndipatri.solarmonitor.providers.solarUpdate.dto;


public class PowerOutput {

    private Double currentPowerInWatts;
    private Double lifeTimeEnergyInWattHours;

    public PowerOutput(Double currentPowerInWatts, Double lifeTimeEnergyInWattHours) {
        this.currentPowerInWatts = currentPowerInWatts;
        this.lifeTimeEnergyInWattHours = lifeTimeEnergyInWattHours;
    }

    public Double getCurrentPowerInWatts() {
        return currentPowerInWatts;
    }

    public void setCurrentPowerInWatts(Double currentPowerInWatts) {
        this.currentPowerInWatts = currentPowerInWatts;
    }

    public Double getLifeTimeEnergyInWattHours() {
        return lifeTimeEnergyInWattHours;
    }

    public void setLifeTimeEnergyInWattHours(Double lifeTimeEnergyInWattHours) {
        this.lifeTimeEnergyInWattHours = lifeTimeEnergyInWattHours;
    }
}

