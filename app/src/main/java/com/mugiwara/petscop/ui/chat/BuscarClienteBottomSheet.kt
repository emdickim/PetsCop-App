package com.mugiwara.petscop.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Chat
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.ChatAdapter
import kotlinx.coroutines.launch
import android.util.Log

class BuscarClienteBottomSheet(
    private val miUid: String,
    private val onUserSelected: (Chat) -> Unit
) : BottomSheetDialogFragment() {

    private val apiService = PetscopApiService.create()
    private val listaSugerencias = mutableListOf<Chat>()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_buscar_cliente, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etBuscar = view.findViewById<EditText>(R.id.etBuscarEmail)
        val rvSugerencias = view.findViewById<RecyclerView>(R.id.rvSugerencias)

        adapter = ChatAdapter(listaSugerencias) { chat ->
            onUserSelected(chat)
            dismiss()
        }

        rvSugerencias.layoutManager = LinearLayoutManager(requireContext())
        rvSugerencias.adapter = adapter

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().lowercase()
                if (query.length >= 3) {
                    buscarUsuarios(query)
                } else {
                    listaSugerencias.clear()
                    adapter.notifyDataSetChanged()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun buscarUsuarios(query: String) {
        Log.d("BuscarCliente", "Buscando: $query")
        lifecycleScope.launch {
            try {
                Log.d("BuscarCliente", "Llamando API con query: $query")
                val usuariosApi = apiService.buscarClientes(query)
                Log.d("BuscarCliente", "API respondió con ${usuariosApi.size} usuarios")
                listaSugerencias.clear()

                usuariosApi.forEach { user ->
                    Log.d("BuscarCliente", "Usuario encontrado: ${user.nombre} (${user.email})")
                    // Usamos el ID de la base de datos de FastAPI como UID
                    val uidStr = user.id.toString()

                    if (uidStr != miUid) {
                        listaSugerencias.add(Chat(
                            uid = uidStr,
                            nombre = user.nombre,
                            hospital = user.email, // Mostramos el email como subtítulo
                            inicial = user.nombre.take(1).uppercase()
                        ))
                    }
                }
                Log.d("BuscarCliente", "Total de usuarios mostrados: ${listaSugerencias.size}")
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("BuscarCliente", "Error al buscar: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getTheme(): Int = R.style.PetscopBottomSheetDialog
}
