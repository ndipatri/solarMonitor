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
    suspend fun getStoredPanel(): Panel?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrReplacePanel(panel: Panel)

    @Query("delete from Panel where id = :panelId")
    fun deletePanel(panelId: String): Int

    @Query("delete from Panel")
    suspend fun deleteAllPanels(): Int
}