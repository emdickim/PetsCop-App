package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Cita

class CitaVeterinarioAdapter(
    private var citas: List<Cita>,
    private val onAceptar: (Cita) -> Unit,
    private val onRechazar: (Cita) -> Unit
) : RecyclerView.Adapter<CitaVeterinarioAdapter.CitaViewHolder>() {

    class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha = view.findViewById<TextView>(R.id.tvFechaCita)
        val tvHora = view.findViewById<TextView>(R.id.tvHoraCita)
        val tvCliente = view.findViewById<TextView>(R.id.tvClienteCita)
        val tvMotivo = view.findViewById<TextView>(R.id.tvMotivoCita)
        val tvEstado = view.findViewById<TextView>(R.id.tvEstadoCita)
        val btnAceptar = view.findViewById<Button>(R.id.btnAceptarCita)
        val btnRechazar = view.findViewById<Button>(R.id.btnRechazarCita)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cita_veterinario, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]
        
        holder.tvFecha.text = cita.fecha
        holder.tvHora.text = if (cita.hora.length >= 5) cita.hora.substring(0, 5) else cita.hora
        holder.tvCliente.text = cita.nombre_otro
        holder.tvMotivo.text = cita.motivo
        holder.tvEstado.text = cita.id_cita.toString() // Mostramos estado o ID temporal
        
        // Mostrar botones solo si está pendiente
        if (cita.id_cita != 0) {
            holder.btnAceptar.visibility = View.VISIBLE
            holder.btnRechazar.visibility = View.VISIBLE
            
            holder.btnAceptar.setOnClickListener {
                onAceptar(cita)
            }
            
            holder.btnRechazar.setOnClickListener {
                onRechazar(cita)
            }
        }
    }

    override fun getItemCount() = citas.size

    fun updateList(newList: List<Cita>) {
        citas = newList
        notifyDataSetChanged()
    }
}
