package com.example.datagov.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dateTime: String, // Formato: "dd/MM/yyyy HH:mm"
    val municipality: String, // Municipio (distrito)
    val specificLocation: String, // Lugar específico (ej: "Sala de juntas")
    val estimatedAttendees: Int // Número estimado de asistentes
)

