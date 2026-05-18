package com.mugiwara.petscop.ui.auth

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.MainActivity
import com.mugiwara.petscop.R
import com.mugiwara.petscop.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var esVeterinario = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        setupRoleSelection()

        // 1. Lógica del botón Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                loginConValidacionDeRol(email, pass)
            } else {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Ir a Registro
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginConValidacionDeRol(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Validando..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        // Obtener datos del usuario en Firestore
                        db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val esVeterinarioEnBD = document.getBoolean("esVeterinario") ?: false
                                    
                                    // Validar que el rol seleccionado coincide con la BD
                                    if (esVeterinarioEnBD == esVeterinario) {
                                        // ✅ Los roles coinciden, permitir login
                                        irAMain()
                                    } else {
                                        // ❌ Los roles NO coinciden
                                        val rolEsperado = if (esVeterinarioEnBD) "Veterinario" else "Cliente"
                                        Toast.makeText(
                                            this,
                                            "Rol incorrecto. Esta cuenta es de $rolEsperado",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        auth.signOut()
                                        binding.btnLogin.isEnabled = true
                                        binding.btnLogin.text = "Iniciar Sesión"
                                    }
                                } else {
                                    // Documento no existe en Firestore
                                    Toast.makeText(
                                        this,
                                        "Perfil no completado. Por favor, regístrate nuevamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    auth.signOut()
                                    binding.btnLogin.isEnabled = true
                                    binding.btnLogin.text = "Iniciar Sesión"
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Error al verificar perfil: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                auth.signOut()
                                binding.btnLogin.isEnabled = true
                                binding.btnLogin.text = "Iniciar Sesión"
                            }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Error: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Iniciar Sesión"
                }
            }
    }

    private fun setupRoleSelection() {
        binding.cvCliente.setOnClickListener {
            esVeterinario = false
            actualizarUISelector()
        }

        binding.cvVeterinario.setOnClickListener {
            esVeterinario = true
            actualizarUISelector()
        }
        
        // Inicializar estado visual
        actualizarUISelector()
    }

    private fun actualizarUISelector() {
        val colorActive = ContextCompat.getColor(this, R.color.primary)
        val colorInactive = ContextCompat.getColor(this, R.color.border)
        val bgCard = ContextCompat.getColor(this, R.color.card)

        if (esVeterinario) {
            binding.cvVeterinario.setStrokeColor(ColorStateList.valueOf(colorActive))
            binding.cvVeterinario.strokeWidth = 6
            
            binding.cvCliente.setStrokeColor(ColorStateList.valueOf(colorInactive))
            binding.cvCliente.strokeWidth = 2
        } else {
            binding.cvCliente.setStrokeColor(ColorStateList.valueOf(colorActive))
            binding.cvCliente.strokeWidth = 6
            
            binding.cvVeterinario.setStrokeColor(ColorStateList.valueOf(colorInactive))
            binding.cvVeterinario.strokeWidth = 2
        }
    }

    private fun irAMain() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "Iniciar Sesión"
        Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}