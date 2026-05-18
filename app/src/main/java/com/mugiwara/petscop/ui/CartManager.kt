package com.mugiwara.petscop.ui

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mugiwara.petscop.model.Producto

class CartManager(private val sharedPreferences: SharedPreferences) {
    private val gson = Gson()
    private val CART_KEY = "carrito_items"

    fun agregarAlCarrito(producto: Producto, cantidad: Int = 1) {
        val carrito = obtenerCarrito().toMutableList()
        
        val index = carrito.indexOfFirst { it.id_producto == producto.id_producto }
        
        if (index >= 0) {
            // Producto ya existe, sumar cantidad
            val item = carrito[index]
            carrito[index] = item.copy(cantidad = item.cantidad + cantidad)
        } else {
            // Nuevo producto
            carrito.add(CartItem(
                id_producto = producto.id_producto,
                nombre = producto.nombre,
                precio = producto.precio,
                cantidad = cantidad,
                foto_url = producto.foto_url ?: ""
            ))
        }
        
        guardarCarrito(carrito)
    }

    fun obtenerCarrito(): List<CartItem> {
        val json = sharedPreferences.getString(CART_KEY, "[]") ?: "[]"
        val type = object : TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun actualizarCantidad(idProducto: Int, cantidad: Int) {
        val carrito = obtenerCarrito().toMutableList()
        val index = carrito.indexOfFirst { it.id_producto == idProducto }
        
        if (index >= 0) {
            if (cantidad <= 0) {
                carrito.removeAt(index)
            } else {
                carrito[index] = carrito[index].copy(cantidad = cantidad)
            }
            guardarCarrito(carrito)
        }
    }

    fun eliminarProducto(idProducto: Int) {
        val carrito = obtenerCarrito().toMutableList()
        carrito.removeAll { it.id_producto == idProducto }
        guardarCarrito(carrito)
    }

    fun obtenerTotal(): Double {
        return obtenerCarrito().sumOf { it.precio * it.cantidad }
    }

    fun obtenerCantidadArticulos(): Int {
        return obtenerCarrito().sumOf { it.cantidad }
    }

    fun vaciarCarrito() {
        sharedPreferences.edit().remove(CART_KEY).apply()
    }

    private fun guardarCarrito(carrito: List<CartItem>) {
        val json = gson.toJson(carrito)
        sharedPreferences.edit().putString(CART_KEY, json).apply()
    }
}
