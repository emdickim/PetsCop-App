package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mugiwara.petscop.databinding.ItemPedidoVentaBinding
import com.mugiwara.petscop.model.Pedido

class PedidoAdapter(
    private val pedidos: List<Pedido>,
    private val onCambiarEstado: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    inner class PedidoViewHolder(val binding: ItemPedidoVentaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pedido: Pedido) {
            binding.tvNumeroPedido.text = "Pedido #${pedido.numero_pedido}"
            binding.tvCliente.text = pedido.cliente_nombre
            binding.tvTotal.text = "€${String.format("%.2f", pedido.total)}"
            binding.tvFecha.text = pedido.fecha

            // Mostrar estado con color según el estado
            binding.tvEstado.text = pedido.estado.uppercase()
            val colorEstado = when (pedido.estado) {
                "pendiente" -> android.graphics.Color.parseColor("#FF9800")
                "confirmado" -> android.graphics.Color.parseColor("#2196F3")
                "enviado" -> android.graphics.Color.parseColor("#9C27B0")
                "entregado" -> android.graphics.Color.parseColor("#4CAF50")
                else -> android.graphics.Color.GRAY
            }
            binding.tvEstado.setTextColor(colorEstado)

            // Listar items del pedido
            val itemsText = pedido.items.joinToString("\n") { item ->
                "${item.nombre} x${item.cantidad}"
            }
            binding.tvItems.text = itemsText

            // Botón cambiar estado
            binding.btnCambiarEstado.setOnClickListener {
                onCambiarEstado(pedido)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoVentaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(pedidos[position])
    }

    override fun getItemCount(): Int = pedidos.size
}
