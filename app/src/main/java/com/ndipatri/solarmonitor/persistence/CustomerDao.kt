package com.ndipatri.solarmonitor.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import com.ndipatri.solarmonitor.providers.customer.Customer

// NJD TODO - For now, this Dao supports a single persisted Customer

@Dao
interface CustomerDao {

    @Insert(onConflict = IGNORE)
    suspend fun insertOrReplaceCustomer(customer: Customer)

    @Query("select * from Customer where id = :id")
    suspend fun getCustomerByName(id: String): Customer

    @Query("SELECT * FROM Customer " +
            "INNER JOIN Panel ON Panel.customerId = Customer.id " +
            "WHERE Panel.id = :panelId LIMIT 1")
    fun getCustomerByPanelId(panelId: String): Customer?

    @Query("delete from Customer")
    fun deleteAllCustomers(): Int
}