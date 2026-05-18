package com.mugiwara.petscop.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.mugiwara.petscop.MainActivity
import com.mugiwara.petscop.R
import com.mugiwara.petscop.databinding.ActivityRegisterBinding
import com.mugiwara.petscop.network.PetscopApiService
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var apiService: PetscopApiService
    private var esVeterinario = false
    private var clinicaSeleccionada: String = ""
    private var clinicaSeleccionadaId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiService = PetscopApiService.create()
        
        setupUI()
        
        // Inicializar el diseño del selector
        actualizarUISelector()
    }

    private fun setupUI() {
        binding.cardCliente.setOnClickListener {
            esVeterinario = false
            actualizarUISelector()
        }

        binding.cardVeterinario.setOnClickListener {
            esVeterinario = true
            actualizarUISelector()
            cargarClinicas()
        }

        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val apellidos = binding.etApellidos.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val telefono = binding.etTelefono.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            val confirmPass = binding.etConfirmPassword.text.toString().trim()

            // Validaciones generales
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa tu nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (apellidos.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa tus apellidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa tu email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (telefono.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa tu teléfono", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validaciones específicas para veterinarios
            if (esVeterinario && clinicaSeleccionadaId == 0) {
                Toast.makeText(this, "Por favor, selecciona una clínica", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Registrar usuario
            if (esVeterinario) {
                registrarVeterinario(nombre, apellidos, email, telefono, pass)
            } else {
                registrarCliente(nombre, apellidos, email, telefono, pass)
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            finish() // Vuelve a la pantalla de Login
        }
    }

    private fun cargarClinicas() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("RegisterActivity", "Iniciando carga de clínicas...")
                val clinicas = apiService.getClinicasRegistro()
                
                android.util.Log.d("RegisterActivity", "Clínicas cargadas: ${clinicas.size}")
                for (clinica in clinicas) {
                    android.util.Log.d("RegisterActivity", "- ${clinica.nombre} (ID: ${clinica.id_clinica})")
                }
                
                if (clinicas.isEmpty()) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "No hay clínicas disponibles en el API",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val clinicasNombres = clinicas.map { it.nombre }.toMutableList()
                val clinicasIds = clinicas.map { it.id_clinica }

                // Crear adapter para AutoCompleteTextView
                val adapter = ArrayAdapter(
                    this@RegisterActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    clinicasNombres
                )
                
                binding.spinnerClinica.setAdapter(adapter)
                binding.spinnerClinica.setText("")
                
                // Listeners para mostrar el dropdown
                binding.spinnerClinica.setOnClickListener {
                    android.util.Log.d("RegisterActivity", "Click en spinnerClinica")
                    binding.spinnerClinica.showDropDown()
                }
                
                binding.spinnerClinica.setOnFocusChangeListener { _, hasFocus ->
                    android.util.Log.d("RegisterActivity", "Focus change: $hasFocus")
                    if (hasFocus) {
                        binding.spinnerClinica.showDropDown()
                    }
                }
                
                // Cuando el usuario selecciona una clínica
                binding.spinnerClinica.setOnItemClickListener { parent, _, position, _ ->
                    android.util.Log.d("RegisterActivity", "Item seleccionado en posición: $position")
                    if (position >= 0 && position < clinicasIds.size) {
                        clinicaSeleccionada = clinicasNombres[position]
                        clinicaSeleccionadaId = clinicasIds[position]
                        android.util.Log.d("RegisterActivity", "✓ Clínica seleccionada: $clinicaSeleccionada (ID: $clinicaSeleccionadaId)")
                        Toast.makeText(this@RegisterActivity, "Clínica seleccionada: $clinicaSeleccionada", Toast.LENGTH_SHORT).show()
                    }
                }
                
                // Mostrar el dropdown automáticamente después de cargar
                binding.spinnerClinica.post {
                    binding.spinnerClinica.showDropDown()
                }
                
                Toast.makeText(this@RegisterActivity, "Clínicas cargadas correctamente", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("RegisterActivity", "Error al cargar clínicas: ${e.message}", e)
                Toast.makeText(
                    this@RegisterActivity,
                    "Error al cargar clínicas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun actualizarUISelector() {
        val colorPrimary = ContextCompat.getColor(this, R.color.primary)
        val colorBorder = ContextCompat.getColor(this, R.color.border)
        val colorMuted = ContextCompat.getColor(this, R.color.muted_foreground)

        if (esVeterinario) {
            // Veterinario Activo
            binding.cardVeterinario.strokeWidth = 6
            binding.cardVeterinario.setStrokeColor(android.content.res.ColorStateList.valueOf(colorPrimary))
            binding.tvLabelVet.setTextColor(colorPrimary)
            binding.ivIconVet.imageTintList = android.content.res.ColorStateList.valueOf(colorPrimary)

            // Cliente Inactivo
            binding.cardCliente.strokeWidth = 2
            binding.cardCliente.setStrokeColor(android.content.res.ColorStateList.valueOf(colorBorder))
            binding.tvLabelCliente.setTextColor(colorMuted)
            binding.ivIconCliente.imageTintList = android.content.res.ColorStateList.valueOf(colorMuted)
            
            binding.layoutSeccionVet.visibility = View.VISIBLE
        } else {
            // Cliente Activo
            binding.cardCliente.strokeWidth = 6
            binding.cardCliente.setStrokeColor(android.content.res.ColorStateList.valueOf(colorPrimary))
            binding.tvLabelCliente.setTextColor(colorPrimary)
            binding.ivIconCliente.imageTintList = android.content.res.ColorStateList.valueOf(colorPrimary)

            // Veterinario Inactivo
            binding.cardVeterinario.strokeWidth = 2
            binding.cardVeterinario.setStrokeColor(android.content.res.ColorStateList.valueOf(colorBorder))
            binding.tvLabelVet.setTextColor(colorMuted)
            binding.ivIconVet.imageTintList = android.content.res.ColorStateList.valueOf(colorMuted)
            
            binding.layoutSeccionVet.visibility = View.GONE
        }
    }

    private fun registrarVeterinario(nombre: String, apellidos: String, email: String, telefono: String, password: String) {
        lifecycleScope.launch {
            try {
                binding.btnRegister.isEnabled = false
                binding.btnRegister.text = "Registrando..."

                // 1. Crear autenticación en Firebase
                val authResult = try {
                    val task = FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email, password)
                    // Esperar a que se complete
                    var result: com.google.firebase.auth.AuthResult? = null
                    var exception: Exception? = null
                    
                    task.addOnSuccessListener { result = it }
                    task.addOnFailureListener { exception = it as Exception }
                    
                    // Esperar brevemente a que se complete
                    var retries = 0
                    while (result == null && exception == null && retries < 50) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                    
                    if (exception != null) throw exception!!
                    result
                } catch (e: Exception) {
                    android.util.Log.e("RegisterActivity", "Error en Firebase Auth: ${e.message}", e)
                    Toast.makeText(this@RegisterActivity, "Error de autenticación: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Crear cuenta"
                    return@launch
                }

                if (authResult == null) {
                    Toast.makeText(this@RegisterActivity, "Error al crear cuenta", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Crear cuenta"
                    return@launch
                }

                val userId = authResult!!.user?.uid ?: run {
                    Toast.makeText(this@RegisterActivity, "Error: No se obtuvo ID de usuario", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Crear cuenta"
                    return@launch
                }

                // 2. Guardar datos del veterinario en Firestore
                val db = FirebaseFirestore.getInstance()
                val datosVeterinario = mapOf(
                    "email" to email,
                    "nombre" to nombre,
                    "apellidos" to apellidos,
                    "telefono" to telefono,
                    "id_clinica" to clinicaSeleccionadaId,
                    "clinica_nombre" to clinicaSeleccionada,
                    "esVeterinario" to true,
                    "createdAt" to Timestamp.now()
                )

                try {
                    var firestoreSuccess = false
                    var firestoreError: Exception? = null
                    
                    db.collection("users").document(userId)
                        .set(datosVeterinario)
                        .addOnSuccessListener { firestoreSuccess = true }
                        .addOnFailureListener { firestoreError = it as Exception }
                    
                    // Esperar a que Firestore responda
                    var retries = 0
                    while (!firestoreSuccess && firestoreError == null && retries < 50) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                    
                    if (firestoreError != null) throw firestoreError!!
                    if (!firestoreSuccess) throw Exception("Timeout guardando en Firestore")
                    
                    android.util.Log.d("RegisterActivity", "✓ Veterinario registrado en Firestore")
                    Toast.makeText(this@RegisterActivity, "¡Veterinario registrado exitosamente!", Toast.LENGTH_SHORT).show()
                    irAMainActivity()
                } catch (e: Exception) {
                    android.util.Log.e("RegisterActivity", "Error guardando en Firestore: ${e.message}", e)
                    Toast.makeText(this@RegisterActivity, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Crear cuenta"
                }
            } catch (e: Exception) {
                android.util.Log.e("RegisterActivity", "Error general: ${e.message}", e)
                Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Crear cuenta"
            }
        }
    }

    private fun registrarCliente(nombre: String, apellidos: String, email: String, telefono: String, password: String) {
        lifecycleScope.launch {
            try {
                binding.btnRegister.isEnabled = false
                binding.btnRegister.text = "Registrando..."

                // 1. Crear autenticación en Firebase
                val authResult = try {
                    val task = FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email, password)
                    // Esperar a que se complete
                    var result: com.google.firebase.auth.AuthResult? = null
                    var exception: Exception? = null
                    
                    task.addOnSuccessListener { result = it }
                    task.addOnFailureListener { exception = it as Exception }
                    
                    // Esperar brevemente a que se complete
                    var retries = 0
                    while (result == null && exception == null && retries < 50) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                    
                    if (exception != null) throw exception!!
                    result
                } catch (e: Exception) {
                    android.util.Log.e("RegisterActivity", "Error en Firebase Auth: ${e.message}", e)
                    Toast.makeText(this@RegisterActivity, "Error de autenticación: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Crear cuenta"
                    return@launch
                }

                if (authResult == null) {
                    Toast.makeText(this@RegisterActivity, "Error al crear cuenta", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Crear cuenta"
                    return@launch
                }

                val userId = authResult!!.user?.uid ?: run {
                    Toast.makeText(this@RegisterActivity, "Error: No se obtuvo ID de usuario", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Crear cuenta"
                    return@launch
                }

                // 2. Guardar datos del cliente en Firestore
                val db = FirebaseFirestore.getInstance()
                val datosCliente = mapOf(
                    "email" to email,
                    "nombre" to nombre,
                    "apellidos" to apellidos,
                    "telefono" to telefono,
                    "esVeterinario" to false,
                    "createdAt" to Timestamp.now()
                )

                try {
                    var firestoreSuccess = false
                    var firestoreError: Exception? = null
                    
                    db.collection("users").document(userId)
                        .set(datosCliente)
                        .addOnSuccessListener { firestoreSuccess = true }
                        .addOnFailureListener { firestoreError = it as Exception }
                    
                    // Esperar a que Firestore responda
                    var retries = 0
                    while (!firestoreSuccess && firestoreError == null && retries < 50) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                    
                    if (firestoreError != null) throw firestoreError!!
                    if (!firestoreSuccess) throw Exception("Timeout guardando en Firestore")
                    
                    android.util.Log.d("RegisterActivity", "✓ Cliente registrado en Firestore")
                    Toast.makeText(this@RegisterActivity, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show()
                    irAMainActivity()
                } catch (e: Exception) {
                    android.util.Log.e("RegisterActivity", "Error guardando en Firestore: ${e.message}", e)
                    Toast.makeText(this@RegisterActivity, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Crear cuenta"
                }
            } catch (e: Exception) {
                android.util.Log.e("RegisterActivity", "Error general: ${e.message}", e)
                Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Crear cuenta"
            }
        }
    }

    private fun irAMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun observeViewModel() {
        // Importante: StateFlow se debe recolectar dentro de una corrutina
        lifecycleScope.launch {
            viewModel.registroState.collect { state ->
                when (state) {
                    is RegistroState.Cargando -> {
                        binding.btnRegister.isEnabled = false
                        binding.btnRegister.text = "Cargando..."
                    }
                    is RegistroState.Exito -> {
                        Toast.makeText(this@RegisterActivity, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    is RegistroState.Error -> {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "Crear cuenta"
                        Toast.makeText(this@RegisterActivity, state.mensaje, Toast.LENGTH_LONG).show()
                    }
                    is RegistroState.Idle -> {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "Crear cuenta"
                    }
                }
            }
        }
    }
}