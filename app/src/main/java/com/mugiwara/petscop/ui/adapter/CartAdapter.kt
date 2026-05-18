package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mugiwara.petscop.R
import com.mugiwara.petscop.ui.CartItem
import com.squareup.picasso.Picasso
import android.widget.Toast

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onCantidadChange: (CartItem, Int) -> Unit,
    private val onEliminar: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onCantidadChange, onEliminar)
    }

    override fun getItemCount(): Int = items.size

    class CartViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val ivFoto = itemView.findViewById<ImageView>(R.id.ivFotoCarrito)
        private val tvNombre = itemView.findViewById<TextView>(R.id.tvNombreCarrito)
        private val tvPrecio = itemView.findViewById<TextView>(R.id.tvPrecioCarrito)
        private val etCantidad = itemView.findViewById<EditText>(R.id.etCantidadCarrito)
        private val btnMenos = itemView.findViewById<MaterialButton>(R.id.btnMenosCarrito)
        private val btnMas = itemView.findViewById<MaterialButton>(R.id.btnMasCarrito)
        private val btnEliminar = itemView.findViewById<MaterialButton>(R.id.btnEliminarCarrito)

        fun bind(
            item: CartItem,
            onCantidadChange: (CartItem, Int) -> Unit,
            onEliminar: (CartItem) -> Unit
        ) {
            tvNombre.text = item.nombre
            tvPrecio.text = "€${String.format("%.2f", item.precio)}"
            etCantidad.setText(item.cantidad.toString())

            if (!item.foto_url.isNullOrEmpty()) {
                Picasso.get()
                    .load(item.foto_url)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(ivFoto)
            }

            btnMenos.setOnClickListener {
                val nuevaCantidad = item.cantidad - 1
                if (nuevaCantidad > 0) {
                    etCantidad.setText(nuevaCantidad.toString())
                    onCantidadChange(item, nuevaCantidad)
                }
            }

            btnMas.setOnClickListener {
                val nuevaCantidad = item.cantidad + 1
                etCantidad.setText(nuevaCantidad.toString())
                onCantidadChange(item, nuevaCantidad)
            }

            btnEliminar.setOnClickListener {
                onEliminar(item)
            }
        }
    }
}
