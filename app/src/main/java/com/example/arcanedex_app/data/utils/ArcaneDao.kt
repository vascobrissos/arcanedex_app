package com.example.arcanedex_app.data.utils

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.arcanedex_app.data.models.ArcaneEntity

@Dao
interface ArcaneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(arcanes: List<ArcaneEntity>)

    @Query("SELECT * FROM arcanes")
    fun getAllArcanes(): List<ArcaneEntity>

    @Query("DELETE FROM arcanes")
    fun clearCache()
}
