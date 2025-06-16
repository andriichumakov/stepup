package com.example.stepupapp

import androidx.room.*

@Dao
interface PlaceDao
{
    @Insert
    suspend fun insert(place: Place)

    @Query("SELECT * FROM places ORDER BY id DESC")
    suspend fun getAll(): List<Place>

    @Query("SELECT * FROM places ORDER BY date_saved DESC LIMIT 1")
    fun getLatestPlace(): Place?

}