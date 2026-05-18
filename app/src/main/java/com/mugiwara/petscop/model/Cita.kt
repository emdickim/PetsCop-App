package com.mugiwara.petscop.model

import com.google.gson.annotations.SerializedName

data class Cita(
    @SerializedName("id_cita")
    val id_cita: Int = 0,
    @SerializedName("fecha")
    val fecha: String = "",
    @SerializedName("hora")
    val hora: String = "",
    @SerializedName("motivo")
    val motivo: String = "",
    @SerializedName("nombre_otro")
    val nombre_otro: String = "",
    @SerializedName("id_mascota")
    val id_mascota: Int = 0,
    @SerializedName("id_veterinario")
    val id_veterinario: String = "",
    @SerializedName("id_clinica")
    val id_clinica: Int = 0
)