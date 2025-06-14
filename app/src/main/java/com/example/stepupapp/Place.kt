package com.example.stepupapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val date_saved: String,
    val steps_taken: String,
    val imageUri: String  // store as string path
)