package com.mugiwara.petscop.network

import com.mugiwara.petscop.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface PetscopApiService {
    
    @GET("api/usuarios/buscar-clientes")
    suspend fun buscarClientes(@Query("query") query: String): List<UserResponse>

    @POST("api/auth/registrar-veterinario")
    suspend fun registrarVeterinarioApi(@Body datos: Map<String, Any>): Map<String, Any>

    @POST("api/auth/registrar-cliente")
    suspend fun registrarClienteApi(@Body datos: Map<String, Any>): Map<String, Any>

    @GET("api/usuarios/buscar-veterinarios-clinica")
    suspend fun getVeterinariosPorCliente(@Header("X-User-Email") email: String): List<UserResponse>

    @GET("api/citas/mis-citas")
    suspend fun getCitasPorCliente(@Header("X-User-Email") email: String): List<Cita>

    @GET("api/citas/veterinario/mis-citas")
    suspend fun getCitasPorVeterinario(@Header("X-User-Email") email: String): List<Cita>

    @GET("api/mascotas/mis-mascotas")
    suspend fun getMascotasPorCliente(@Header("X-User-Email") email: String): List<Mascota>

    @POST("api/mascotas")
    suspend fun crearMascota(@Body mascota: Map<String, Any>, @Header("X-User-Email") email: String): Mascota

    @POST("api/citas")
    suspend fun crearCita(@Body cita: Map<String, Any>, @Header("X-User-Email") email: String): UserResponse

    @PUT("api/citas/{id}/estado")
    suspend fun actualizarEstadoCita(@Path("id") id: Int, @Body data: Map<String, String>, @Header("X-User-Email") email: String): UserResponse

    @GET("api/clinicas")
    suspend fun getClinicas(): List<Clinica>

    @GET("api/clinicas/registro")
    suspend fun getClinicasRegistro(): List<Clinica>

    @GET("api/productos/clinica/{id}")
    suspend fun getProductosPorClinica(@Path("id") idClinica: Int, @Header("X-User-Email") email: String): List<Producto>

    @POST("api/pedidos")
    suspend fun crearPedido(@Body pedido: Map<String, Any>, @Header("X-User-Email") email: String): Map<String, Any>

    @GET("api/chats/lista")
    suspend fun getListaChats(@Header("X-User-Email") email: String): List<Chat>

    @GET("api/productos/veterinario")
    suspend fun getProductosPorVeterinario(@Header("X-User-Email") email: String): List<Producto>

    @GET("api/pedidos/veterinario")
    suspend fun getPedidosPorVeterinario(@Header("X-User-Email") email: String): List<Pedido>

    @POST("api/productos")
    suspend fun crearProducto(@Body producto: Map<String, Any>, @Header("X-User-Email") email: String): Map<String, Any>

    @PUT("api/productos/{id}")
    suspend fun actualizarProducto(@Path("id") id: Int, @Body producto: Map<String, Any>, @Header("X-User-Email") email: String): Map<String, Any>

    @DELETE("api/productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: Int, @Header("X-User-Email") email: String): Map<String, Any>

    @PUT("api/pedidos/{id}/estado")
    suspend fun actualizarEstadoPedido(@Path("id") idPedido: Int, @Body data: Map<String, String>, @Header("X-User-Email") email: String): Map<String, Any>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000/"

        fun create(): PetscopApiService {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val client = OkHttpClient.Builder().addInterceptor(logger).build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PetscopApiService::class.java)
        }
    }
}
