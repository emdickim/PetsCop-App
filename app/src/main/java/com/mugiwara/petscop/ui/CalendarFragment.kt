package com.mugiwara.petscop.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Cita
import com.mugiwara.petscop.model.Mascota
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.CitaAdapter
import com.mugiwara.petscop.ui.adapter.CitaVeterinarioAdapter
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var apiService: PetscopApiService
    private lateinit var citaAdapter: CitaAdapter
    private lateinit var citaVetAdapter: CitaVeterinarioAdapter
    private lateinit var rvCitas: RecyclerView
    private lateinit var btnNuevaCita: Button
    
    private val citas = mutableListOf<Cita>()
    private var mascotas = listOf<Mascota>()
    private var veterinarios = listOf<com.mugiwara.petscop.model.UserResponse>()
    private var horas = listOf<String>()
    private var esVeterinario = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
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
        apiService = PetscopApiService.create()

        rvCitas = view.findViewById<RecyclerView>(R.id.rvCitas)
        btnNuevaCita = view.findViewById<Button>(R.id.btnNuevaCita)
        
        rvCitas.layoutManager = LinearLayoutManager(requireContext())

        // Generar lista de horas (09:00 a 18:00)
        horas = (9..17).map { String.format("%02d:00", it) }

        // Detectar si es veterinario o cliente
        detectarRol()
    }

    private fun detectarRol() {
        val firebaseUid = auth.currentUser?.uid ?: return
        
        db.collection("users").document(firebaseUid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    esVeterinario = document.getBoolean("esVeterinario") ?: false
                    
                    if (esVeterinario) {
                        configurarVistaVeterinario()
                    } else {
                        configurarVistaCliente()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al detectar rol", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarVistaCliente() {
        btnNuevaCita.visibility = View.VISIBLE
        btnNuevaCita.setOnClickListener {
            mostrarDialogoNuevaCita()
        }
        
        citaAdapter = CitaAdapter(citas)
        rvCitas.adapter = citaAdapter
        
        cargarCitasCliente()
    }

    private fun configurarVistaVeterinario() {
        btnNuevaCita.visibility = View.GONE
        
        citaVetAdapter = CitaVeterinarioAdapter(
            citas,
            onAceptar = { cita -> aceptarCita(cita) },
            onRechazar = { cita -> rechazarCita(cita) }
        )
        rvCitas.adapter = citaVetAdapter
        
        cargarCitasVeterinario()
    }

    private fun cargarCitasCliente() {
        val email = auth.currentUser?.email ?: return
        
        lifecycleScope.launch {
            try {
                val citasAPI = apiService.getCitasPorCliente(email)
                citas.clear()
                citas.addAll(citasAPI)
                citaAdapter.updateList(citas)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar citas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarCitasVeterinario() {
        val email = auth.currentUser?.email ?: return
        
        lifecycleScope.launch {
            try {
                val citasAPI = apiService.getCitasPorVeterinario(email)
                citas.clear()
                citas.addAll(citasAPI)
                citaVetAdapter.updateList(citas)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar citas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun aceptarCita(cita: Cita) {
        actualizarEstadoCita(cita.id_cita, "confirmada")
    }

    private fun rechazarCita(cita: Cita) {
        actualizarEstadoCita(cita.id_cita, "cancelada")
    }

    private fun actualizarEstadoCita(idCita: Int, nuevoEstado: String) {
        val email = auth.currentUser?.email ?: return
        
        lifecycleScope.launch {
            try {
                val datos = mapOf("estado" to nuevoEstado)
                apiService.actualizarEstadoCita(idCita, datos, email)
                
                val mensaje = if (nuevoEstado == "confirmada") "Cita aceptada" else "Cita rechazada"
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                
                // Recargar citas
                cargarCitasVeterinario()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al actualizar cita: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoNuevaCita() {
        val email = auth.currentUser?.email ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_nueva_cita, null)

        val spinnerMascota = dialogView.findViewById<Spinner>(R.id.spinnerMascota)
        val spinnerVeterinario = dialogView.findViewById<Spinner>(R.id.spinnerVeterinario)
        val etMotivo = dialogView.findViewById<EditText>(R.id.etMotivo)
        val etFecha = dialogView.findViewById<EditText>(R.id.etFecha)
        val spinnerHora = dialogView.findViewById<Spinner>(R.id.spinnerHora)
        val btnSolicitarCita = dialogView.findViewById<Button>(R.id.btnSolicitarCita)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        lifecycleScope.launch {
            try {
                mascotas = apiService.getMascotasPorCliente(email)
                val adapterMascotas = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    mascotas.map { it.nombre }
                )
                adapterMascotas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerMascota.adapter = adapterMascotas

                veterinarios = apiService.getVeterinariosPorCliente(email)
                
                if (veterinarios.isEmpty()) {
                    Toast.makeText(requireContext(), "No hay veterinarios disponibles en tu zona", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@launch
                }
                
                val adapterVeterinarios = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    veterinarios.map { it.nombre }
                )
                adapterVeterinarios.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerVeterinario.adapter = adapterVeterinarios

                android.util.Log.d("CalendarFragment", "Mascotas cargadas: ${mascotas.size}, Veterinarios: ${veterinarios.size}")

            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("CalendarFragment", "Error cargando datos: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@launch
            }
        }

        etFecha.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    etFecha.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val adapterHoras = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            horas
        )
        adapterHoras.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHora.adapter = adapterHoras

        btnSolicitarCita.setOnClickListener {
            val mascotaIndex = spinnerMascota.selectedItemPosition
            val veterinarioIndex = spinnerVeterinario.selectedItemPosition
            val motivo = etMotivo.text.toString().trim()
            val fecha = etFecha.text.toString().trim()
            val hora = spinnerHora.selectedItem.toString()

            if (mascotaIndex < 0 || mascotaIndex >= mascotas.size) {
                Toast.makeText(requireContext(), "Selecciona una mascota", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (veterinarioIndex < 0 || veterinarioIndex >= veterinarios.size) {
                Toast.makeText(requireContext(), "Selecciona un veterinario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (motivo.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa el motivo de la consulta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fecha.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona una fecha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idMascota = mascotas[mascotaIndex].id_mascota
            val idVeterinario = veterinarios[veterinarioIndex].id

            val citaMap = mapOf(
                "id_mascota" to idMascota,
                "id_veterinario" to idVeterinario,  // Es String, como viene del API
                "motivo" to motivo,
                "fecha" to fecha,  // Formato: dd/MM/yyyy
                "hora" to hora
            )

            android.util.Log.d("CalendarFragment", "Enviando cita: $citaMap")

            lifecycleScope.launch {
                try {
                    val response = apiService.crearCita(citaMap, email)
                    android.util.Log.d("CalendarFragment", "Respuesta API: $response")
                    Toast.makeText(requireContext(), "Cita solicitada exitosamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    cargarCitasCliente()
                } catch (e: HttpException) {
                    val code = e.code()
                    val body = e.response()?.errorBody()?.string() ?: "Sin detalles"
                    android.util.Log.e("CalendarFragment", "Error HTTP $code: $body", e)
                    Toast.makeText(
                        requireContext(),
                        "Error $code del servidor: $body",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.e("CalendarFragment", "Error: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        dialog.show()
    }
}