package com.mugiwara.petscop

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mugiwara.petscop.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Configurar Toolbar como ActionBar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // 1. Obtener el NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 2. Conectar BottomNavigationView con NavController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // 3. Setup DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        // Configurar ActionBar toggle (hamburguesa)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Manejar clicks en los items del drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> navController.navigate(R.id.nav_profile)
                R.id.nav_settings -> navController.navigate(R.id.nav_settings)
                R.id.nav_subscription -> navController.navigate(R.id.nav_subscription)
                R.id.nav_carrito -> navController.navigate(R.id.nav_carrito)
                R.id.nav_ventas -> navController.navigate(R.id.nav_ventas)
                R.id.nav_mascotas -> navController.navigate(R.id.nav_mascotas)
                R.id.nav_logout -> {
                    auth.signOut()
                    // Navegar a LoginActivity
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    return@setNavigationItemSelectedListener true
                }
                else -> false
            }
            drawerLayout.closeDrawers()
            true
        }

        // 4. Detectar rol y mostrar/ocultar items según el tipo de usuario
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) {
            db.collection("users").document(firebaseUid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val esVeterinario = document.getBoolean("esVeterinario") ?: false
                        
                        // Items del drawer
                        val drawerSettings = navView.menu.findItem(R.id.nav_settings)
                        val drawerProfile = navView.menu.findItem(R.id.nav_profile)
                        val drawerSubscription = navView.menu.findItem(R.id.nav_subscription)
                        val drawerCarrito = navView.menu.findItem(R.id.nav_carrito)
                        val drawerVentas = navView.menu.findItem(R.id.nav_ventas)
                        val drawerMascotas = navView.menu.findItem(R.id.nav_mascotas)
                        
                        // Items del bottom nav
                        val bottomMarketplace = bottomNav.menu.findItem(R.id.nav_marketplace)
                        
                        if (esVeterinario) {
                            // Veterinario: mostrar Perfil, Suscripción y Ventas, ocultar Configuración, Carrito y Mascotas
                            drawerSettings?.isVisible = false
                            drawerProfile?.isVisible = true
                            drawerSubscription?.isVisible = true
                            drawerCarrito?.isVisible = false
                            drawerVentas?.isVisible = true
                            drawerMascotas?.isVisible = false
                            bottomMarketplace?.isVisible = false
                        } else {
                            // Cliente: mostrar Configuración, Carrito y Mascotas, ocultar Suscripción y Ventas
                            drawerSettings?.isVisible = true
                            drawerProfile?.isVisible = true
                            drawerSubscription?.isVisible = false
                            drawerCarrito?.isVisible = true
                            drawerVentas?.isVisible = false
                            drawerMascotas?.isVisible = true
                            bottomMarketplace?.isVisible = true
                        }
                    }
                }
        }

        // 5. Ocultar el menú inferior cuando estemos en la pantalla de CHAT DETALLE
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.chatFragment) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Dejar que el ActionBarDrawerToggle maneje el click del ícono hamburguesa
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (drawerLayout.isDrawerOpen(findViewById(R.id.nav_view))) {
            drawerLayout.closeDrawers()
            true
        } else {
            super.onSupportNavigateUp()
        }
    }
}

