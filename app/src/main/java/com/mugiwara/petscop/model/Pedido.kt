package com.mugiwara.petscop.model

data class Pedido(
    val id_pedido: Int = 0,
    val numero_pedido: String = "",
    val cliente_nombre: String = "",
    val items: List<ItemPedido> = emptyList(),
    val total: Double = 0.0,
    val estado: String = "", // pendiente, confirmado, enviado, entregado
    val fecha: String = "",
    val direccion: String = ""
)

data class ItemPedido(
    val id_producto: Int = 0,
    val nombre: String = "",
    val cantidad: Int = 0,
    val precio: Double = 0.0
)
