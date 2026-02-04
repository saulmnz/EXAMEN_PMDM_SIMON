package com.example.examen_pmdm_simon.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.examen_pmdm_simon.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyViewModel(application: Application) : AndroidViewModel(application) {

    // 1. INSTANCIAMOS LA BASE DE DATOS ROOM
    private val database = SimonDatabase.getDatabase(application)
    private val dao = database.simonDao()
    var fechaRecord by mutableStateOf("Cargando...")



    // ESTADOS REACTIVOS (La UI se repinta sola cuando cambian)
    var ronda by mutableStateOf(0)
    var recordEnMemoria by mutableStateOf(0)
    var estadoActual by mutableStateOf(EstadoJuego.INICIO)
    var colorIluminado by mutableStateOf<Colores?>(null)

    private val secuenciaSimon = mutableListOf<Colores>()
    private var indiceUsuario = 0


    init {
        // ROOM REQUIERE CORRUTINAS (NO SE PUEDE HACER EN EL HILO PRINCIPAL)
        viewModelScope.launch(Dispatchers.IO) {
            // OBTENEMOS LA RONDA MÁXIMA, PUEDE SER NULL SI NO HAY RECORD
            val maxRonda = dao.getMaxRonda() ?: 0
            val fecha = if (maxRonda > 0) {
                dao.getFechaByRonda(maxRonda) ?: "Sin fecha"
            } else {
                "Sin fecha"
            }

            // VOLVEMOS AL HILO PRINCIPAL PARA ACTUALIZAR LA UI
            withContext(Dispatchers.Main) {
                recordEnMemoria = maxRonda
                fechaRecord = fecha
                Log.d("ROOM_SIMON", "DATOS CARGADOS: Récord $maxRonda - Fecha $fecha")
            }
        }
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
        if (ronda > recordEnMemoria) {
            recordEnMemoria = ronda

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaActual = sdf.format(Date())
            fechaRecord = fechaActual

            // LANZAMOS UNA CORRUTINA PARA GUARDAR EN BASE DE DATOS
            viewModelScope.launch(Dispatchers.IO) {
                val nuevoRecord = RecordEntity(ronda = ronda, fecha = fechaActual)
                dao.insert(nuevoRecord)
                Log.d("ROOM_SIMON", "NUEVO RECORD GUARDADO EN ROOM: $ronda ($fechaActual)")
            }
        }
    }
}