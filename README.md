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


