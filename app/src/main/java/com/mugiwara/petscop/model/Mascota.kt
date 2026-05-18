package com.mugiwara.petscop.model

import com.google.gson.annotations.SerializedName

data class Mascota(
    @SerializedName("id_mascota")
    val id_mascota: Int = 0,
    @SerializedName("nombre")
    val nombre: String = "",
    @SerializedName("especie")
    val especie: String = "",
    @SerializedName("raza")
    val raza: String = "",
    @SerializedName("edad")
    val edad: Int = 0
)
