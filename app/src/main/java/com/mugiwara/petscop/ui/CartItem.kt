package com.mugiwara.petscop.ui

data class CartItem(
    val id_producto: Int = 0,
    val nombre: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 1,
    val foto_url: String? = null
)

