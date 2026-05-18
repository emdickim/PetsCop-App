package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Cita

class CitaAdapter(private var citas: List<Cita>) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

    class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHora = view.findViewById<TextView>(R.id.tvHoraCita)
        val tvMotivo = view.findViewById<TextView>(R.id.tvMotivoCita)
        val tvNombre = view.findViewById<TextView>(R.id.tvNombreOtro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cita, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]
        // Formatear hora si es necesario (ej: 10:30:00 -> 10:30)
        holder.tvHora.text = if (cita.hora.length >= 5) cita.hora.substring(0, 5) else cita.hora
        holder.tvMotivo.text = cita.motivo
        holder.tvNombre.text = cita.nombre_otro
    }

    override fun getItemCount() = citas.size

    fun updateList(newList: List<Cita>) {
        citas = newList
        notifyDataSetChanged()
    }
}
