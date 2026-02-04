# SQLITE 🥣

>[!NOTE]
> ***SQLite nos permite guardar estructura de datos más complejos y un historial completo de partidas, no solo el último record***

### CONCEPTOS FUNDAMENTALES DE SQLITE


- ***SQLiteOpenHelper: Es la clase "maestra" que gestiona la creación y la versión de la base de datos.***
- ***Cursor: Es el objeto que nos permite navegar por los resultados de una consulta ( es como un puntero que recorre las filas)***
- ***ContentValues: Un contenedor de datos tipo clave-valor que se usa para insertar o actualizar filas en la tabla***

### PRIMER PASO: CREAR LA CLASE DATABASEHELPER

```kotlin
package com.example.examen_pmdm_simon.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// CLASE QUE GESTIONA LA CREACIÓN Y CONEXIÓN A LA BASE DE DATOS SQLITE
class SimonDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // NOMBRE DEL ARCHIVO DE LA BASE DE DATOS
        private const val DATABASE_NAME = "SimonGame.db"
        // VERSIÓN DE LA BASE DE DATOS
        private const val DATABASE_VERSION = 1

        // DEFINICIÓN DE LA TABLA Y SUS COLUMNAS PARA EL RÉCORD Y LA FECHA
        // NOMBRE TABLA
        const val TABLE_RECORDS = "historico_records"
        // NOMBRE COLUMNAS - ID - RONDA - FECHA
        const val COLUMN_ID = "id"
        const val COLUMN_RONDA = "ronda"
        const val COLUMN_FECHA = "fecha"
    }

    // SE EJECUTA AUTOMÁTICAMENTE LA PRIMERA VEZ QUE SE ACCEDE A LA BASE DE DATOS
    override fun onCreate(db: SQLiteDatabase?) {
        // DEFINIMOS LA SENTENCIA SQL PARA CREAR LA TABLA DE RÉCORDS
        val createTableQuery = ("CREATE TABLE $TABLE_RECORDS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " + // ID AUTONUMÉRICO
                "$COLUMN_RONDA INTEGER, " +                         // COLUMNA PARA LA RONDA MÁS ALTA
                "$COLUMN_FECHA TEXT)")                             // COLUMNA PARA LA FECHA DEL LOGRO

        // EJECUTAMOS LA CREACIÓN DE LA TABLA
        db?.execSQL(createTableQuery)
    }

    // SE EJECUTA SI SE DETECTA UNA VERSIÓN DE DATABASE_VERSION SUPERIOR A LA INSTALADA
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // ELIMINAMOS LA TABLA SI YA EXISTÍA PARA EVITAR CONFLICTOS
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        // VOLVEMOS A CREARLA VACÍA
        onCreate(db)
    }

    // MÉTODO PARA INSERTAR UNA NUEVA FILA CON EL RÉCORD Y LA FECHA
    fun insertarRecord(ronda: Int, fecha: String) {
        // OBTENEMOS LA BASE DE DATOS EN MODO ESCRITURA
        val db = this.writableDatabase
        // CONTENEDOR DE VALORES TIPO CLAVE-VALOR PARA LA INSERCIÓN
        val values = ContentValues()
        // ASOCIAMOS EL VALOR DE LA RONDA A SU COLUMNA
        values.put(COLUMN_RONDA, ronda)
        // ASOCIAMOS EL TEXTO DE LA FECHA A SU COLUMNA
        values.put(COLUMN_FECHA, fecha)

        // INSERTAMOS LOS DATOS EN LA TABLA CORRESPONDIENTE
        db.insert(TABLE_RECORDS, null, values)

        val resultado = db.insert(TABLE_RECORDS, null, values)
        if (resultado != -1L) {
            Log.d("SQLITE_SIMON", "INSERTADO CON ÉXITO: Ronda $ronda el $fecha")
        } else {
            Log.e("SQLITE_SIMON", "ERROR AL INSERTAR EL RÉCORD")
        }

        // CERRAMOS LA CONEXIÓN PARA LIBERAR RECURSOS
        db.close()
    }

    // MÉTODO PARA CONSULTAR LA RONDA MÁS ALTA ALMACENADA
    fun obtenerMaximoRecord(): Int {
        // OBTENEMOS LA BASE DE DATOS EN MODO LECTURA
        val db = this.readableDatabase
        // REALIZAMOS UNA CONSULTA SQL PARA BUSCAR EL VALOR MÁXIMO DE LA COLUMNA RONDA
        val cursor = db.rawQuery("SELECT MAX($COLUMN_RONDA) FROM $TABLE_RECORDS", null)

        var maxRonda = 0
        // SI EL CURSOR TIENE RESULTADOS, ACCEDEMOS AL PRIMERO
        if (cursor.moveToFirst()) {
            // EL RESULTADO ESTÁ EN LA POSICIÓN 0 DE LA CONSULTA
            maxRonda = cursor.getInt(0)
        }
        // CERRAMOS EL CURSOR PARA EVITAR FUGAS DE MEMORIA (MEMORY LEAKS)
        cursor.close()
        // DEVOLVEMOS EL RÉCORD ENCONTRADO O 0 SI NO HABÍA NADA
        return maxRonda
    }

    fun obtenerFechaDelRecord(puntuacion: Int): String {

        // ABRIMOS LA BD EN MODO LECTURA
        val db = this.readableDatabase

        // DEFINIMOS LA QUERY, PEDIMOS LA COLUMNA FECHA DE LA TABLA RECORDS DONDE LA RONDA COINCIDA CON NUESTRO PARÁMETRO, SI HAY EMPATES TRAEMOS EL ID MÁS ALTO ( REGISTRO MÁS RECIENTE )
        val query = "SELECT $COLUMN_FECHA FROM $TABLE_RECORDS WHERE $COLUMN_RONDA = ? ORDER BY $COLUMN_ID DESC"

        // EJECUTAMOS LA CONSULTA CON CURSOR
        val cursor = db.rawQuery(query, arrayOf(puntuacion.toString()))

        // CREAMOS UNA VARIABLE PARA GUARDAR EL RESULTADO
        var fechaEncontrada = "Sin fecha"

        // INTENTAMOS MOVER EL CURSOR A LA PRIMERA FILA DE RESULTADOS, SI DEVUELVE TRUE ES QUE ENCONTRÓ DATOS
        if (cursor.moveToFirst()) {
            // EXTRAEMOS EL VALOR DE LA COLUMNA 0 (LA ÚNICA QUE PEDIMOS EN EL SELECT)
            fechaEncontrada = cursor.getString(0)
        }

        cursor.close()
        return fechaEncontrada
    }
}
```

