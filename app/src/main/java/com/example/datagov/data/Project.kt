package com.example.datagov.data

data class Project(
    val id: String = "",
    val name: String = "",
    val ubicacion: String = "",
    val categoryId: String = "",
    val createdAt: Long = 0L,
    val description: String = "",
    val presupuesto: Long = 0L,
    val avance: Int = 0,
    val picUrl: String = "",
    val codigo_snip: String = ""
)

