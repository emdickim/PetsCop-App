package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.R
import com.mugiwara.petscop.model.Producto
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.ProductAdapter
import kotlinx.coroutines.launch

class MarketplaceFragment : Fragment() {

    private lateinit var rvProductos: RecyclerView
    private lateinit var tvSinProductos: android.widget.TextView
    private lateinit var loadingContainer: LinearLayout
    private lateinit var apiService: PetscopApiService
    private var idClinica: Int = 0
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var productAdapter: ProductAdapter
    private lateinit var cartManager: CartManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_store, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvProductos = view.findViewById(R.id.rvProductos)
        tvSinProductos = view.findViewById(R.id.tvSinProductos)
        loadingContainer = view.findViewById(R.id.loadingContainer)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        apiService = PetscopApiService.create()

        // Configurar RecyclerView
        rvProductos.layoutManager = GridLayoutManager(requireContext(), 2)

        // Cargar clínica del usuario
        cargarClinicaUsuario()
    }

    private fun cargarClinicaUsuario() {
        loadingContainer.visibility = View.VISIBLE

        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    idClinica = document.getLong("id_clinica")?.toInt() ?: 0
                    if (idClinica > 0) {
                        cargarProductos(idClinica)
                    } else {
                        loadingContainer.visibility = View.GONE
                        tvSinProductos.visibility = View.VISIBLE
                        tvSinProductos.text = "Selecciona una clínica en Configuración"
                    }
                }
                .addOnFailureListener {
                    loadingContainer.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error al cargar clínica", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cargarProductos(idClinica: Int) {
        val email = auth.currentUser?.email ?: return

        lifecycleScope.launch {
            try {
                loadingContainer.visibility = View.GONE
                val productos = apiService.getProductosPorClinica(idClinica, email)
                
                if (productos.isEmpty()) {
                    tvSinProductos.visibility = View.VISIBLE
                    tvSinProductos.text = "No hay productos disponibles"
                } else {
                    tvSinProductos.visibility = View.GONE
                    productAdapter = ProductAdapter(productos) { producto ->
                        mostrarDialogoCompra(producto)
                    }
                    rvProductos.adapter = productAdapter
                }
            } catch (e: Exception) {
                loadingContainer.visibility = View.GONE
                tvSinProductos.visibility = View.VISIBLE
                tvSinProductos.text = "Error al cargar productos: ${e.message}"
            }
        }
    }

    private fun mostrarDialogoCompra(producto: Producto) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle(producto.nombre)
        builder.setMessage("Precio: €${String.format("%.2f", producto.precio)}\nStock: ${producto.stock}")
        builder.setPositiveButton("Agregar al carrito") { _, _ ->
            cartManager = CartManager(requireContext().getSharedPreferences("carrito", 0))
            cartManager.agregarAlCarrito(producto, 1)
            Toast.makeText(requireContext(), "Agregado al carrito", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
}