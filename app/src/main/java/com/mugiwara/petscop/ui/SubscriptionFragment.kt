package com.mugiwara.petscop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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
import com.mugiwara.petscop.ui.adapter.BillingAdapter
import kotlinx.coroutines.launch

data class Plan(
    val nombre: String,
    val precio: Double,
    val descripcion: String,
    val caracteristicas: List<String>
)

data class Factura(
    val fecha: String,
    val plan: String,
    val importe: Double,
    val estado: String
)

class SubscriptionFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvPlanActual: TextView
    private lateinit var tvFechaRenovacion: TextView
    private lateinit var plansContainer: LinearLayout
    private lateinit var rvBilling: RecyclerView
    
    private val planes = listOf(
        Plan(
            "Básico",
            9.99,
            "Perfecto para empezar",
            listOf("Hasta 10 citas/mes", "Panel de control básico", "Chat con clientes", "Estadísticas básicas")
        ),
        Plan(
            "Pro",
            19.99,
            "Para profesionales",
            listOf("Hasta 50 citas/mes", "Panel avanzado", "Chat y notificaciones", "Reportes detallados", "Integración SMS")
        ),
        Plan(
            "Premium",
            39.99,
            "Todo lo que necesitas",
            listOf("Citas ilimitadas", "Panel completo", "Chat y teléfono", "Reportes customizados", "API acceso", "Soporte prioritario")
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscription, container, false)
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

        tvPlanActual = view.findViewById(R.id.tvPlanActual)
        tvFechaRenovacion = view.findViewById(R.id.tvFechaRenovacion)
        plansContainer = view.findViewById(R.id.plansContainer)
        rvBilling = view.findViewById(R.id.rvBilling)

        rvBilling.layoutManager = LinearLayoutManager(requireContext())

        cargarPlanActual()
        generarPlanes()
        cargarFacturas()
    }

    private fun cargarPlanActual() {
        val firebaseUid = auth.currentUser?.uid ?: return

        db.collection("users").document(firebaseUid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val planActual = document.getString("plan") ?: "Básico"
                    val fechaRenovacion = document.getString("fecha_renovacion_plan") ?: "17/04/2026"

                    tvPlanActual.text = planActual
                    tvFechaRenovacion.text = "Renueva el $fechaRenovacion"
                }
            }
    }

    private fun generarPlanes() {
        plansContainer.removeAllViews()

        for (plan in planes) {
            val planCard = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_plan, plansContainer, false) as ViewGroup

            val tvPlanNombre = planCard.findViewById<TextView>(R.id.tvPlanNombre)
            val tvPlanPrecio = planCard.findViewById<TextView>(R.id.tvPlanPrecio)
            val tvPlanDescripcion = planCard.findViewById<TextView>(R.id.tvPlanDescripcion)
            val planeFeaturesList = planCard.findViewById<LinearLayout>(R.id.planeFeaturesList)
            val btnContratar = planCard.findViewById<Button>(R.id.btnContratar)

            tvPlanNombre.text = plan.nombre
            tvPlanPrecio.text = "€${String.format("%.2f", plan.precio)}/mes"
            tvPlanDescripcion.text = plan.descripcion

            // Agregar características
            planeFeaturesList.removeAllViews()
            for (caracteristica in plan.caracteristicas) {
                val tvCaracteristica = TextView(requireContext()).apply {
                    text = "✓ $caracteristica"
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                    setTextColor(requireContext().getColor(android.R.color.darker_gray))
                }
                planeFeaturesList.addView(tvCaracteristica)
            }

            btnContratar.text = "Contratar"
            btnContratar.setOnClickListener {
                mostrarDialogoConfirmacion(plan)
            }

            plansContainer.addView(planCard)
        }
    }

    private fun mostrarDialogoConfirmacion(plan: Plan) {
        val mensaje = "¿Deseas contratar el plan ${plan.nombre} por €${String.format("%.2f", plan.precio)}/mes?"

        AlertDialog.Builder(requireContext())
            .setTitle("Contratar Plan")
            .setMessage(mensaje)
            .setPositiveButton("Contratar") { _, _ ->
                contratarPlan(plan)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun contratarPlan(plan: Plan) {
        val firebaseUid = auth.currentUser?.uid ?: return

        val datosActualizar = mapOf(
            "plan" to plan.nombre,
            "precio_plan" to plan.precio,
            "fecha_renovacion_plan" to obtenerFechaRenovacion()
        )

        db.collection("users").document(firebaseUid)
            .update(datosActualizar)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Plan ${plan.nombre} contratado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                cargarPlanActual()
                cargarFacturas()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al contratar plan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun obtenerFechaRenovacion(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MONTH, 1)
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun cargarFacturas() {
        val firebaseUid = auth.currentUser?.uid ?: return

        db.collection("users").document(firebaseUid)
            .collection("facturas")
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val facturas = mutableListOf<Factura>()
                for (document in result) {
                    val factura = Factura(
                        fecha = document.getString("fecha") ?: "",
                        plan = document.getString("plan") ?: "",
                        importe = document.getDouble("importe") ?: 0.0,
                        estado = document.getString("estado") ?: "Pendiente"
                    )
                    facturas.add(factura)
                }
                rvBilling.adapter = BillingAdapter(facturas)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al cargar facturas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
