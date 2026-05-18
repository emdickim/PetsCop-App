package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Mascota

class MascotaAdapter(
    private var mascotas: List<Mascota>
) : RecyclerView.Adapter<MascotaAdapter.MascotaViewHolder>() {

    class MascotaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre = view.findViewById<TextView>(R.id.tvMascotaNombre)
        val tvEspecie = view.findViewById<TextView>(R.id.tvMascotaEspecie)
        val tvRaza = view.findViewById<TextView>(R.id.tvMascotaRaza)
        val tvEdad = view.findViewById<TextView>(R.id.tvMascotaEdad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MascotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mascota, parent, false)
        return MascotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MascotaViewHolder, position: Int) {
        val mascota = mascotas[position]
        holder.tvNombre.text = mascota.nombre
        holder.tvEspecie.text = mascota.especie.ifEmpty { holder.itemView.context.getString(R.string.sin_especie) }
        holder.tvRaza.text = mascota.raza.ifEmpty { holder.itemView.context.getString(R.string.sin_raza) }
        holder.tvEdad.text = holder.itemView.context.getString(R.string.edad_mascota, mascota.edad)
    }

    override fun getItemCount(): Int = mascotas.size

    fun updateList(newList: List<Mascota>) {
        mascotas = newList
        notifyDataSetChanged()
    }
}
