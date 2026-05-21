package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Mascota
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.MascotaAdapter
import kotlinx.coroutines.launch

class MascotasFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var apiService: PetscopApiService
    private lateinit var rvMascotas: RecyclerView
    private lateinit var tvEmptyMascotas: TextView
    private lateinit var btnAgregarMascota: Button
    private val mascotas = mutableListOf<Mascota>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mascotas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        apiService = PetscopApiService.create()

        rvMascotas = view.findViewById(R.id.rvMascotas)
        tvEmptyMascotas = view.findViewById(R.id.tvEmptyMascotas)
        btnAgregarMascota = view.findViewById(R.id.btnAgregarMascota)

        rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        rvMascotas.adapter = MascotaAdapter(mascotas)

        btnAgregarMascota.setOnClickListener {
            mostrarDialogoNuevaMascota()
        }

        cargarMascotas()
    }

    private fun mostrarDialogoNuevaMascota() {
        Log.d("MascotasFragment", "mostrarDialogoNuevaMascota() iniciado")
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_nueva_mascota, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreMascota)
        val etEspecie = dialogView.findViewById<EditText>(R.id.etEspecieMascota)
        val etRaza = dialogView.findViewById<EditText>(R.id.etRazaMascota)
        val etEdad = dialogView.findViewById<EditText>(R.id.etEdadMascota)
        val etPeso = dialogView.findViewById<EditText>(R.id.etPesoMascota)
        val etFechaNacimiento = dialogView.findViewById<EditText>(R.id.etFechaNacimientoMascota)
        val etMicrochip = dialogView.findViewById<EditText>(R.id.etMicrochipMascota)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.titulo_nueva_mascota))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.boton_registrar), DialogInterface.OnClickListener { dialogInterface, i ->
                // Se deja vacío intencionalmente. El listener real se adjunta en setOnShowListener.
            })
            .setNegativeButton(getString(R.string.boton_cancelar)) { _, _ -> 
                Log.d("MascotasFragment", "Botón Cancelar clickeado")
            }
            .create()

        dialog.setOnShowListener {
            Log.d("MascotasFragment", "setOnShowListener disparado")
            
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            Log.d("MascotasFragment", "positiveButton obtenido: $positiveButton")
            
            positiveButton.setOnClickListener {
                Log.d("MascotasFragment", "Botón Registrar clickeado")
                
                val nombre = etNombre.text.toString().trim()
                val especie = etEspecie.text.toString().trim()
                val raza = etRaza.text.toString().trim()
                val edadText = etEdad.text.toString().trim()
                val pesoText = etPeso.text.toString().trim()
                val fechaNacimiento = etFechaNacimiento.text.toString().trim()
                val microchip = etMicrochip.text.toString().trim()

                Log.d("MascotasFragment", "Campos leídos - nombre: $nombre, especie: $especie, raza: $raza, edad: $edadText")

                if (nombre.isEmpty() || especie.isEmpty() || raza.isEmpty() || edadText.isEmpty()) {
                    Log.d("MascotasFragment", "Validación fallida: campos vacíos")
                    Toast.makeText(requireContext(), getString(R.string.mensaje_completa_campos), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val edad = edadText.toIntOrNull()
                if (edad == null) {
                    Log.d("MascotasFragment", "Validación fallida: edad no es válida")
                    Toast.makeText(requireContext(), getString(R.string.mensaje_edad_valida), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                Log.d("MascotasFragment", "Validación exitosa. Llamando crearMascota()")
                crearMascota(nombre, especie, raza, edad, pesoText, fechaNacimiento, microchip)
                dialog.dismiss()
            }
        }

        Log.d("MascotasFragment", "Mostrando diálogo")
        dialog.show()

        // Ensure the first field gets focus and the keyboard is shown so
        // the InputConnection remains active while the user types.
        etNombre.post {
            etNombre.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etNombre, InputMethodManager.SHOW_IMPLICIT)
        }

        // Hide keyboard cleanly when dialog is dismissed
        dialog.setOnDismissListener {
            try {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etNombre.windowToken, 0)
            } catch (_: Exception) {
            }
        }
    }

    private fun crearMascota(nombre: String, especie: String, raza: String, edad: Int, pesoText: String, fechaNacimiento: String, microchip: String) {
        Log.d("MascotasFragment", "crearMascota() iniciado con: nombre=$nombre, especie=$especie, raza=$raza, edad=$edad")
        
        val email = auth.currentUser?.email
        val firebaseUid = auth.currentUser?.uid
        Log.d("MascotasFragment", "Email del usuario: $email, Firebase UID: $firebaseUid")
        
        if (email.isNullOrEmpty() || firebaseUid.isNullOrEmpty()) {
            Log.d("MascotasFragment", "Email o Firebase UID es nulo o vacío")
            Toast.makeText(requireContext(), "No se ha encontrado el usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val peso = if (pesoText.isNotEmpty()) pesoText.toDoubleOrNull() else null
        Log.d("MascotasFragment", "Peso: $peso, FechaNacimiento: $fechaNacimiento, Microchip: $microchip")
        
        // Crear JsonObject para la API
        val mascotaJson = JsonObject().apply {
            addProperty("firebase_uid", firebaseUid)
            addProperty("nombre", nombre)
            addProperty("especie", especie)
            addProperty("raza", raza)
            addProperty("edad", edad)
            
            if (peso != null) addProperty("peso", peso)
            if (fechaNacimiento.isNotEmpty()) addProperty("fecha_nacimiento", fechaNacimiento)
            if (microchip.isNotEmpty()) addProperty("microchip", microchip)
        }
        
        Log.d("MascotasFragment", "mascotaJson a enviar: $mascotaJson")

        Log.d("MascotasFragment", "Iniciando lifecycleScope.launch para llamada API")
        lifecycleScope.launch {
            try {
                Log.d("MascotasFragment", "Llamando apiService.crearMascota()")
                apiService.crearMascota(mascotaJson, email)
                Log.d("MascotasFragment", "API call completada exitosamente")
                Toast.makeText(requireContext(), getString(R.string.mensaje_mascota_registrada), Toast.LENGTH_SHORT).show()
                cargarMascotas()
            } catch (e: Exception) {
                Log.e("MascotasFragment", "Error en apiService.crearMascota(): ${e.message}", e)
                Toast.makeText(requireContext(), "Error al registrar mascota: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cargarMascotas() {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            tvEmptyMascotas.text = getString(R.string.error_usuario_no_encontrado)
            tvEmptyMascotas.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                val lista = apiService.getMascotasPorCliente(email)
                mascotas.clear()
                for (item in lista) {
                    val id = (item["id"] as? Number)?.toInt()
                        ?: (item["id_mascota"] as? Number)?.toInt() ?: 0
                    val nombre = item["nombre"] as? String ?: ""
                    val especie = item["especie"] as? String ?: ""
                    val raza = item["raza"] as? String ?: ""
                    val edad = (item["edad"] as? Number)?.toInt() ?: 0
                    mascotas.add(Mascota(id_mascota = id, nombre = nombre, especie = especie, raza = raza, edad = edad))
                }
                rvMascotas.adapter = MascotaAdapter(mascotas)
                actualizarVista(mascotas)
            } catch (e: Exception) {
                tvEmptyMascotas.text = getString(R.string.error_cargar_mascotas, e.message ?: "")
                tvEmptyMascotas.visibility = View.VISIBLE
            }
        }
    }

    private fun actualizarVista(lista: List<Mascota>) {
        if (lista.isEmpty()) {
            tvEmptyMascotas.visibility = View.VISIBLE
            rvMascotas.visibility = View.GONE
        } else {
            tvEmptyMascotas.visibility = View.GONE
            rvMascotas.visibility = View.VISIBLE
        }
    }
}