---

### SEGUNDO PASO: USAR LA BASE DE DATOS EN LA ACTIVIDAD PRINCIPAL

```kotlin

class MyViewModel(application: Application) : AndroidViewModel(application) {

    // INSTANCIAMOS EL HELPER DE SQLITE PASÁNDOLE EL CONTEXTO DE LA APP
    private val dbHelper = SimonDatabaseHelper(application)
    // FECHA DEL RÉCORD ACTUAL
    var fechaRecord by mutableStateOf("")

    init {
        // AL CARGAR EL VIEWMODEL, BUSCAMOS EL RÉCORD MÁXIMO EN LA BASE DE DATOS SQLITE
        recordEnMemoria = dbHelper.obtenerMaximoRecord()
        fechaRecord = dbHelper.obtenerFechaDelRecord(recordEnMemoria)

        Log.d("SQLITE_SIMON", "DATOS CARGADOS AL INICIO: Récord $recordEnMemoria ($fechaRecord)")

    }

    private fun actualizarRecord() {
        // VERIFICAMOS SI LA RONDA ACTUAL SUPERA EL RÉCORD HISTÓRICO
        if (ronda > recordEnMemoria) {
            recordEnMemoria = ronda

            // GENERAMOS LA FECHA Y HORA DEL MOMENTO ACTUAL
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaActual = sdf.format(Date())
            fechaRecord = fechaActual

            // GUARDAMOS EL NUEVO RÉCORD Y LA FECHA EN LA TABLA SQLITE
            dbHelper.insertarRecord(recordEnMemoria, fechaActual)
        }
    }

```


