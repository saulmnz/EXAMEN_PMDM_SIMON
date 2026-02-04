# ROOM 🏠

>[!NOTE]
> ***Es una librería de Google que actúa como una capa superior sobre SQLite. Nos ahorra escribir mucho código repetitivo (boilerplate), verifica las consultas SQL mientras escribes el código (no al ejecutar) y se integra perfectamente con Corrutinas.***

### CONCEPTOS FUNDAMENTALES DE ROOM

- ***Entity (@Entity): Representa una tabla de la base de datos. Es una simple clase de datos (Data Class).***

- ***DAO (Data Access Object): Es una interfaz donde definimos QUÉ queremos hacer (Insertar, Consultar, Borrar). Room se encarga del "cómo".***

- ***Database (@Database): Es la clase abstracta que conecta las Entidades con los DAOs. Suele usar el patrón Singleton.***

- ***Suspend Functions: Room nos obliga a usar funciones de suspensión (Corrutinas) para no bloquear la pantalla principal al leer/escribir datos.***


---


```kotlin
plugins {
    id("kotlin-kapt") // NECESARIO PARA QUE ROOM GENERE CÓDIGO AUTOMÁTICO
}

dependencies {
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // SOPORTE PARA CORRUTINAS
    kapt("androidx.room:room-compiler:$room_version")
}
```

---

## ENTIDAD 
```kotlin
package com.example.examen_pmdm_simon.data
import androidx.room.Entity
import androidx.room.PrimaryKey

// DEFINIMOS QUE ESTA CLASE ES UNA TABLA LLAMADA 'TABLA_RECORDS'
@Entity(tableName = "tabla_records")
data class RecordEntity(
    // CLAVE PRIMARIA AUTOGENERADA (COMO EL AUTOINCREMENT DE SQLITE)
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ronda: Int,
    val fecha: String
)
```

---

## DAO (DATA ACCESS OBJECT)
```kotlin
package com.example.examen_pmdm_simon.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SimonDao {
    // USAMOS 'SUSPEND' PARA QUE SE EJECUTE EN SEGUNDO PLANO (CORRUTINA)
    @Insert
    suspend fun insert(record: RecordEntity)

    // CONSULTA SQL VERIFICADA EN TIEMPO DE COMPILACIÓN
    @Query("SELECT MAX(ronda) FROM tabla_records")
    suspend fun getMaxRonda(): Int?

    // OBTENER LA FECHA DEL RÉCORD (LIMIT 1 PARA OBTENER SOLO UNO)
    @Query("SELECT fecha FROM tabla_records WHERE ronda = :ronda ORDER BY id DESC LIMIT 1")
    suspend fun getFechaByRonda(ronda: Int): String?
}
```

---

## DATABASE SINGLETON

```kotlin
package com.example.examen_pmdm_simon.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// DECLARAMOS LAS ENTIDADES Y LA VERSIÓN DE LA BD
@Database(entities = [RecordEntity::class], version = 1)
abstract class SimonDatabase : RoomDatabase() {

    abstract fun simonDao(): SimonDao

    companion object {
        @Volatile
        private var INSTANCE: SimonDatabase? = null

        fun getDatabase(context: Context): SimonDatabase {
            // PATRÓN SINGLETON: SI YA EXISTE LA DEVUELVE, SI NO, LA CREA
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SimonDatabase::class.java,
                    "simon_room_db" // NOMBRE DEL ARCHIVO FÍSICO
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

---

## USO DE ROOM EN EL REPOSITORIO

```kotlin
class MyViewModel(application: Application) : AndroidViewModel(application) {

    // INSTANCIAMOS LA BASE DE DATOS Y EL DAO
    private val database = SimonDatabase.getDatabase(application)
    private val dao = database.simonDao()

    var fechaRecord by mutableStateOf("Cargando...")

    init {
        // LANZAMOS UNA CORRUTINA EN EL HILO DE I/O (ENTRADA/SALIDA)
        viewModelScope.launch(Dispatchers.IO) {
            
            // 1. OBTENEMOS DATOS DE LA BD (OPERACIÓN PESADA)
            val maxRonda = dao.getMaxRonda() ?: 0
            val fecha = if (maxRonda > 0) dao.getFechaByRonda(maxRonda) ?: "Sin fecha" else "Sin fecha"

            // 2. VOLVEMOS AL HILO PRINCIPAL (MAIN) PARA ACTUALIZAR LA UI
            withContext(Dispatchers.Main) {
                recordEnMemoria = maxRonda
                fechaRecord = fecha
                Log.d("ROOM_SIMON", "DATOS CARGADOS: $maxRonda - $fecha")
            }
        }
    }

