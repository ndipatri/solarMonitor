package com.ndipatri.solarmonitor.providers.customer

import androidx.room.Entity
import androidx.room.PrimaryKey

// NJD TODO - Ideally, the Customer object would also contain authentication details for
// communicating with cloud.

@Entity
data class Customer (@PrimaryKey var id: String,
                     var dollarsPerkWh: Double)
