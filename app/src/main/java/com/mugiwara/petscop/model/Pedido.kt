package com.mugiwara.petscop.model

import com.google.gson.annotations.SerializedName

data class PedidoItem(
    @SerializedName("nombre") val nombre: String = "",
    @SerializedName("cantidad") val cantidad: Int = 0,
    @SerializedName("precio") val precio: Double = 0.0
)

data class Pedido(
    @SerializedName("id_pedido") val id_pedido: Int = 0,
    @SerializedName("numero_pedido") val numero_pedido: String = "",
    @SerializedName("id_cliente") val id_cliente: Int = 0,
    @SerializedName("cliente_nombre") val cliente_nombre: String = "",
    @SerializedName("fecha") val fecha: String = "",
    @SerializedName("estado") val estado: String = "",
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("items") val items: List<PedidoItem> = emptyList(),
    @SerializedName("productos") val productos: List<String>? = null
)
