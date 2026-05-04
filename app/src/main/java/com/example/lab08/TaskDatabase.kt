package com.example.lab08

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lab08.com.example.lab08.Task


@Database(entities = [Task::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