    private fun actualizarRecord() {
        if (ronda > recordEnMemoria) {
            recordEnMemoria = ronda
            
            // ACTUALIZAMOS LA VARIABLE REACTIVA
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaActual = sdf.format(Date())
            fechaRecord = fechaActual

            // GUARDAMOS EN ROOM USANDO OTRA CORRUTINA
            viewModelScope.launch(Dispatchers.IO) {
                // INSERTAMOS EL OBJETO ENTIDAD DIRECTAMENTE
                dao.insert(RecordEntity(ronda = ronda, fecha = fechaActual))
                
                Log.d("ROOM_SIMON", "NUEVO RÉCORD GUARDADO EN ROOM: $ronda")
            }
        }
    }
}
```

---

### EJEMPLO COMPLETO ROOM OJITO 👻

```kotlin
package com.example.examen_pmdm_simon.data

import android.content.Context
import androidx.room.*

// ==========================================
// 1. ENTIDADES (TABLAS)
// ==========================================

@Entity(tableName = "tabla_usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nombre") val nombre: String
)

@Entity(
    tableName = "historico_records",
    // AQUÍ SE DEFINE LA RELACIÓN (CLAVE FORÁNEA)
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],       // PK del Padre
            childColumns = ["usuario_id"], // FK del Hijo
            onDelete = ForeignKey.CASCADE  // Si borras al usuario, se borran sus récords
        )
    ],
    // Opcional: Crear un índice en la FK hace las consultas más rápidas
    indices = [Index(value = ["usuario_id"])] 
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "ronda") val ronda: Int,
    @ColumnInfo(name = "fecha") val fecha: String,
    
    // ESTA ES LA COLUMNA QUE UNE LAS TABLAS
    @ColumnInfo(name = "usuario_id") val usuarioId: Int 
)

// ==========================================
// 2. CLASE DE RELACIÓN (EL "JOIN")
// ==========================================
// Esta clase NO es una entidad, es un contenedor para el resultado
data class UsuarioConRecords(
    @Embedded val usuario: UsuarioEntity,
    
    @Relation(
        parentColumn = "id",        // ID en UsuarioEntity
        entityColumn = "usuario_id" // ID en RecordEntity
    )
    val listaRecords: List<RecordEntity>
)

// ==========================================
// 3. DAOS (ACCESO A DATOS)
// ==========================================

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: UsuarioEntity): Long // Devuelve el ID insertado

    @Query("SELECT * FROM tabla_usuarios")
    suspend fun obtenerTodos(): List<UsuarioEntity>

    // --- ¡IMPORTANTE! CONSULTA CON RELACIÓN ---
    // @Transaction es obligatorio porque Room hace dos consultas internamente
    @Transaction
    @Query("SELECT * FROM tabla_usuarios")
    suspend fun obtenerUsuariosConSusRecords(): List<UsuarioConRecords>

    @Query("DELETE FROM tabla_usuarios WHERE id = :userId")
    suspend fun borrarPorId(userId: Int)
}

@Dao
interface RecordDao {
    @Insert
    suspend fun insertar(record: RecordEntity)
    
    // Obtener el récord máximo de un usuario específico
    @Query("SELECT MAX(ronda) FROM historico_records WHERE usuario_id = :userId")
    suspend fun obtenerMaxRecordDeUsuario(userId: Int): Int?
}

// ==========================================
// 4. BASE DE DATOS (SINGLETON)
// ==========================================

@Database(entities = [UsuarioEntity::class, RecordEntity::class], version = 1, exportSchema = false)
abstract class SimonRoomDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile
        private var INSTANCE: SimonRoomDatabase? = null

        fun getDatabase(context: Context): SimonRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SimonRoomDatabase::class.java,
                    "simon_database_relacional"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

