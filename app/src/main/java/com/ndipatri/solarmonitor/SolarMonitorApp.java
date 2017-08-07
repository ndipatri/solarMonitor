package com.ndipatri.solarmonitor;


import android.app.Application;
import android.preference.PreferenceManager;
import android.support.test.espresso.IdlingResource;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.ndipatri.iot.googleproximity.GoogleProximity;
import com.ndipatri.solarmonitor.container.ObjectGraph;
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo;

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

        GoogleProximity.initialize(this, false);
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
