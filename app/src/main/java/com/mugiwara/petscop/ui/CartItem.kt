package com.mugiwara.petscop.ui

data class CartItem(
    val id_producto: Int = 0,
    val nombre: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 1,
    val foto_url: String? = null
)

data class Pedido(
    val id_pedido: Int = 0,
    val fecha: String = "",
    val total: Double = 0.0,
    val estado: String = "",
    val direccion: String = "",
    val items: List<CartItem> = emptyList()
)
