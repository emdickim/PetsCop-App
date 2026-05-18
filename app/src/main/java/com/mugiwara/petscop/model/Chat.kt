package com.mugiwara.petscop.model

data class Chat(
    val uid: String = "",
    val nombre: String = "",
    val hospital: String = "",
    val ultimoMensaje: String = "",
    val hora: String = "",
    val inicial: String = "",
    val notificaciones: Int = 0
)