---

### GUARDADO ANTES DE MODIFICAR

-  **BD**

```kotlin
package com.example.examen_pmdm_simon.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// CLASE QUE GESTIONA LA CREACIÓN Y CONEXIÓN A LA BASE DE DATOS SQLITE
class SimonDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // NOMBRE DEL ARCHIVO DE LA BASE DE DATOS
        private const val DATABASE_NAME = "SimonGame.db"
        // VERSIÓN DE LA BASE DE DATOS
        private const val DATABASE_VERSION = 1

        // DEFINICIÓN DE LA TABLA Y SUS COLUMNAS PARA EL RÉCORD Y LA FECHA
        // NOMBRE TABLA
        const val TABLE_RECORDS = "historico_records"
        // NOMBRE COLUMNAS - ID - RONDA - FECHA
        const val COLUMN_ID = "id"
        const val COLUMN_RONDA = "ronda"
        const val COLUMN_FECHA = "fecha"
    }

    // SE EJECUTA AUTOMÁTICAMENTE LA PRIMERA VEZ QUE SE ACCEDE A LA BASE DE DATOS
    override fun onCreate(db: SQLiteDatabase?) {
        // DEFINIMOS LA SENTENCIA SQL PARA CREAR LA TABLA DE RÉCORDS
        val createTableQuery = ("CREATE TABLE $TABLE_RECORDS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " + // ID AUTONUMÉRICO
                "$COLUMN_RONDA INTEGER, " +                         // COLUMNA PARA LA RONDA MÁS ALTA
                "$COLUMN_FECHA TEXT)")                             // COLUMNA PARA LA FECHA DEL LOGRO

        // EJECUTAMOS LA CREACIÓN DE LA TABLA
        db?.execSQL(createTableQuery)
    }

    // SE EJECUTA SI SE DETECTA UNA VERSIÓN DE DATABASE_VERSION SUPERIOR A LA INSTALADA
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // ELIMINAMOS LA TABLA SI YA EXISTÍA PARA EVITAR CONFLICTOS
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        // VOLVEMOS A CREARLA VACÍA
        onCreate(db)
    }

    // MÉTODO PARA INSERTAR UNA NUEVA FILA CON EL RÉCORD Y LA FECHA
    fun insertarRecord(ronda: Int, fecha: String) {
        // OBTENEMOS LA BASE DE DATOS EN MODO ESCRITURA
        val db = this.writableDatabase
        // CONTENEDOR DE VALORES TIPO CLAVE-VALOR PARA LA INSERCIÓN
        val values = ContentValues()
        // ASOCIAMOS EL VALOR DE LA RONDA A SU COLUMNA
        values.put(COLUMN_RONDA, ronda)
        // ASOCIAMOS EL TEXTO DE LA FECHA A SU COLUMNA
        values.put(COLUMN_FECHA, fecha)

        // INSERTAMOS LOS DATOS EN LA TABLA CORRESPONDIENTE
        db.insert(TABLE_RECORDS, null, values)

        val resultado = db.insert(TABLE_RECORDS, null, values)
        if (resultado != -1L) {
            Log.d("SQLITE_SIMON", "INSERTADO CON ÉXITO: Ronda $ronda el $fecha")
        } else {
            Log.e("SQLITE_SIMON", "ERROR AL INSERTAR EL RÉCORD")
        }

        // CERRAMOS LA CONEXIÓN PARA LIBERAR RECURSOS
        db.close()
    }

    // MÉTODO PARA CONSULTAR LA RONDA MÁS ALTA ALMACENADA
    fun obtenerMaximoRecord(): Int {
        // OBTENEMOS LA BASE DE DATOS EN MODO LECTURA
        val db = this.readableDatabase
        // REALIZAMOS UNA CONSULTA SQL PARA BUSCAR EL VALOR MÁXIMO DE LA COLUMNA RONDA
        val cursor = db.rawQuery("SELECT MAX($COLUMN_RONDA) FROM $TABLE_RECORDS", null)

        var maxRonda = 0
        // SI EL CURSOR TIENE RESULTADOS, ACCEDEMOS AL PRIMERO
        if (cursor.moveToFirst()) {
            // EL RESULTADO ESTÁ EN LA POSICIÓN 0 DE LA CONSULTA
            maxRonda = cursor.getInt(0)
        }
        // CERRAMOS EL CURSOR PARA EVITAR FUGAS DE MEMORIA (MEMORY LEAKS)
        cursor.close()
        // DEVOLVEMOS EL RÉCORD ENCONTRADO O 0 SI NO HABÍA NADA
        return maxRonda
    }

    fun obtenerFechaDelRecord(puntuacion: Int): String {

        // ABRIMOS LA BD EN MODO LECTURA
        val db = this.readableDatabase

        // DEFINIMOS LA QUERY, PEDIMOS LA COLUMNA FECHA DE LA TABLA RECORDS DONDE LA RONDA COINCIDA CON NUESTRO PARÁMETRO, SI HAY EMPATES TRAEMOS EL ID MÁS ALTO ( REGISTRO MÁS RECIENTE )
        val query = "SELECT $COLUMN_FECHA FROM $TABLE_RECORDS WHERE $COLUMN_RONDA = ? ORDER BY $COLUMN_ID DESC"

        // EJECUTAMOS LA CONSULTA CON CURSOR
        val cursor = db.rawQuery(query, arrayOf(puntuacion.toString()))

        // CREAMOS UNA VARIABLE PARA GUARDAR EL RESULTADO
        var fechaEncontrada = "Sin fecha"

        // INTENTAMOS MOVER EL CURSOR A LA PRIMERA FILA DE RESULTADOS, SI DEVUELVE TRUE ES QUE ENCONTRÓ DATOS
        if (cursor.moveToFirst()) {
            // EXTRAEMOS EL VALOR DE LA COLUMNA 0 (LA ÚNICA QUE PEDIMOS EN EL SELECT)
            fechaEncontrada = cursor.getString(0)
        }

        cursor.close()
        return fechaEncontrada
    }
}
```

