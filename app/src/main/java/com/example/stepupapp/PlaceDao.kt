package com.example.stepupapp

import androidx.room.*

@Dao
interface PlaceDao
{
    @Insert
    suspend fun insert(place: Place)

    @Query("SELECT * FROM places ORDER BY id DESC")
    suspend fun getAll(): List<Place>
}