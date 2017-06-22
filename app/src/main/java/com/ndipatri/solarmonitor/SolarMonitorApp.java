package com.ndipatri.solarmonitor;


import android.app.Application;

import com.ndipatri.solarmonitor.container.ObjectGraph;

public class SolarMonitorApp extends Application {

    private ObjectGraph objectGraph;

    // NJD TODO - need to allow user to config this
    private String solarCustomerId = "480557";

    private static SolarMonitorApp instance;

    public SolarMonitorApp() {
        SolarMonitorApp.instance = this;
    }

    public static SolarMonitorApp getInstance() {
        return instance;
    }

    public ObjectGraph getObjectGraph() {
        if (objectGraph == null) {
            objectGraph = ObjectGraph.Initializer.init(this);
        }

        return objectGraph;
    }

    public void setObjectGraph(ObjectGraph objectGraph) {
        this.objectGraph = objectGraph;
    }

    public String getSolarCustomerId() {
        return solarCustomerId;
    }

    // NJD TODO - user should be able to change this.
    public void setSolarCustomerId(String solarCustomerId) {
        this.solarCustomerId = solarCustomerId;
    }
}
