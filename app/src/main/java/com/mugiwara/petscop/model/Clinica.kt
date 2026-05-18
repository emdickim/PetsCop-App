package com.mugiwara.petscop.model

import com.google.gson.annotations.SerializedName

data class Clinica(
    @SerializedName("id_clinica")
    val id_clinica: Int = 0,
    @SerializedName("nombre")
    val nombre: String = "",
    @SerializedName("direccion")
    val direccion: String = "",
    @SerializedName("telefono")
    val telefono: String = ""
)
