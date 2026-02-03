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