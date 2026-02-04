package com.example.examen_pmdm_simon.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.examen_pmdm_simon.data.* import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyViewModel(application: Application) : AndroidViewModel(application) {

    // --- ESTADOS DE UI (StateFlow) ---
    var ronda = MutableStateFlow(0)
    var estadoActual = MutableStateFlow(EstadoJuego.INICIO)
    var colorIluminado = MutableStateFlow<Colores?>(null)

    // --- ESTADOS DE DATOS ---
    var recordEnMemoria = MutableStateFlow(0)
    var listaUsuariosTexto = MutableStateFlow("")
    var rankingTexto = MutableStateFlow("")

    // Variables internas juego
    private val secuenciaSimon = mutableListOf<Colores>()
    private var indiceUsuario = 0

    init {
        // Carga inicial: Llamamos al Controlador y actualizamos Estados
        refreshDatos()
    }

    // =========================================================
    // MÉTODOS DE DATOS (PUENTES AL CONTROLADOR)
    // Fíjate: Solo 1 línea de código. Llaman al Controller y actualizan la UI.
    // =========================================================

    fun registrarUsuarioNuevo(nombre: String) {
        // ViewModel solo delega
        ControllerSQLite.crearUsuario(nombre, getApplication())
        refreshDatos() // Actualiza la pantalla
    }

    fun eliminarUsuario(id: Int) {
        ControllerSQLite.borrarUsuario(id, getApplication())
        refreshDatos()
    }

    fun borrarTodo() {
        ControllerSQLite.borrarTodo(getApplication())
        refreshDatos()
    }

    // Función privada para refrescar todo lo que viene de la BD
    private fun refreshDatos() {
        val context = getApplication<Application>()

        // El ViewModel pide los datos ya cocinados al Controlador
        recordEnMemoria.value = ControllerSQLite.leerMaximoRecord(context)

        val usuarios = ControllerSQLite.leerTodosLosUsuarios(context)
        listaUsuariosTexto.value = if (usuarios.isNotEmpty()) usuarios.joinToString("\n") else "Vacío"

        val ranking = ControllerSQLite.obtenerRanking(context)
        rankingTexto.value = if (ranking.isNotEmpty()) ranking.joinToString("\n") else "Sin Récords"
    }

    // =========================================================
    // MÉTODOS DEL JUEGO (LÓGICA DEL SIMÓN)
    // Esto sí pertenece al ViewModel porque controla el estado del juego
    // =========================================================

    fun iniciarJuego() {
        secuenciaSimon.clear()
        ronda.value = 0
        siguienteRonda()
    }

    private fun siguienteRonda() {
        ronda.value++
        indiceUsuario = 0
        secuenciaSimon.add(Colores.values().random())

        viewModelScope.launch {
            estadoActual.value = EstadoJuego.REPRODUCIENDO
            delay(500)
            for (color in secuenciaSimon) {
                colorIluminado.value = color
                delay(500)
                colorIluminado.value = null
                delay(200)
            }
            estadoActual.value = EstadoJuego.ESPERANDO
        }
    }

    fun respuestaUsuario(color: Colores) {
        if (estadoActual.value != EstadoJuego.ESPERANDO) return

        // Animación click
        viewModelScope.launch {
            colorIluminado.value = color
            delay(150)
            colorIluminado.value = null
        }

        if (color == secuenciaSimon[indiceUsuario]) {
            indiceUsuario++
            if (indiceUsuario == secuenciaSimon.size) {
                // Ronda superada -> Verificar Récord
                guardarSiEsRecord()
                viewModelScope.launch { delay(500); siguienteRonda() }
            }
        } else {
            estadoActual.value = EstadoJuego.GAME_OVER
        }
    }

    private fun guardarSiEsRecord() {
        if (ronda.value > recordEnMemoria.value) {
            val fecha = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())

            // LLAMADA AL CONTROLADOR PARA GUARDAR
            ControllerSQLite.guardarRecord(ronda.value, fecha, getApplication())

            refreshDatos() // Refrescamos pantallas
            Log.d("SIMON_VM", "Nuevo récord guardado a través del Controlador")
        }
    }
}