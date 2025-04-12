package com.msb.purrytify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.msb.purrytify.data.local.dao.SongDao
import com.msb.purrytify.data.local.entity.Song

@Database(entities = [Song::class], version = 2, exportSchema = false)
abstract class PurrytifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: PurrytifyDatabase? = null

        fun getDatabase(context: Context): PurrytifyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PurrytifyDatabase::class.java,
                    "purrytify_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}