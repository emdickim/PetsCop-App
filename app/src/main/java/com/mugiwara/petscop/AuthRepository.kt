package com.mugiwara.petscop.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun logout() { auth.signOut() }

    // El ID que necesitas para el chat
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "anonimo"
    }
}
