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

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.ndipatri.solarmonitor.providers.customer.Customer
import com.ndipatri.solarmonitor.providers.panelScan.Panel

@Database(entities = [(Panel::class), (Customer::class)], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scannedPanelDao(): PanelDao

    abstract fun customerDao(): CustomerDao

    companion object {

        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                                        AppDatabase::class.java,
                                                        "solar_monitor.db")
                                        .fallbackToDestructiveMigration()

                                        // NDJ TODO - remove following once i get my operators in
                                        // in order in PanelProvider
                                        .allowMainThreadQueries()

                                        .build()
                    }
                }
            }

            return INSTANCE!!
        }
    }
}