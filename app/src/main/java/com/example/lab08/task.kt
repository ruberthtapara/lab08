package com.example.lab08.com.example.lab08 // Asegúrate de que coincida con tu paquete

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val isCompleted: Boolean = false,
    val priority: String = "Media",
    val category: String = "General",
    val repeatInterval: Int = 0,
    val createdAt: Long = System.currentTimeMillis() // <--- Valor por defecto para evitar errores
)