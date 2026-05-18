package com.mugiwara.petscop.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("nombre")
    val nombre: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("es_veterinario")
    val esVeterinario: Boolean = false
)
