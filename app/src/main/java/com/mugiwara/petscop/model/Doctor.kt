package com.mugiwara.petscop.model

data class Doctor(
    val id: String = "",        // El ID del chat en Firebase
    val nombre: String = "",    // Nombre del veterinario
    val especialidad: String = "",
    val imagenUrl: String = ""  // Opcional
)