- **VIEWMODEL**

```kotlin
package com.example.examen_pmdm_simon.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.examen_pmdm_simon.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// CAMBIAMOS A AndroidViewModel PARA PASAR EL CONTEXTO AL HELPER
class MyViewModel(application: Application) : AndroidViewModel(application) {


    // INSTANCIAMOS EL HELPER DE SQLITE PASÁNDOLE EL CONTEXTO DE LA APP
    private val dbHelper = SimonDatabaseHelper(application)
    // FECHA DEL RÉCORD ACTUAL
    var fechaRecord by mutableStateOf("")


    // ESTADOS REACTIVOS (La UI se repinta sola cuando cambian)
    var ronda by mutableStateOf(0)
    var recordEnMemoria by mutableStateOf(0)
    var estadoActual by mutableStateOf(EstadoJuego.INICIO)
    var colorIluminado by mutableStateOf<Colores?>(null)

    private val secuenciaSimon = mutableListOf<Colores>()
    private var indiceUsuario = 0


    init {
        // AL CARGAR EL VIEWMODEL, BUSCAMOS EL RÉCORD MÁXIMO EN LA BASE DE DATOS SQLITE
        recordEnMemoria = dbHelper.obtenerMaximoRecord()
        fechaRecord = dbHelper.obtenerFechaDelRecord(recordEnMemoria)

        Log.d("SQLITE_SIMON", "DATOS CARGADOS AL INICIO: Récord $recordEnMemoria ($fechaRecord)")

    }

    fun iniciarJuego() {
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
            delay(500) // Pausa antes de empezar
            for (color in secuenciaSimon) {
                colorIluminado = color
                delay(Constantes.VELOCIDAD_MUESTRA)
                colorIluminado = null
                delay(Constantes.PAUSA_ENTRE_COLORES)
            }
            estadoActual = EstadoJuego.ESPERANDO
        }
    }

    fun respuestaUsuario(colorPulsado: Colores) {
        if (estadoActual != EstadoJuego.ESPERANDO) return

        if (colorPulsado == secuenciaSimon[indiceUsuario]) {
            // Acierto
            indiceUsuario++
            if (indiceUsuario == secuenciaSimon.size) {
                // Ha completado toda la secuencia
                actualizarRecord()
                siguienteRonda()
            }
        } else {
            // Error
            estadoActual = EstadoJuego.GAME_OVER
        }
    }

    private fun actualizarRecord() {
        // VERIFICAMOS SI LA RONDA ACTUAL SUPERA EL RÉCORD HISTÓRICO
        if (ronda > recordEnMemoria) {
            recordEnMemoria = ronda

            // GENERAMOS LA FECHA Y HORA DEL MOMENTO ACTUAL
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaActual = sdf.format(Date())
            fechaRecord = fechaActual

            // GUARDAMOS EL NUEVO RÉCORD Y LA FECHA EN LA TABLA SQLITE
            dbHelper.insertarRecord(recordEnMemoria, fechaActual)
        }
    }
}
```

