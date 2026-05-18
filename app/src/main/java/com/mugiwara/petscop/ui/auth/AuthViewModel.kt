package com.mugiwara.petscop.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _registroState = MutableStateFlow<RegistroState>(RegistroState.Idle)
    val registroState: StateFlow<RegistroState> = _registroState.asStateFlow()

    fun registrarUsuario(email: String, password: String) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Cargando
            try {
                // 1. Ejecutamos el registro y esperamos el resultado
                val result = auth.createUserWithEmailAndPassword(email, password).await()

                //2. Obetenemos el usuario del resultado
                val firebaseUser = result.user

                //3. Enviamos el usuario a la activity a través del estado Exito
                _registroState.value = RegistroState.Exito(firebaseUser)

            }   catch (e: Exception) {
                _registroState.value = RegistroState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class RegistroState {
    data object Idle : RegistroState()
    data object Cargando : RegistroState()
    data class Exito(val user: FirebaseUser?) : RegistroState()
    data class Error(val mensaje: String) : RegistroState()
}