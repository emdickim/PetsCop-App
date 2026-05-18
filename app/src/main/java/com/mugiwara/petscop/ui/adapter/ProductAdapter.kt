package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Producto
import com.squareup.picasso.Picasso

class ProductAdapter(
    private val productos: List<Producto>,
    private val onComprarClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val producto = productos[position]
        holder.bind(producto, onComprarClick)
    }

    override fun getItemCount(): Int = productos.size

    class ProductViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val ivProducto = itemView.findViewById<ImageView>(R.id.ivProducto)
        private val tvNombreProd = itemView.findViewById<TextView>(R.id.tvNombreProd)
        private val tvCategoriaTag = itemView.findViewById<TextView>(R.id.tvCategoriaTag)
        private val tvPrecio = itemView.findViewById<TextView>(R.id.tvPrecio)
        private val tvStock = itemView.findViewById<TextView>(R.id.tvStock)
        private val btnAgregar = itemView.findViewById<MaterialButton>(R.id.btnAgregar)

        fun bind(producto: Producto, onComprarClick: (Producto) -> Unit) {
            tvNombreProd.text = producto.nombre
            tvCategoriaTag.text = producto.categoria
            tvPrecio.text = "€${String.format("%.2f", producto.precio)}"
            tvStock.text = "Stock: ${producto.stock} unidades"
            
            // Cargar imagen con Picasso
            if (!producto.foto_url.isNullOrEmpty()) {
                Picasso.get()
                    .load(producto.foto_url)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(ivProducto)
            }
            
            // Habilitar/deshabilitar botón según stock
            btnAgregar.isEnabled = producto.stock > 0
            btnAgregar.setOnClickListener {
                onComprarClick(producto)
            }
        }
    }
}
