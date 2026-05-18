package com.mugiwara.petscop.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.mugiwara.petscop.R
import com.mugiwara.petscop.databinding.FragmentVentasBinding
import com.mugiwara.petscop.model.Pedido
import com.mugiwara.petscop.model.Producto
import com.mugiwara.petscop.network.PetscopApiService
import com.mugiwara.petscop.ui.adapter.PedidoAdapter
import com.mugiwara.petscop.ui.adapter.ProductoVentaAdapter
import kotlinx.coroutines.launch

class VentasFragment : Fragment() {

    private var _binding: FragmentVentasBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiService: PetscopApiService
    private lateinit var auth: FirebaseAuth
    private lateinit var rvProductos: RecyclerView
    private lateinit var rvPedidos: RecyclerView
    private lateinit var tvSinProductos: android.widget.TextView
    private lateinit var tvSinPedidos: android.widget.TextView
    private lateinit var loadingContainer: LinearLayout

    private val productos = mutableListOf<Producto>()
    private val pedidos = mutableListOf<Pedido>()
    private var productoAdapter: ProductoVentaAdapter? = null
    private var pedidoAdapter: PedidoAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVentasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        apiService = PetscopApiService.create()
        auth = FirebaseAuth.getInstance()

        rvProductos = binding.rvProductos
        rvPedidos = binding.rvPedidos
        tvSinProductos = binding.tvSinProductos
        tvSinPedidos = binding.tvSinPedidos
        loadingContainer = binding.loadingContainer

        // Configurar RecyclerViews
        rvProductos.layoutManager = LinearLayoutManager(requireContext())
        rvPedidos.layoutManager = LinearLayoutManager(requireContext())

        // Configurar botones de tab
        val tabProductos = binding.tabProductos
        val tabPedidos = binding.tabPedidos
        val tabProductosContent = binding.tabProductosContent
        val tabPedidosContent = binding.tabPedidosContent

        tabProductos.setOnClickListener {
            tabProductosContent.visibility = View.VISIBLE
            tabPedidosContent.visibility = View.GONE
            tabProductos.isChecked = true
            tabPedidos.isChecked = false
        }

        tabPedidos.setOnClickListener {
            tabProductosContent.visibility = View.GONE
            tabPedidosContent.visibility = View.VISIBLE
            tabProductos.isChecked = false
            tabPedidos.isChecked = true
        }

        // Botón nuevo producto
        binding.btnNuevoProducto.setOnClickListener {
            mostrarDialogoNuevoProducto()
        }

