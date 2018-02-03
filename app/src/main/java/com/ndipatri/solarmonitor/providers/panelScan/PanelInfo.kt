package com.ndipatri.solarmonitor.providers.panelScan


import java.util.Optional

class PanelInfo {

    var description: String? = null
    var customerId: Optional<String>? = Optional.empty()
        private set

    val attachment: Array<String>
        get() = arrayOf<String>(description, if (customerId!!.isPresent) customerId!!.get() else null)

    constructor() {
        this.description = "new panel"
    }

    constructor(description: String, customerId: String) {
        this.description = description
        this.customerId = Optional.ofNullable(customerId)
    }

    constructor(attachment: Array<String>) {
        this.description = attachment[0]
        this.customerId = Optional.ofNullable(attachment[1])
    }

    fun setCustomerId(customerId: String) {
        this.customerId = Optional.ofNullable(customerId)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val panelInfo = o as PanelInfo?

        if (if (description != null) description != panelInfo!!.description else panelInfo!!.description != null)
            return false
        return if (customerId != null) customerId == panelInfo.customerId else panelInfo.customerId == null

    }

    override fun hashCode(): Int {
        var result = if (description != null) description!!.hashCode() else 0
        result = 31 * result + if (customerId != null) customerId!!.hashCode() else 0
        return result
    }

    override fun toString(): String {
        return "PanelInfo{" +
                "description='" + description + '\''.toString() +
                ", customerId='" + customerId + '\''.toString() +
                '}'.toString()
    }
}
