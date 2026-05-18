package com.mugiwara.petscop.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.R
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.CartAdapter
import kotlinx.coroutines.launch

class CarritoFragment : Fragment() {

    private lateinit var rvCarrito: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvEnvio: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvVacio: TextView
    private lateinit var btnCheckout: MaterialButton
    private lateinit var cartContainer: LinearLayout
    private lateinit var cartManager: CartManager
    private lateinit var cartAdapter: CartAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var apiService: PetscopApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_carrito, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCarrito = view.findViewById(R.id.rvCarrito)
        tvSubtotal = view.findViewById(R.id.tvSubtotal)
        tvEnvio = view.findViewById(R.id.tvEnvio)
        tvTotal = view.findViewById(R.id.tvTotal)
        tvVacio = view.findViewById(R.id.tvVacio)
        btnCheckout = view.findViewById(R.id.btnCheckout)
        cartContainer = view.findViewById(R.id.cartContainer)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        cartManager = CartManager(requireContext().getSharedPreferences("carrito", 0))
        apiService = PetscopApiService.create()

        rvCarrito.layoutManager = LinearLayoutManager(requireContext())

        cargarCarrito()

        btnCheckout.setOnClickListener {
            mostrarDialogoCheckout()
        }
    }

    private fun cargarCarrito() {
        val items = cartManager.obtenerCarrito().toMutableList()

        if (items.isEmpty()) {
            tvVacio.visibility = View.VISIBLE
            cartContainer.visibility = View.GONE
        } else {
            tvVacio.visibility = View.GONE
            cartContainer.visibility = View.VISIBLE

            cartAdapter = CartAdapter(items, { item, cantidad ->
                cartManager.actualizarCantidad(item.id_producto, cantidad)
                actualizarTotales()
            }, { item ->
                cartManager.eliminarProducto(item.id_producto)
                cargarCarrito()
            })

            rvCarrito.adapter = cartAdapter
            actualizarTotales()
        }
    }

    private fun actualizarTotales() {
        val subtotal = cartManager.obtenerTotal()
        val envio = if (subtotal >= 25.0) 0.0 else 5.0
        val total = subtotal + envio

        tvSubtotal.text = "€${String.format("%.2f", subtotal)}"
        tvEnvio.text = if (envio == 0.0) "Gratis" else "€${String.format("%.2f", envio)}"
        tvTotal.text = "€${String.format("%.2f", total)}"
    }

    private fun mostrarDialogoCheckout() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Finalizar compra")

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val etNombre = EditText(requireContext()).apply {
            hint = "Nombre completo"
            layout.addView(this)
        }

        val etIban = EditText(requireContext()).apply {
            hint = "IBAN (ES00 0000...)"
            layout.addView(this)
        }

        val etDireccion = EditText(requireContext()).apply {
            hint = "Dirección de envío"
            layout.addView(this)
        }

        builder.setView(layout)
        builder.setPositiveButton("Confirmar compra") { _, _ ->
            val nombre = etNombre.text.toString()
            val iban = etIban.text.toString()
            val direccion = etDireccion.text.toString()

            if (nombre.isNotEmpty() && iban.isNotEmpty() && direccion.isNotEmpty()) {
                crearPedido(nombre, iban, direccion)
            } else {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun crearPedido(nombre: String, iban: String, direccion: String) {
        val email = auth.currentUser?.email ?: return
        val items = cartManager.obtenerCarrito()
        val total = cartManager.obtenerTotal()

        val pedidoData = mapOf(
            "items" to items.map { mapOf(
                "id_producto" to it.id_producto,
                "nombre" to it.nombre,
                "cantidad" to it.cantidad,
                "precio" to it.precio
            )},
            "total" to total,
            "nombre_titular" to nombre,
            "iban" to iban,
            "direccion" to direccion,
            "estado" to "pendiente"
        )

        lifecycleScope.launch {
            try {
                val response = apiService.crearPedido(pedidoData, email)
                Toast.makeText(requireContext(), "Pedido creado exitosamente", Toast.LENGTH_SHORT).show()
                cartManager.vaciarCarrito()
                cargarCarrito()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al crear pedido: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
