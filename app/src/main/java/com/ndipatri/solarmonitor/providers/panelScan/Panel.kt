package com.ndipatri.solarmonitor.providers.panelScan

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import com.ndipatri.solarmonitor.providers.customer.Customer

val NEW_PANEL_ID = "1111"
val NEW_PANEL_DESCRIPTION = "a new panel"

@Entity(foreignKeys =
    [(ForeignKey(entity = Customer::class,
                 parentColumns = arrayOf("id"),
                 childColumns = arrayOf("customerId"),
                 onDelete = ForeignKey.SET_NULL))
    ]
)
data class Panel (@PrimaryKey var id: String = NEW_PANEL_ID,
                  var description: String = NEW_PANEL_DESCRIPTION,
                  var customerId: String? = null)
