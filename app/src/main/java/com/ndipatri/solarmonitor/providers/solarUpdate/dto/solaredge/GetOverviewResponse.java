
package com.ndipatri.solarmonitor.providers.solarUpdate.dto.solaredge;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GetOverviewResponse {

    @SerializedName("overview")
    @Expose
    private Overview overview;

    public Overview getOverview() {
        return overview;
    }

    public void setOverview(Overview overview) {
        this.overview = overview;
    }

}
