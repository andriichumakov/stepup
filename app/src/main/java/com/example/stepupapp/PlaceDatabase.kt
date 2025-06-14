package com.example.stepupapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Place::class], version = 1)
abstract class PlaceDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao

    companion object {
        @Volatile private var INSTANCE: PlaceDatabase? = null

        fun getDatabase(context: Context): PlaceDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PlaceDatabase::class.java,
                    "place_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}