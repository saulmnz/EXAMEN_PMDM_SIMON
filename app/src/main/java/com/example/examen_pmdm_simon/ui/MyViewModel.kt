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


    // MOSTRAR LISTA DE USUARIOS DESDE SQLITE
    var listaUsuariosTexto by mutableStateOf("CARGANDO USUARIOS...")

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
        inicializarDatosPrueba()
        actualizarListaUsuariosUI()
        val pruebaId = dbHelper.obtenerRecordPorId(1)
        // Log.d("SQLITE_SIMON", "DATOS CARGADOS AL INICIO: Récord $recordEnMemoria ($fechaRecord)")
        Log.d("SQLITE_SIMON", "Prueba getRecordById(1): $pruebaId")

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
            // ACIERTO
            indiceUsuario++
            if (indiceUsuario == secuenciaSimon.size) {
                // SI SE COMPLETA LA RONDA, COMPROBAMOS SI HAY QUE GUARDAR RÉCORD
                actualizarRecord()
                siguienteRonda()
            }
        } else {
            // ERROR -> FIN DEL JUEGO
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

    // FUNCION AUXILIAR
    private fun inicializarDatosPrueba() {
        val usuarios = dbHelper.obtenerTodosLosUsuarios()
        if (usuarios.isEmpty()) {
            dbHelper.insertarUsuario("TESTER 1")
            dbHelper.insertarUsuario("PROFE 1")
            Log.d("SQLITE_SIMON", "Datos de prueba insertados en tabla_usuarios")
        }
    }

    fun actualizarListaUsuariosUI() {
        val lista = dbHelper.obtenerTodosLosUsuarios()
        // CONVERTIMOS LA LISTA [Usuario(1, "Pepe"), Usuario(2, "Juan")] EN ["1: Pepe", "2: Juan"] A STRING
        listaUsuariosTexto = if (lista.isNotEmpty()) lista.joinToString("\n") else "Sin usuarios"
    }

    // AGREGA UN USUARIO
    fun registrarUsuarioNuevo(nombre: String) {
        dbHelper.insertarUsuario(nombre)
        actualizarListaUsuariosUI() // REFRESCAMOS LA LISTA
    }

    fun eliminarUsuario(id: Int) {
        val borrados = dbHelper.borrarUsuarioPorId(id)
        if (borrados > 0) {
            actualizarListaUsuariosUI() // REFRESCAMOS PANTALLA
        }
    }

    // MËTODO NUEVO PARA PROBAR EL BORRADO TOTAL
    fun borrarTodosLosUsuarios() {
        dbHelper.borrarTodosLosUsuarios()
        actualizarListaUsuariosUI()
    }
}
