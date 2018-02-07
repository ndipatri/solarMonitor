package com.ndipatri.solarmonitor.providers.panelScan

data class PanelInfo (val description: String = "new panel",
                      val customerId: String? = null) {

    constructor(attachment: Array<String>) : this(description = attachment[0], customerId = attachment[1])
}
