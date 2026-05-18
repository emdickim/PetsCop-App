package com.mugiwara.petscop.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Chat

class ChatAdapter(
    private val chatList: List<Chat>,
    private val onItemClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView? = view.findViewById(R.id.tvNombre)
        val tvHospital: TextView? = view.findViewById(R.id.tvHospital)
        val tvMensaje: TextView? = view.findViewById(R.id.tvUltimoMensaje)
        val tvHora: TextView? = view.findViewById(R.id.tvHora)
        val tvInicial: TextView? = view.findViewById(R.id.tvInicial)
        val tvNotif: TextView? = view.findViewById(R.id.tvNotificacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        
        holder.tvNombre?.text = chat.nombre
        holder.tvHospital?.text = chat.hospital
        holder.tvMensaje?.text = chat.ultimoMensaje
        holder.tvHora?.text = chat.hora
        holder.tvInicial?.text = chat.inicial
        
        if (chat.notificaciones > 0) {
            holder.tvNotif?.text = chat.notificaciones.toString()
            holder.tvNotif?.visibility = View.VISIBLE
        } else {
            holder.tvNotif?.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(chat)
        }
    }

    override fun getItemCount() = chatList.size
}