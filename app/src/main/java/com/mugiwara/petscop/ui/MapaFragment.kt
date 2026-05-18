package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Clinica
import com.mugiwara.petscop.network.PetscopApiService
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapaFragment : Fragment(R.layout.fragment_mapa) {

    private lateinit var map: MapView
    private lateinit var apiService: PetscopApiService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        apiService = PetscopApiService.create()

        // Configuración necesaria para osmdroid
        val prefs = requireContext().getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        Configuration.getInstance().load(requireContext(), prefs)

        map = view.findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(15.0)
        
        // Punto inicial (Barcelona por defecto, basándome en tu SQL)
        val startPoint = GeoPoint(41.3851, 2.1734)
        mapController.setCenter(startPoint)

        // Cargar clínicas desde la API
        cargarClinicas()
    }

    private fun cargarClinicas() {
        lifecycleScope.launch {
            try {
                val clinicas = apiService.getClinicas()
                for (clinica in clinicas) {
                    // Usar coordenadas por defecto si están disponibles en la base de datos
                    // Por ahora usaremos coordenadas aproximadas de Barcelona
                    val lat = 41.3851 + (Math.random() * 0.1 - 0.05)
                    val lon = 2.1734 + (Math.random() * 0.1 - 0.05)
                    
                    añadirMarcador(lat, lon, clinica.nombre, clinica.direccion)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar clínicas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun añadirMarcador(lat: Double, lon: Double, titulo: String, descripcion: String = "") {
        val marker = Marker(map)
        marker.position = GeoPoint(lat, lon)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = titulo
        marker.snippet = descripcion
        map.overlays.add(marker)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