```kotlin
package com.example.examen_pmdm_simon.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.examen_pmdm_simon.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SimonRoomDatabase.getDatabase(application)
    private val usuarioDao = db.usuarioDao()
    private val recordDao = db.recordDao()

    // --- ESTADO UI ---
    var textoInfoUsuarios by mutableStateOf("Cargando...")
    
    // JUEGO
    var ronda by mutableStateOf(0)
    var estadoActual by mutableStateOf(EstadoJuego.INICIO)
    var colorIluminado by mutableStateOf<Colores?>(null)
    
    // GESTIÓN DE USUARIO ACTUAL
    // Guardamos el ID del usuario que está jugando ahora mismo
    // Si es -1, significa que no hay usuario seleccionado
    var idUsuarioActual by mutableStateOf(-1) 
    var nombreUsuarioActual by mutableStateOf("Nadie")
    var recordPersonalActual by mutableStateOf(0)

    private val secuenciaSimon = mutableListOf<Colores>()
    private var indiceUsuario = 0

    init {
        viewModelScope.launch {
            // Cargar lista inicial
            actualizarListaCompleta()
            
            // Opcional: Crear un usuario por defecto si no hay nadie
            crearUsuarioPorDefectoSiVacio()
        }
    }

    // --- GESTIÓN DE USUARIOS Y RELACIONES ---

    private suspend fun crearUsuarioPorDefectoSiVacio() {
        val usuarios = usuarioDao.obtenerTodos()
        if (usuarios.isEmpty()) {
            val id = usuarioDao.insertar(UsuarioEntity(nombre = "Jugador 1"))
            seleccionarUsuario(id.toInt(), "Jugador 1")
        } else {
            // Seleccionar el primero de la lista por defecto
            seleccionarUsuario(usuarios.first().id, usuarios.first().nombre)
        }
    }

    // Llamamos a esta función cuando el usuario clica en un nombre de la lista (si implementas eso)
    fun seleccionarUsuario(id: Int, nombre: String) {
        idUsuarioActual = id
        nombreUsuarioActual = nombre
        viewModelScope.launch {
            // Buscamos su mejor récord personal en la BD
            val max = recordDao.obtenerMaxRecordDeUsuario(id) ?: 0
            recordPersonalActual = max
        }
    }

    fun registrarNuevoUsuario(nombre: String) {
        viewModelScope.launch {
            val nuevoId = usuarioDao.insertar(UsuarioEntity(nombre = nombre))
            seleccionarUsuario(nuevoId.toInt(), nombre)
            actualizarListaCompleta()
        }
    }
    
    fun borrarUsuario(id: Int) {
        viewModelScope.launch {
            usuarioDao.borrarPorId(id)
            actualizarListaCompleta()
            // Si borramos al usuario actual, reseteamos
            if (id == idUsuarioActual) {
                idUsuarioActual = -1
                nombreUsuarioActual = "Nadie"
            }
        }
    }

    // AQUÍ SE VE LA MAGIA DE LA RELACIÓN
    fun actualizarListaCompleta() {
        viewModelScope.launch {
            val listaRelacionada = usuarioDao.obtenerUsuariosConSusRecords()
            
            // Formateamos el texto para ver los datos cruzados
            val sb = StringBuilder()
            if (listaRelacionada.isEmpty()) sb.append("Sin usuarios")
            
            for (item in listaRelacionada) {
                val u = item.usuario
                val totalPartidas = item.listaRecords.size
                // Calculamos maximo usando la lista que nos devuelve Room
                val maxPuntuacion = item.listaRecords.maxOfOrNull { it.ronda } ?: 0
                
                sb.append("👤 ${u.nombre} (ID: ${u.id})\n")
                sb.append("   ↪ Partidas jugadas: $totalPartidas\n")
                sb.append("   ↪ Mejor Récord: $maxPuntuacion\n\n")
            }
            textoInfoUsuarios = sb.toString()
        }
    }

    // --- LÓGICA DEL JUEGO SIMON ---

    fun iniciarJuego() {
        if (idUsuarioActual == -1) {
            Log.e("SIMON", "¡Debes seleccionar un usuario antes de jugar!")
            return 
        }
        secuenciaSimon.clear()
        ronda = 0
        siguienteRonda()
    }

    private fun siguienteRonda() {
        indiceUsuario = 0
        ronda++
        secuenciaSimon.add(Colores.values().random())
        reproducirSecuencia()
    }

    private fun reproducirSecuencia() {
        viewModelScope.launch {
            estadoActual = EstadoJuego.REPRODUCIENDO
            delay(500)
            for (color in secuenciaSimon) {
                colorIluminado = color
                delay(500)
                colorIluminado = null
                delay(250)
            }
            estadoActual = EstadoJuego.ESPERANDO
        }
    }

    fun respuestaUsuario(colorPulsado: Colores) {
        if (estadoActual != EstadoJuego.ESPERANDO) return

        if (colorPulsado == secuenciaSimon[indiceUsuario]) {
            indiceUsuario++
            if (indiceUsuario == secuenciaSimon.size) {
                // Ronda superada
                siguienteRonda()
            }
        } else {
            // PERDIÓ: GUARDAMOS EL RÉCORD
            guardarPartidaEnBaseDeDatos()
            estadoActual = EstadoJuego.GAME_OVER
        }
    }

    private fun guardarPartidaEnBaseDeDatos() {
        if (idUsuarioActual == -1) return

        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaActual = sdf.format(Date())

            // CREAMOS EL RECORD ASOCIADO AL USUARIO ACTUAL (CLAVE FORÁNEA)
            val nuevoRecord = RecordEntity(
                ronda = ronda, 
                fecha = fechaActual,
                usuarioId = idUsuarioActual // <-- AQUÍ SE HACE LA RELACIÓN
            )
            
            recordDao.insertar(nuevoRecord)
            
            // Actualizamos UI
            if (ronda > recordPersonalActual) {
                recordPersonalActual = ronda
            }
            actualizarListaCompleta()
        }
    }
}
```
