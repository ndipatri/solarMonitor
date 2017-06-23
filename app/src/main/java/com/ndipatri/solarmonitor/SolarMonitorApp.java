package com.ndipatri.solarmonitor;


import android.app.Application;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.ndipatri.solarmonitor.container.ObjectGraph;

public class SolarMonitorApp extends Application {

    private ObjectGraph objectGraph;

    private static SolarMonitorApp instance;

    private RxSharedPreferences sharedPreferences;

    public SolarMonitorApp() {
        SolarMonitorApp.instance = this;
    }

    public static SolarMonitorApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(this));
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

    public Preference<String> getSolarCustomerId() {
        return sharedPreferences.getString("SOLAR_CUSTOMER_ID");
    }

    public void setSolarCustomerId(final String solarCustomerId) {
        getSolarCustomerId().set(solarCustomerId);
    }
}
