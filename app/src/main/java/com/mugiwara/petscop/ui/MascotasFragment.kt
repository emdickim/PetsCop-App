package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Mascota
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.MascotaAdapter
import kotlinx.coroutines.launch
import android.widget.Toast

class MascotasFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var apiService: PetscopApiService
    private lateinit var rvMascotas: RecyclerView
    private lateinit var tvEmptyMascotas: TextView
    private val mascotas = mutableListOf<Mascota>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mascotas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        apiService = PetscopApiService.create()

        rvMascotas = view.findViewById(R.id.rvMascotas)
        tvEmptyMascotas = view.findViewById(R.id.tvEmptyMascotas)

        rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        rvMascotas.adapter = MascotaAdapter(mascotas)

        cargarMascotas()
    }

    private fun cargarMascotas() {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            tvEmptyMascotas.text = getString(R.string.error_usuario_no_encontrado)
            tvEmptyMascotas.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                val lista = apiService.getMascotasPorCliente(email)
                mascotas.clear()
                mascotas.addAll(lista)
                rvMascotas.adapter = MascotaAdapter(mascotas)
                actualizarVista(lista)
            } catch (e: Exception) {
                tvEmptyMascotas.text = getString(R.string.error_cargar_mascotas, e.message ?: "")
                tvEmptyMascotas.visibility = View.VISIBLE
            }
        }
    }

    private fun actualizarVista(lista: List<Mascota>) {
        if (lista.isEmpty()) {
            tvEmptyMascotas.visibility = View.VISIBLE
            rvMascotas.visibility = View.GONE
        } else {
            tvEmptyMascotas.visibility = View.GONE
            rvMascotas.visibility = View.VISIBLE
        }
    }
}