        // Cargar datos
        cargarProductos()
        cargarPedidos()
    }

    private fun cargarProductos() {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: No hay usuario logueado", Toast.LENGTH_SHORT).show()
            return
        }

        loadingContainer.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val productosObtenidos = apiService.getProductosPorVeterinario(email)
                productos.clear()
                productos.addAll(productosObtenidos)

                if (productos.isEmpty()) {
                    tvSinProductos.visibility = View.VISIBLE
                    rvProductos.visibility = View.GONE
                } else {
                    tvSinProductos.visibility = View.GONE
                    rvProductos.visibility = View.VISIBLE
                    productoAdapter = ProductoVentaAdapter(productos, { producto ->
                        mostrarDialogoEditarProducto(producto)
                    }, { idProducto ->
                        mostrarDialogoEliminarProducto(idProducto)
                    })
                    rvProductos.adapter = productoAdapter
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT).show()
                tvSinProductos.visibility = View.VISIBLE
                tvSinProductos.text = "Error al cargar productos"
            } finally {
                loadingContainer.visibility = View.GONE
            }
        }
    }

    private fun cargarPedidos() {
        val email = auth.currentUser?.email
        if (email.isNullOrEmpty()) {
            return
        }

        lifecycleScope.launch {
            try {
                val pedidosObtenidos = apiService.getPedidosPorVeterinario(email)
                pedidos.clear()
                pedidos.addAll(pedidosObtenidos)

                if (pedidos.isEmpty()) {
                    tvSinPedidos.visibility = View.VISIBLE
                    rvPedidos.visibility = View.GONE
                } else {
                    tvSinPedidos.visibility = View.GONE
                    rvPedidos.visibility = View.VISIBLE
                    pedidoAdapter = PedidoAdapter(pedidos) { pedido ->
                        mostrarDialogoCambiarEstado(pedido)
                    }
                    rvPedidos.adapter = pedidoAdapter
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tvSinPedidos.visibility = View.VISIBLE
                tvSinPedidos.text = "Error al cargar pedidos"
            }
        }
    }

    private fun mostrarDialogoNuevoProducto() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nuevo Producto")

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val etNombre = EditText(requireContext()).apply {
            hint = "Nombre"
            layout.addView(this)
        }

        val categorias = arrayOf("Alimentación", "Salud", "Accesorios", "Higiene", "Juguetes")
        val spCategoria = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categorias)
            layout.addView(this)
        }

        val etPrecio = EditText(requireContext()).apply {
            hint = "Precio (€)"
            inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layout.addView(this)
        }

        val etStock = EditText(requireContext()).apply {
            hint = "Stock"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layout.addView(this)
        }

        val etFoto = EditText(requireContext()).apply {
            hint = "URL de la foto"
            layout.addView(this)
        }

        builder.setView(layout)
        builder.setPositiveButton("Crear") { _, _ ->
            val nombre = etNombre.text.toString()
            val categoria = spCategoria.selectedItem.toString()
            val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val stock = etStock.text.toString().toIntOrNull() ?: 0
            val foto = etFoto.text.toString()

            if (nombre.isNotEmpty()) {
                crearProducto(nombre, categoria, precio, stock, foto)
            } else {
                Toast.makeText(requireContext(), "Completa los campos", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogoEditarProducto(producto: Producto) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Editar Producto")

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val etNombre = EditText(requireContext()).apply {
            setText(producto.nombre)
            layout.addView(this)
        }

        val categorias = arrayOf("Alimentación", "Salud", "Accesorios", "Higiene", "Juguetes")
        val spCategoria = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categorias)
            setSelection(categorias.indexOf(producto.categoria))
            layout.addView(this)
        }

        val etPrecio = EditText(requireContext()).apply {
            setText(producto.precio.toString())
            inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layout.addView(this)
        }

        val etStock = EditText(requireContext()).apply {
            setText(producto.stock.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layout.addView(this)
        }

        val etFoto = EditText(requireContext()).apply {
            setText(producto.foto_url ?: "")
            layout.addView(this)
        }

        builder.setView(layout)
        builder.setPositiveButton("Guardar") { _, _ ->
            val nombre = etNombre.text.toString()
            val categoria = spCategoria.selectedItem.toString()
            val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val stock = etStock.text.toString().toIntOrNull() ?: 0
            val foto = etFoto.text.toString()

            if (nombre.isNotEmpty()) {
                actualizarProducto(producto.id_producto, nombre, categoria, precio, stock, foto)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogoEliminarProducto(idProducto: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que deseas eliminar este producto?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarProducto(idProducto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCambiarEstado(pedido: Pedido) {
        val estados = arrayOf("pendiente", "confirmado", "enviado", "entregado")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cambiar Estado")
        builder.setSingleChoiceItems(estados, estados.indexOf(pedido.estado)) { _, which ->
            cambiarEstadoPedido(pedido.id_pedido, estados[which])
        }
        builder.show()
    }

    private fun crearProducto(nombre: String, categoria: String, precio: Double, stock: Int, foto: String) {
        val email = auth.currentUser?.email ?: return

        val productoData = mapOf(
            "nombre" to nombre,
            "categoria" to categoria,
            "precio" to precio,
            "stock" to stock,
            "foto_url" to foto
        )

        lifecycleScope.launch {
            try {
                apiService.crearProducto(productoData, email)
                Toast.makeText(requireContext(), "Producto creado", Toast.LENGTH_SHORT).show()
                cargarProductos()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al crear: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarProducto(id: Int, nombre: String, categoria: String, precio: Double, stock: Int, foto: String) {
        val email = auth.currentUser?.email ?: return

        val productoData = mapOf(
            "nombre" to nombre,
            "categoria" to categoria,
            "precio" to precio,
            "stock" to stock,
            "foto_url" to foto
        )

        lifecycleScope.launch {
            try {
                apiService.actualizarProducto(id, productoData, email)
                Toast.makeText(requireContext(), "Producto actualizado", Toast.LENGTH_SHORT).show()
                cargarProductos()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarProducto(id: Int) {
        val email = auth.currentUser?.email ?: return

        lifecycleScope.launch {
            try {
                apiService.eliminarProducto(id, email)
                Toast.makeText(requireContext(), "Producto eliminado", Toast.LENGTH_SHORT).show()
                cargarProductos()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cambiarEstadoPedido(idPedido: Int, nuevoEstado: String) {
        val email = auth.currentUser?.email ?: return

        lifecycleScope.launch {
            try {
                apiService.actualizarEstadoPedido(idPedido, mapOf("estado" to nuevoEstado), email)
                Toast.makeText(requireContext(), "Estado actualizado", Toast.LENGTH_SHORT).show()
                cargarPedidos()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