---

### PROBAR LOS MÉTODOS 

```kotlin
// FUNCIÓN QUE PRUEBA TODOS LOS MÉTODOS Y LOGUEA EL RESULTADO
    private fun ejecutarPruebasDeConsola() {
        val TAG = "SQLITE_TEST_TOTAL"
        Log.i(TAG, "---- INICIANDO BATERÍA DE PRUEBAS ----")

        // PRUEBA 1: INSERTAR RECORD MANUALMENTE
        // Probamos el método insertarRecord()
        dbHelper.insertarRecord(999, "01/01/2099 Test") 
        Log.d(TAG, "1. Insertar Récord Falso (999): OK")

        // PRUEBA 2: OBTENER ESE RECORD POR ID (Suponemos que es un ID alto o el último)
        // Probamos obtenerRecordPorId()
        // Nota: Si es la primera vez, el ID 1 existirá. Si no, busca uno genérico.
        val recordRecuperado = dbHelper.obtenerRecordPorId(1)
        Log.d(TAG, "2. Obtener Record ID 1: $recordRecuperado")

        // PRUEBA 3: LEER EL MÁXIMO (Debería ser 999 si acabamos de insertarlo y es el mayor)
        // Probamos obtenerMaximoRecord()
        val maximo = dbHelper.obtenerMaximoRecord()
        Log.d(TAG, "3. Obtener Máximo (Esperado >= 999): $maximo")

        // PRUEBA 4: GESTIÓN DE USUARIOS (INSERTAR Y BORRAR)
        // A) Insertamos uno para borrarlo
        dbHelper.insertarUsuario("USUARIO_FANTASMA")
        Log.d(TAG, "4A. Usuario Fantasma insertado")

        // B) Leemos la lista para encontrar su ID (Probamos obtenerTodosLosUsuarios)
        val lista = dbHelper.obtenerTodosLosUsuarios() // Devuelve strings "ID: Nombre"
        Log.d(TAG, "4B. Lista actual leída: ${lista.size} usuarios")
        
        // Buscamos el ID del fantasma para probar el borrado
        val usuarioFantasma = lista.find { it.contains("USUARIO_FANTASMA") }
        if (usuarioFantasma != null) {
            // Extraemos el ID del String "5: USUARIO_FANTASMA"
            val idFantasma = usuarioFantasma.substringBefore(":").toInt()
            
            // C) Probamos borrarUsuarioPorId()
            val filas = dbHelper.borrarUsuarioPorId(idFantasma)
            Log.d(TAG, "4C. Borrar Usuario ID $idFantasma: Filas afectadas = $filas (Debería ser 1)")
        } else {
            Log.e(TAG, "4C. Error: No se encontró al usuario fantasma para borrarlo")
        }

        // PRUEBA 5: BORRADO TOTAL (CUIDADO: ESTO BORRA TODO SI LO DESCOMENTAS)
        // dbHelper.borrarTodosLosUsuarios()
        // Log.w(TAG, "5. Borrado total ejecutado (Método Probado)")

        Log.i(TAG, "---- FIN DE PRUEBAS ----")
        
        // Limpieza visual: volvemos a cargar la lista real en pantalla
        actualizarListaUsuariosUI()
    }
```