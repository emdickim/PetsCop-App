package com.mugiwara.petscop.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mugiwara.petscop.MainActivity
import com.mugiwara.petscop.R
import com.mugiwara.petscop.databinding.ActivityRegisterBinding
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.model.Clinica
import kotlinx.coroutines.launch
import android.util.Log

class RegisterActivity : AppCompatActivity() {

    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()
    private var esVeterinario = false
    
    private lateinit var apiService: PetscopApiService
    private var clinicaSeleccionada: Clinica? = null
    private var clinicas: List<Clinica> = emptyList()

    private var email = ""
    private var nombre = ""
    private var password = "" // Variable para enviar a FastAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        apiService = PetscopApiService.create()

        setupUI()
        observeViewModel()
        actualizarUISelector()
        cargarClinicas()
    }

    private fun setupUI() {
        binding.cardCliente.setOnClickListener {
            esVeterinario = false
            actualizarUISelector()
        }

        binding.cardVeterinario.setOnClickListener {
            esVeterinario = true
            actualizarUISelector()
        }

        binding.btnRegister.setOnClickListener {
            email = binding.etEmail.text.toString().trim()
            password = binding.etPassword.text.toString().trim()
            val confirmPass = binding.etConfirmPassword.text.toString().trim()
            nombre = binding.etNombre.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || nombre.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (esVeterinario && clinicaSeleccionada == null) {
                Toast.makeText(this, "Selecciona una clínica", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registrarUsuario(email, password)
        }

        binding.tvGoToLogin.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.registroState.collect { state ->
                when (state) {
                    is RegistroState.Cargando -> {
                        binding.btnRegister.isEnabled = false
                        binding.btnRegister.text = "Cargando..."
                    }
                    is RegistroState.Exito -> {
                        val uid = state.user?.uid
                        if (uid != null) {
                            guardarUsuarioEnFirestore(uid)
                        }
                    }
                    is RegistroState.Error -> {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "Crear cuenta"
                        Toast.makeText(this@RegisterActivity, state.mensaje, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun guardarUsuarioEnFirestore(uid: String) {
        val userMap = mutableMapOf<String, Any>(
            "uid" to uid,
            "nombre" to nombre,
            "email" to email,
            "esVeterinario" to esVeterinario,
            "fechaRegistro" to com.google.firebase.Timestamp.now()
        )

        if (esVeterinario && clinicaSeleccionada != null) {
            userMap["clinicaId"] = clinicaSeleccionada!!.id_clinica
            userMap["clinicaNombre"] = clinicaSeleccionada!!.nombre
        }

        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Firestore OK. Registrando en FastAPI...")
                registrarEnFastAPI()
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Error Firestore: ${e.message}")
                Toast.makeText(this, "Error al guardar perfil local: ${e.message}", Toast.LENGTH_SHORT).show()
                // Intentamos API de todos modos por si el error es solo de permisos
                registrarEnFastAPI()
            }
    }

    private fun registrarEnFastAPI() {
        lifecycleScope.launch {
            try {
                // FastAPI espera: nombre, apellidos, email, telefono, password
                val datos = mutableMapOf(
                    "nombre" to nombre,
                    "apellidos" to "", 
                    "email" to email,
                    "telefono" to "",
                    "password" to password
                )

                if (esVeterinario) {
                    datos["clinica"] = clinicaSeleccionada?.nombre ?: ""
                    apiService.registrarVeterinarioApi(datos)
                } else {
                    apiService.registrarClienteApi(datos)
                }

                Log.d("RegisterActivity", "FastAPI OK")
                irAMain()
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Error FastAPI: ${e.message}")
                Toast.makeText(this@RegisterActivity, "Perfil guardado en Firebase, pero hubo un error de sincronización", Toast.LENGTH_LONG).show()
                irAMain()
            }
        }
    }

    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun cargarClinicas() {
        lifecycleScope.launch {
            try {
                clinicas = apiService.getClinicasRegistro()
                val adapter = ArrayAdapter(
                    this@RegisterActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    clinicas.map { it.nombre }
                )
                
                val spinnerClinica = binding.layoutSeccionVet.findViewById<AutoCompleteTextView>(R.id.spinnerClinica)
                spinnerClinica?.setAdapter(adapter)
                spinnerClinica?.setOnItemClickListener { _, _, position, _ ->
                    clinicaSeleccionada = clinicas[position]
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Error clínicas: ${e.message}")
            }
        }
    }

    private fun actualizarUISelector() {
        val colorActive = ContextCompat.getColorStateList(this, R.color.primary)
        val colorInactive = ContextCompat.getColorStateList(this, R.color.border)

        if (esVeterinario) {
            binding.cardVeterinario.strokeWidth = 6
            binding.cardVeterinario.setStrokeColor(colorActive)
            binding.layoutSeccionVet.visibility = View.VISIBLE
            binding.cardCliente.strokeWidth = 2
            binding.cardCliente.setStrokeColor(colorInactive)
        } else {
            binding.cardCliente.strokeWidth = 6
            binding.cardCliente.setStrokeColor(colorActive)
            binding.layoutSeccionVet.visibility = View.GONE
            binding.cardVeterinario.strokeWidth = 2
            binding.cardVeterinario.setStrokeColor(colorInactive)
        }
    }
}
