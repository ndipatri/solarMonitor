package com.ndipatri.solarmonitor;


import android.app.Application;

import com.ndipatri.solarmonitor.container.ObjectGraph;

public class SolarMonitorApp extends Application {

    private ObjectGraph objectGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        objectGraph = ObjectGraph.Initializer.init(this);
    }

    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    public void setObjectGraph(ObjectGraph objectGraph) {
        this.objectGraph = objectGraph;
    }
}
