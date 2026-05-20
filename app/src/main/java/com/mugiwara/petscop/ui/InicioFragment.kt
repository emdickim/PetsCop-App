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

    // _binding: Se utiliza para acceder a los componentes del XML (fragment_inicio.xml) de forma directa
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    
    // Declaración de variables para Firebase, API y el adaptador de la lista de citas
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var apiService: PetscopApiService
    private lateinit var citaAdapter: CitaAdapter
    private val citas = mutableListOf<Cita>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout usando View Binding para evitar el uso de findViewById
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializamos los servicios de autenticación de Firebase, la base de datos Firestore y la API de FastAPI
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        apiService = PetscopApiService.create()

        // Configuramos qué hace cada tarjeta al ser pulsada (ir a Citas, Chat, Mapa o Tienda)
        setupNavigation()

        // Llamamos a los métodos que cargan la información del usuario y sus citas desde los servidores
        cargarNombreUsuario()
        cargarUltimasCitas()
    }

    /**
     * Configura la navegación: al pulsar en una tarjeta, la app cambia a la pantalla correspondiente.
     */
    private fun setupNavigation() {
        // Al pulsar en 'Nueva Cita', navegamos al fragmento del calendario
        binding.cardNuevaCita.setOnClickListener {
            findNavController().navigate(R.id.nav_calendar)
        }

        // Al pulsar en 'Chat', vamos a la sección de mensajes
        binding.cardChat.setOnClickListener {
            findNavController().navigate(R.id.nav_chat)
        }

        // Al pulsar en 'Mapa', abrimos la localización de clínicas
        binding.cardMapa.setOnClickListener {
            findNavController().navigate(R.id.nav_mapa)
        }

        // Al pulsar en 'Marketplace', vamos a la tienda de productos
        binding.cardMarketplace.setOnClickListener {
            findNavController().navigate(R.id.nav_marketplace)
        }
    }

    /**
     * Busca el nombre del usuario logueado en la base de datos de Firestore.
     * Esto permite personalizar el saludo en la parte superior de la pantalla.
     */
    private fun cargarNombreUsuario() {
        // Obtenemos el ID único del usuario actual
        val uid = auth.currentUser?.uid ?: return

        // Consultamos Firestore para obtener el campo 'nombre' del documento del usuario
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                // Si el documento existe, actualizamos el TextView con el nombre real
                if (isAdded && _binding != null && document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: "Usuario"
                    binding.tvUserGreeting.text = "¡Hola, $nombre!"
                }
            }
            .addOnFailureListener {
                // Si hay un error de conexión, el saludo simplemente se queda como estaba por defecto
            }
    }

    /**
     * Llama a la API de FastAPI para obtener las citas del cliente desde la base de datos SQL.
     * Solo mostramos las 3 citas más próximas para no saturar la pantalla principal.
     */
    private fun cargarUltimasCitas() {
        val email = auth.currentUser?.email
        
        // Verificamos que el usuario tenga un email válido para realizar la consulta
        if (email.isNullOrEmpty()) {
            return
        }

        // Ejecutamos la petición de red dentro de una corrutina para que la app no se congele
        lifecycleScope.launch {
            try {
                // Obtenemos la lista completa de citas desde el servidor FastAPI
                val todasLasCitas = apiService.getCitasPorCliente(email)
                
                // Filtramos la lista para quedarnos solo con las 3 primeras citas
                val ultimasTres = todasLasCitas.take(3)
                
                citas.clear()
                citas.addAll(ultimasTres)
                
                // Si el fragmento sigue cargado, configuramos el RecyclerView para mostrar los datos
                if (_binding != null) {
                    citaAdapter = CitaAdapter(citas)
                    binding.rvCitasProximas.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvCitasProximas.adapter = citaAdapter
                    
                    // Si el usuario no tiene citas, mostramos un mensaje indicándolo
                    binding.tvSinCitas.visibility = if (citas.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                // Si el servidor FastAPI da un error 500 o no hay conexión, avisamos al usuario
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error al sincronizar citas con el servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Liberamos el binding al destruir la vista para evitar problemas de memoria
        _binding = null
    }
}
