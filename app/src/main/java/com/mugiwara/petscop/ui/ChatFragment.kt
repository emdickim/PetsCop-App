package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Message
import com.mugiwara.petscop.ui.adapter.MessageAdapter

class ChatFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    
    private var chatId: String = ""
    private var doctorNombre: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
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

        chatId = arguments?.getString("chatId") ?: "chat_generico"
        doctorNombre = arguments?.getString("nombre") ?: "Veterinario"

        val tvTitulo = view.findViewById<TextView>(R.id.tvNombreChatActivo)
        tvTitulo.text = doctorNombre

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val rvMessages = view.findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = view.findViewById<EditText>(R.id.etMessage)
        val btnSend = view.findViewById<ImageButton>(R.id.btnSend)

        adapter = MessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        val currentUserId = auth.currentUser?.uid

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                messages.clear()
                snapshot?.let {
                    for (doc in it) {
                        val message = doc.toObject(Message::class.java)
                        // Marcamos si el mensaje es mío comparando los IDs
                        val updatedMessage = message.copy(isMe = message.senderId == currentUserId)
                        messages.add(updatedMessage)
                    }
                    adapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        rvMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            val userId = auth.currentUser?.uid ?: "anonimo"
            if (text.isNotEmpty()) {
                val newMessage = hashMapOf(
                    "senderId" to userId,
                    "text" to text,
                    "timestamp" to Timestamp.now()
                )
                
                db.collection("chats").document(chatId).collection("messages").add(newMessage)
                etMessage.text.clear()
            }
        }
    }
}