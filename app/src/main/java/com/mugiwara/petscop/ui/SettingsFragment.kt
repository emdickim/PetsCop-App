package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Clinica
import com.mugiwara.petscop.network.PetscopApiService
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var apiService: PetscopApiService
    private lateinit var spinnerClinicas: Spinner
    private lateinit var btnGuardar: Button
    
    private var clinicas = listOf<Clinica>()
    private var clinicaSeleccionadaId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        apiService = PetscopApiService.create()

        spinnerClinicas = view.findViewById(R.id.spinnerClinicas)
        btnGuardar = view.findViewById(R.id.btnGuardar)

        cargarClinicas()

        btnGuardar.setOnClickListener {
            guardarClinicaSeleccionada()
        }
    }

    private fun cargarClinicas() {
        lifecycleScope.launch {
            try {
                clinicas = apiService.getClinicas()
                val nombresClinicas = clinicas.map { it.nombre }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    nombresClinicas
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerClinicas.adapter = adapter

                // Cargar clínica actual del usuario
                cargarClinicaActual()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar clínicas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun cargarClinicaActual() {
        val firebaseUid = auth.currentUser?.uid ?: return
        
        db.collection("users").document(firebaseUid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val idClinicaActual = document.getLong("id_clinica")?.toInt() ?: 0
                    clinicaSeleccionadaId = idClinicaActual
                    
                    // Seleccionar la clínica actual en el spinner
                    val index = clinicas.indexOfFirst { it.id_clinica == idClinicaActual }
                    if (index >= 0) {
                        spinnerClinicas.setSelection(index)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al cargar clínica actual: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun guardarClinicaSeleccionada() {
        val index = spinnerClinicas.selectedItemPosition
        if (index < 0 || index >= clinicas.size) {
            Toast.makeText(requireContext(), "Selecciona una clínica", Toast.LENGTH_SHORT).show()
            return
        }

        val clinicaSeleccionada = clinicas[index]
        val firebaseUid = auth.currentUser?.uid ?: return

        // Actualizar en Firestore
        db.collection("users").document(firebaseUid)
            .update("id_clinica", clinicaSeleccionada.id_clinica)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Clínica actualizada: ${clinicaSeleccionada.nombre}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar clínica: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
