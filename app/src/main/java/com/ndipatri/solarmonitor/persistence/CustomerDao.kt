/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ndipatri.solarmonitor.persistence

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.IGNORE
import android.arch.persistence.room.Query
import com.ndipatri.solarmonitor.providers.customer.Customer

@Dao
interface CustomerDao {

    @Query("select * from Customer")
    fun getAllCustomers(): List<Customer>

    @Insert(onConflict = IGNORE)
    fun insertOrReplaceCustomer(customer: Customer)

    @Query("select * from Customer where id = :id")
    fun getCustomerByName(id: String): Customer

    @Query("SELECT * FROM Customer " +
            "INNER JOIN Panel ON Panel.customerId = Customer.id " +
            "WHERE Panel.id = :panelId LIMIT 1")
    fun getCustomerByPanelId(panelId: String): Customer?

    @Query("delete from Customer")
    fun deleteAllCustomers(): Int
}