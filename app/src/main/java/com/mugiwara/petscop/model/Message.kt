package com.mugiwara.petscop.model

import com.google.firebase.Timestamp

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val isMe: Boolean = false
)