/*
# Busca un usuario por email
@app.get("/usuario/buscar", response_model=Usuario)
def buscar_usuario_por_email(email: str):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Usamos LOWER para que no importe si escriben con mayúsculas
        query = "SELECT firebase_uid, nombre, email FROM cliente WHERE LOWER(email) = %s LIMIT 1"
        cursor.execute(query, (email.lower(),))

        # Obtenemos el primer resultado (si existe)
        usuario = cursor.fetchone()

        if not usuario:
            raise HTTPException(status_code=404, detail="No se encontró ningún usuario con ese email")

        return usuario

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

@app.get("/chats/lista", response_model=List[Chat])
def get_lista_chats(mi_uid: str):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # 1. Buscamos solo CLIENTES (quitamos la tabla clinica)
        # 2. Obtenemos el último mensaje intercambiado con cada uno
        query = """
            SELECT
                c.firebase_uid as uid,
                c.nombre,
                (SELECT mensaje FROM mensajes
                 WHERE (emisor_id = %s AND receptor_id = c.firebase_uid)
                    OR (receptor_id = %s AND emisor_id = c.firebase_uid)
                 ORDER BY fecha DESC LIMIT 1) as ultimo_mensaje,
                (SELECT fecha FROM mensajes
                 WHERE (emisor_id = %s AND receptor_id = c.firebase_uid)
                    OR (receptor_id = %s AND emisor_id = c.firebase_uid)
                 ORDER BY fecha DESC LIMIT 1) as hora
            FROM cliente c
            WHERE c.firebase_uid != %s
        """

        cursor.execute(query, (mi_uid, mi_uid, mi_uid, mi_uid, mi_uid))
        resultados = cursor.fetchall()

        # Formateamos para que Android lo reciba perfecto
        lista_final = []
        for r in resultados:
            # Solo añadimos a la lista si hay un mensaje previo (chats activos)
            if r['ultimo_mensaje']:
                lista_final.append({
                    "uid": r['uid'],
                    "nombre": r['nombre'],
                    "ultimoMensaje": r['ultimo_mensaje'],
                    "hora": str(r['hora']) if r['hora'] else "",
                    "inicial": r['nombre'][0].upper() if r['nombre'] else "?"
                })

        return lista_final

    finally:
        cursor.close()
        conn.close()


@app.get("/citas/proximas", response_model=List[Cita])
def get_proximas_citas(mi_uid: str, es_veterinario: bool):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        if es_veterinario:
            # Si soy VETERINARIO:
            # 1. Filtro por mi firebase_uid (en la tabla veterinario)
            # 2. Quiero ver el nombre del CLIENTE y de su MASCOTA
            query = """
                SELECT ci.id_cita, ci.fecha, ci.hora, ci.motivo,
                       CONCAT(c.nombre, ' ', c.apellidos) as nombre_otro,
                       m.nombre as mascota_nombre
                FROM cita ci
                JOIN veterinario v ON ci.id_veterinario = v.id_veterinario
                JOIN cliente c ON ci.firebase_uid_cliente = c.firebase_uid
                JOIN mascota m ON ci.id_mascota = m.id_mascota
                WHERE v.firebase_uid = %s AND ci.fecha >= CURDATE()
                ORDER BY ci.fecha ASC, ci.hora ASC
            """
        else:
            # Si soy CLIENTE:
            # 1. Filtro por mi firebase_uid_cliente
            # 2. Quiero ver el nombre de la CLÍNICA donde tengo la cita
            query = """
                SELECT ci.id_cita, ci.fecha, ci.hora, ci.motivo,
                       cl.nombre as nombre_otro,
                       m.nombre as mascota_nombre
                FROM cita ci
                JOIN veterinario v ON ci.id_veterinario = v.id_veterinario
                JOIN clinica cl ON v.id_clinica = cl.id_clinica
                JOIN mascota m ON ci.id_mascota = m.id_mascota
                WHERE ci.firebase_uid_cliente = %s AND ci.fecha >= CURDATE()
                ORDER BY ci.fecha ASC, ci.hora ASC
            """

        cursor.execute(query, (mi_uid,))
        citas = cursor.fetchall()

        # Formateamos fechas y horas para que no den error al enviar el JSON
        for c in citas:
            c['fecha'] = str(c['fecha'])
            c['hora'] = str(c['hora'])
            # Opcional: Combinar motivo con nombre de mascota para Android
            if c.get('mascota_nombre'):
                c['motivo'] = f"{c['mascota_nombre']}: {c['motivo']}"

        return citas

    finally:
        cursor.close()
        conn.close()

@app.get("/market/productos", response_model=List[Producto])
def get_productos(categoria: str = None):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    try:
        # Consulta base
        query = """
            SELECT p.*, cl.nombre as clinica_nombre
            FROM producto p
            JOIN clinica cl ON p.id_clinica = cl.id_clinica
        """
        # Filtro opcional por categoría
        if categoria and categoria != "Todos":
            query += " WHERE p.categoria = %s"
            cursor.execute(query, (categoria,))
        else:
            cursor.execute(query)

        return cursor.fetchall()
    finally:
        cursor.close()
        conn.close()


@app.post("/productos", status_code=201, response_model=ApiResponse)
def crear_producto(producto: ProductoCreate):
    conn = get_db_connection()
    cursor = conn.cursor()

    try:
        query = """
            INSERT INTO producto
            (id_clinica, nombre, categoria, descripcion, precio, stock, foto_url)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """
        values = (
            producto.id_clinica,
            producto.nombre,
            producto.categoria,
            producto.descripcion,
            producto.precio,
            producto.stock,
            producto.foto_url
        )

        cursor.execute(query, values)
        conn.commit()

        return {
            "status": "success",
            "message": "Producto publicado con éxito",
            "id_producto": cursor.lastrowid
        }

    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=f"Error al crear producto: {str(e)}")
    finally:
        cursor.close()
        conn.close()


* */