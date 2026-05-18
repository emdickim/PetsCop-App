package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.R
import com.mugiwara.petscop.network.PetscopApiService
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var apiService: PetscopApiService
    
    // Views - Información Personal
    private lateinit var tvNombre: TextView
    private lateinit var tvClinica: TextView
    private lateinit var etNombreCompleto: EditText
    private lateinit var etNumeroColegiado: EditText
    private lateinit var etTelefono: EditText
    private lateinit var spinnerEspecialidad: Spinner
    private lateinit var etAnosExperiencia: EditText
    private lateinit var btnGuardar: MaterialButton

    // Views - Seguridad
    private lateinit var etPasswordActual: EditText
    private lateinit var etPasswordNueva: EditText
    private lateinit var etPasswordConfirmar: EditText
    private lateinit var btnCambiarPassword: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
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

        // Inicializar vistas - Header e Información Personal
        tvNombre = view.findViewById(R.id.tvNombre)
        tvClinica = view.findViewById(R.id.tvClinica)
        etNombreCompleto = view.findViewById(R.id.etNombreCompleto)
        etNumeroColegiado = view.findViewById(R.id.etNumeroColegiado)
        etTelefono = view.findViewById(R.id.etTelefono)
        spinnerEspecialidad = view.findViewById(R.id.spinnerEspecialidad)
        etAnosExperiencia = view.findViewById(R.id.etAnosExperiencia)
        btnGuardar = view.findViewById(R.id.btnGuardarPerfil)

        // Inicializar vistas - Seguridad
        etPasswordActual = view.findViewById(R.id.etPasswordActual)
        etPasswordNueva = view.findViewById(R.id.etPasswordNueva)
        etPasswordConfirmar = view.findViewById(R.id.etPasswordConfirmar)
        btnCambiarPassword = view.findViewById(R.id.btnCambiarPassword)

        // Configurar especialidades
        val especialidades = listOf(
            "Cirugía",
            "Medicina interna",
            "Dermatología",
            "Odontología",
            "Oncología"
        )
        val adapterEspecialidades = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            especialidades
        )
        adapterEspecialidades.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEspecialidad.adapter = adapterEspecialidades

        cargarPerfil()

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        btnCambiarPassword.setOnClickListener {
            cambiarContrasena()
        }
    }

    private fun cargarPerfil() {
        val firebaseUid = auth.currentUser?.uid ?: return

        db.collection("users").document(firebaseUid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: ""
                    val numeroColegiado = document.getString("numero_colegiado") ?: ""
                    val telefono = document.getString("telefono") ?: ""
                    val especialidad = document.getString("especialidad") ?: "Cirugía"
                    val anosExperiencia = document.getLong("anos_experiencia")?.toInt() ?: 0
                    val idClinica = document.getLong("id_clinica")?.toInt() ?: 0

                    tvNombre.text = nombre
                    etNombreCompleto.setText(nombre)
                    etNumeroColegiado.setText(numeroColegiado)
                    etTelefono.setText(telefono)
                    etAnosExperiencia.setText(anosExperiencia.toString())

                    val especialidades = listOf(
                        "Cirugía",
                        "Medicina interna",
                        "Dermatología",
                        "Odontología",
                        "Oncología"
                    )
                    val index = especialidades.indexOf(especialidad)
                    if (index >= 0) {
                        spinnerEspecialidad.setSelection(index)
                    }

                    cargarNombreClinica(idClinica)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarNombreClinica(idClinica: Int) {
        lifecycleScope.launch {
            try {
                val clinicas = apiService.getClinicas()
                val clinica = clinicas.find { it.id_clinica == idClinica }
                if (clinica != null) {
                    tvClinica.text = "📍 ${clinica.nombre}"
                }
            } catch (e: Exception) {
                // Silenciar error
            }
        }
    }

    private fun guardarCambios() {
        val nombre = etNombreCompleto.text.toString().trim()
        val numeroColegiado = etNumeroColegiado.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val especialidad = spinnerEspecialidad.selectedItem.toString()
        val anosExperiencia = etAnosExperiencia.text.toString().trim().toIntOrNull() ?: 0

        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val firebaseUid = auth.currentUser?.uid ?: return

        val datosActualizados = mapOf(
            "nombre" to nombre,
            "numero_colegiado" to numeroColegiado,
            "telefono" to telefono,
            "especialidad" to especialidad,
            "anos_experiencia" to anosExperiencia
        )

        db.collection("users").document(firebaseUid)
            .update(datosActualizados)
            .addOnSuccessListener {
                tvNombre.text = nombre
                Toast.makeText(requireContext(), "Cambios guardados exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cambiarContrasena() {
        val passwordActual = etPasswordActual.text.toString()
        val passwordNueva = etPasswordNueva.text.toString()
        val passwordConfirmar = etPasswordConfirmar.text.toString()

        if (passwordActual.isEmpty() || passwordNueva.isEmpty() || passwordConfirmar.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (passwordNueva != passwordConfirmar) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (passwordNueva.length < 6) {
            Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        val email = user?.email ?: return

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, passwordActual)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(passwordNueva)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Contraseña cambiada exitosamente", Toast.LENGTH_SHORT).show()
                        etPasswordActual.setText("")
                        etPasswordNueva.setText("")
                        etPasswordConfirmar.setText("")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al cambiar contraseña: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
            }
    }
}
