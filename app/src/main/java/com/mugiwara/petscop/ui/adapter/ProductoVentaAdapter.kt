package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mugiwara.petscop.databinding.ItemProductoVentaBinding
import com.mugiwara.petscop.model.Producto
import com.squareup.picasso.Picasso

class ProductoVentaAdapter(
    private val productos: List<Producto>,
    private val onEditar: (Producto) -> Unit,
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<ProductoVentaAdapter.ProductoViewHolder>() {

    inner class ProductoViewHolder(val binding: ItemProductoVentaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Producto) {
            binding.tvProductoNombre.text = producto.nombre
            binding.tvProductoCategoria.text = producto.categoria
            binding.tvProductoPrecio.text = "€${String.format("%.2f", producto.precio)}"
            binding.tvProductoStock.text = "Stock: ${producto.stock}"

            // Cargar imagen
            if (!producto.foto_url.isNullOrEmpty()) {
                Picasso.get().load(producto.foto_url).into(binding.ivProductoFoto)
            }

            // Botones de acción
            binding.btnEditar.setOnClickListener {
                onEditar(producto)
            }

            binding.btnEliminar.setOnClickListener {
                onEliminar(producto.id_producto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoVentaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size
}
