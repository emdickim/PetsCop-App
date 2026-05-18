package com.mugiwara.petscop.model

data class Producto(
    val id_producto: Int = 0,
    val nombre: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val stock: Int = 0,
    val categoria: String = "",
    val clinica_nombre: String = "",
    val foto_url: String? = null
)