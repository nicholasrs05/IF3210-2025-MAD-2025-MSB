package com.msb.purrytify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.msb.purrytify.data.local.converter.DateTimeConverter
import com.msb.purrytify.data.local.dao.ArtistDao
import com.msb.purrytify.data.local.dao.SongDao
import com.msb.purrytify.data.local.dao.SoundCapsuleDao
import com.msb.purrytify.data.local.entity.Artist
import com.msb.purrytify.data.local.entity.Song
import com.msb.purrytify.data.local.entity.SoundCapsule
import com.msb.purrytify.data.local.entity.DayStreak
import com.msb.purrytify.data.local.entity.DailyListeningTime
import com.msb.purrytify.data.local.entity.MonthlySongPlayCount

@Database(
    entities = [
        Song::class,
        SoundCapsule::class,
        DayStreak::class,
        DailyListeningTime::class,
        Artist::class,
        MonthlySongPlayCount::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class PurrytifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun soundCapsuleDao(): SoundCapsuleDao
    abstract fun artistDao(): ArtistDao

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