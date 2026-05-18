package com.mugiwara.petscop.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Chat
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.ChatAdapter
import com.mugiwara.petscop.ui.chat.BuscarClienteBottomSheet
import kotlinx.coroutines.launch

class ListaChatsFragment : Fragment(R.layout.fragment_lista_chats) {

    private lateinit var rvChats: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var fabNuevoChat: FloatingActionButton
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var apiService: PetscopApiService
    private val listaChats = mutableListOf<Chat>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val miUid = auth.currentUser?.uid
        if (miUid == null) return

        apiService = PetscopApiService.create()
        rvChats = view.findViewById(R.id.rvListaChats)
        fabNuevoChat = view.findViewById(R.id.fabNuevoChat)

        // Adaptador para la lista principal de chats abiertos
        adapter = ChatAdapter(listaChats) { chatSeleccionado ->
            abrirChat(chatSeleccionado)
        }

        rvChats.layoutManager = LinearLayoutManager(requireContext())
        rvChats.adapter = adapter

        // AL PULSAR +: Abrimos la pestaña desplegable para buscar por Gmail
        fabNuevoChat.setOnClickListener {
            val bottomSheet = BuscarClienteBottomSheet(miUid) { usuarioEncontrado ->
                // Cuando se selecciona un usuario, abrimos el chat y recargamos lista
                abrirChat(usuarioEncontrado)
                cargarChats()
            }
            bottomSheet.show(childFragmentManager, "BuscarClienteBottomSheet")
        }

        // Cargar chats inicial
        cargarChats()
    }

    private fun cargarChats() {
        val miUid = auth.currentUser?.uid
        if (miUid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: No hay usuario logueado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val chatsObtenidos = apiService.getListaChats(miUid)
                listaChats.clear()
                listaChats.addAll(chatsObtenidos)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ListaChats", "Error al cargar chats: ${e.message}")
                Toast.makeText(requireContext(), "Error al cargar chats", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirChat(chat: Chat) {
        val otroUid = chat.uid
        val miId = auth.currentUser?.uid ?: return
        if (otroUid.isEmpty()) return

        // ID compartido (id_menor_id_mayor) para que ambos entren al mismo documento
        val chatIdCombinado = if (miId < otroUid) "${miId}_$otroUid" else "${otroUid}_$miId"

        val bundle = Bundle().apply {
            putString("chatId", chatIdCombinado)
            putString("nombre", chat.nombre)
        }

        findNavController().navigate(R.id.chatFragment, bundle)
    }
}