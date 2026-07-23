package com.example.smartpace.db.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocalRun::class], version = 1, exportSchema = false)
abstract class LocalRoomDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
}
