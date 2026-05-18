package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.R
import com.mugiwara.petscop.databinding.FragmentInicioBinding
import com.mugiwara.petscop.model.Cita
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.CitaAdapter
import kotlinx.coroutines.launch

/**
 * Fragmento de Inicio: Actúa como el panel principal para el usuario.
 * Gestiona la visualización del nombre del usuario, accesos directos a funciones
 * principales y un resumen de las próximas citas médicas.
 */
class InicioFragment : Fragment() {

    // View Binding para acceder a los elementos del layout de forma eficiente y segura
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var apiService: PetscopApiService
    private lateinit var citaAdapter: CitaAdapter
    private val citas = mutableListOf<Cita>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout usando View Binding
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializamos los servicios necesarios (Firebase Auth, Firestore y API de FastAPI)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        apiService = PetscopApiService.create()

        // Configuramos los clics de navegación para las tarjetas superiores
        setupNavigation()

        // Cargamos la información dinámica del usuario y sus citas
        cargarNombreUsuario()
        cargarUltimasCitas()
    }

    /**
     * Configura los listeners de clic para navegar a las distintas secciones de la app
     * utilizando Navigation Component.
     */
    private fun setupNavigation() {
        binding.cardNuevaCita.setOnClickListener {
            findNavController().navigate(R.id.nav_calendar)
        }

        binding.cardChat.setOnClickListener {
            findNavController().navigate(R.id.nav_chat)
        }

        binding.cardMapa.setOnClickListener {
            findNavController().navigate(R.id.nav_mapa)
        }

        binding.cardMarketplace.setOnClickListener {
            findNavController().navigate(R.id.nav_marketplace)
        }
    }

    /**
     * Obtiene el nombre del usuario desde la base de datos Firestore.
     * Si tiene éxito, actualiza el saludo personalizado en el encabezado.
     */
    private fun cargarNombreUsuario() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                // Verificamos que el fragmento siga activo y que el documento del usuario exista
                if (isAdded && _binding != null && document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: "Usuario"
                    // Mostramos el nombre en el encabezado: "¡Hola, [Nombre]!"
                    binding.tvUserGreeting.text = "¡Hola, $nombre!"
                }
            }
            .addOnFailureListener {
                // En caso de error, el XML mantiene su valor por defecto
            }
    }

    /**
     * Recupera la lista completa de citas desde el servidor FastAPI y
     * muestra únicamente las 3 más próximas en el RecyclerView de inicio.
     */
    private fun cargarUltimasCitas() {
        val email = auth.currentUser?.email
        
        if (email.isNullOrEmpty()) {
            return
        }

        // Ejecutamos la llamada de red en una corrutina para no bloquear el hilo principal
        lifecycleScope.launch {
            try {
                // Llamamos a la API de FastAPI para obtener las citas por email del cliente
                val todasLasCitas = apiService.getCitasPorCliente(email)
                
                // Seleccionamos solo las 3 primeras para el resumen rápido
                val ultimasTres = todasLasCitas.take(3)
                
                citas.clear()
                citas.addAll(ultimasTres)
                
                // Actualizamos el RecyclerView si el fragmento sigue cargado
                if (_binding != null) {
                    citaAdapter = CitaAdapter(citas)
                    binding.rvCitasProximas.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvCitasProximas.adapter = citaAdapter
                    
                    // Si no hay citas, mostramos un mensaje informativo
                    binding.tvSinCitas.visibility = if (citas.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                if (isAdded) {
                    // Notificamos al usuario si hubo un problema al sincronizar con el servidor SQL
                    Toast.makeText(requireContext(), "Error al sincronizar citas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Liberamos el binding para evitar pérdidas de memoria (memory leaks)
        _binding = null
    }
}
