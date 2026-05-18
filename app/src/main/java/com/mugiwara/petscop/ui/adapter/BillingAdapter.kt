package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mugiwara.petscop.R
import com.mugiwara.petscop.ui.Factura

class BillingAdapter(private val facturas: List<Factura>) : RecyclerView.Adapter<BillingAdapter.BillingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_factura, parent, false)
        return BillingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BillingViewHolder, position: Int) {
        val factura = facturas[position]
        holder.bind(factura)
    }

    override fun getItemCount(): Int = facturas.size

    class BillingViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvFecha = itemView.findViewById<TextView>(R.id.tvFecha)
        private val tvPlan = itemView.findViewById<TextView>(R.id.tvPlan)
        private val tvImporte = itemView.findViewById<TextView>(R.id.tvImporte)
        private val tvEstado = itemView.findViewById<TextView>(R.id.tvEstado)
        private val tvFactura = itemView.findViewById<TextView>(R.id.tvFactura)

        fun bind(factura: Factura) {
            tvFecha.text = factura.fecha
            tvPlan.text = factura.plan
            tvImporte.text = "€${String.format("%.2f", factura.importe)}"
            tvEstado.text = factura.estado
            tvFactura.text = "Descargar"
        }
    }
}
