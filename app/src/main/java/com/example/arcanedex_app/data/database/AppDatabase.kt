package com.example.arcanedex_app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.arcanedex_app.data.models.ArcaneEntity
import com.example.arcanedex_app.data.utils.ArcaneDao

@Database(entities = [ArcaneEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun arcaneDao(): ArcaneDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arcane_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
