package com.mugiwara.petscop.network

import com.mugiwara.petscop.model.Cita
import com.mugiwara.petscop.model.Chat
import com.mugiwara.petscop.model.Clinica
import com.mugiwara.petscop.model.Mascota
import com.mugiwara.petscop.model.Pedido
import com.mugiwara.petscop.model.Producto
import com.mugiwara.petscop.model.UserResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PetscopApiService {
    
    @GET("api/usuarios/buscar-clientes")
    suspend fun buscarClientes(@Query("query") query: String): List<UserResponse>

    @GET("api/usuarios/buscar-veterinarios-clinica/{email}")
    suspend fun getVeterinariosPorCliente(@Path("email") email: String): List<UserResponse>

    @GET("api/citas/cliente/{email}")
    suspend fun getCitasPorCliente(@Path("email") email: String): List<Cita>

    @GET("api/citas/veterinario/{email}")
    suspend fun getCitasPorVeterinario(@Path("email") email: String): List<Cita>

    @GET("api/mascotas/cliente/{email}")
    suspend fun getMascotasPorCliente(@Path("email") email: String): List<Mascota>

    @POST("api/citas")
    suspend fun crearCita(@Body cita: Map<String, Any>, @Header("X-User-Email") email: String): UserResponse

    @PUT("api/citas/{id}/estado")
    suspend fun actualizarEstadoCita(@Path("id") id: Int, @Body data: Map<String, String>, @Header("X-User-Email") email: String): UserResponse

    @GET("api/clinicas")
    suspend fun getClinicas(): List<Clinica>

    @GET("api/clinicas/registro")
    suspend fun getClinicasRegistro(): List<Clinica>

    @GET("api/productos/clinica/{id_clinica}")
    suspend fun getProductosPorClinica(@Path("id_clinica") idClinica: Int, @Header("X-User-Email") email: String): List<Producto>

    @POST("api/pedidos")
    suspend fun crearPedido(@Body pedido: Map<String, Any>, @Header("X-User-Email") email: String): Map<String, Any>

    @GET("api/productos/veterinario/{email}")
    suspend fun getProductosPorVeterinario(@Path("email") email: String): List<Producto>

    @GET("api/pedidos/veterinario/{email}")
    suspend fun getPedidosPorVeterinario(@Path("email") email: String): List<Pedido>

    @POST("api/productos")
    suspend fun crearProducto(@Body producto: Map<String, Any>, @Header("X-User-Email") email: String): Map<String, Any>

    @PUT("api/productos/{id}")
    suspend fun actualizarProducto(@Path("id") id: Int, @Body producto: Map<String, Any>, @Header("X-User-Email") email: String): Map<String, Any>

    @DELETE("api/productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: Int, @Header("X-User-Email") email: String): Map<String, Any>

    @PUT("api/pedidos/{id}/estado")
    suspend fun actualizarEstadoPedido(@Path("id") id: Int, @Body data: Map<String, String>, @Header("X-User-Email") email: String): Map<String, Any>

    @GET("api/chats/lista")
    suspend fun getListaChats(@Header("X-User-ID") userId: String): List<Chat>
    
    @POST("api/auth/registro/cliente")
    suspend fun registrarClienteApi(@Body datos: Map<String, String>): Map<String, Any>

    @POST("api/auth/registro/veterinario")
    suspend fun registrarVeterinarioApi(@Body datos: Map<String, String>): Map<String, Any>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000/"

        fun create(): PetscopApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PetscopApiService::class.java)
        }
    }
}
