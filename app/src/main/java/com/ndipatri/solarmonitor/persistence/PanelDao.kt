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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ndipatri.solarmonitor.providers.panelScan.Panel

// NJD TODO - For now, this Dao supports a single persisted Panel

@Dao
interface PanelDao {

    @Query("select * from Panel LIMIT 1")
    fun getStoredPanel(): Panel?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrReplacePanel(panel: Panel)

    @Query("delete from Panel where id = :panelId")
    fun deletePanel(panelId: String): Int

    @Query("delete from Panel")
    fun deleteAllPanels(): Int
}