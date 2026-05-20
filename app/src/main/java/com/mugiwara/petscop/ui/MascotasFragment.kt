package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

class MascotasFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var apiService: PetscopApiService
    private lateinit var rvMascotas: RecyclerView
    private lateinit var tvEmptyMascotas: TextView
    private lateinit var btnAgregarMascota: Button
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
        btnAgregarMascota = view.findViewById(R.id.btnAgregarMascota)

        rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        rvMascotas.adapter = MascotaAdapter(mascotas)

        btnAgregarMascota.setOnClickListener {
            mostrarDialogoNuevaMascota()
        }

        cargarMascotas()
    }

    private fun mostrarDialogoNuevaMascota() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 0)
        }

        val etNombre = EditText(requireContext()).apply {
            hint = "Nombre"
            layout.addView(this)
        }

        val etEspecie = EditText(requireContext()).apply {
            hint = "Especie"
            layout.addView(this)
        }

        val etRaza = EditText(requireContext()).apply {
            hint = "Raza"
            layout.addView(this)
        }

        val etEdad = EditText(requireContext()).apply {
            hint = "Edad"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layout.addView(this)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.titulo_nueva_mascota))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_agregar_mascota)) { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val especie = etEspecie.text.toString().trim()
                val raza = etRaza.text.toString().trim()
                val edadText = etEdad.text.toString().trim()

                if (nombre.isEmpty() || especie.isEmpty() || raza.isEmpty() || edadText.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.mensaje_completa_campos), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val edad = edadText.toIntOrNull()
                if (edad == null) {
                    Toast.makeText(requireContext(), getString(R.string.mensaje_edad_valida), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                crearMascota(nombre, especie, raza, edad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearMascota(nombre: String, especie: String, raza: String, edad: Int) {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No se ha encontrado el usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val mascotaData = mapOf(
            "nombre" to nombre,
            "especie" to especie,
            "raza" to raza,
            "edad" to edad
        )

        lifecycleScope.launch {
            try {
                apiService.crearMascota(mascotaData, email)
                Toast.makeText(requireContext(), getString(R.string.mensaje_mascota_registrada), Toast.LENGTH_SHORT).show()
                cargarMascotas()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al registrar mascota: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
