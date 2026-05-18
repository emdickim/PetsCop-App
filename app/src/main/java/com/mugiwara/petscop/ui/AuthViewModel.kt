package com.mugiwara.petscop.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    // Usamos StateFlow para que sea fácil de observar desde tu Activity con XML
    private val _registroState = MutableStateFlow<RegistroState>(RegistroState.Idle)
    val registroState: StateFlow<RegistroState> = _registroState.asStateFlow()

    fun registrarUsuario(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _registroState.value = RegistroState.Error("Email y contraseña obligatorios")
            return
        }

        _registroState.value = RegistroState.Cargando

        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _registroState.value = RegistroState.Exito
            } catch (e: Exception) {
                _registroState.value = RegistroState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class RegistroState {
    data object Idle : RegistroState()
    data object Cargando : RegistroState()
    data object Exito : RegistroState()
    data class Error(val mensaje: String) : RegistroState()
}