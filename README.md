# SHAREDPREFERENCES 🦠

> [!NOTE]
> ***Guarda datos de forma ligera en android usando un sistema de clave-valor***

- ***CLAVE: Un string que identifica el dato.***
- ***VALOR: El dato real que se guarda, puede ser de varios tipos (String, Int, Boolean, etc).***
- ***APPLY(): Realiza el guardado de forma asíncrona en segundo plano, evitando bloquear la interfaz de usuario***

 ## CÓMO LO IMPLEMENTAS:

> [!TIP]
> ***Esto es todo el código implementado en la clase viewmodel base***

```kotlin
class MyViewModel(application: Application) : AndroidViewModel(application) {

    // SHARED PREFERENCES
    // NOMBRE DEL ARCHIVO
    private val PREFS_NAME = "simon_dice_prefs"
    
    // CLAVES PARA IDENTIFICAR LOS DATOS QUE GUARDAREMOS
    private val KEY_RECORD = "ronda_mas_alta"
    private val KEY_FECHA = "fecha_record"
    private val KEY_MARCA_TIEMPO = "marca_tiempo"

    // ACCEDER AL OBJETO DE SHARED PREFERENCES
    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // NUEVO ESTADO PARA MOSTRAR LA FECHA DEL RÉCORD - ESTADO REACTIVO
    var fechaRecord by mutableStateOf("")
    
    
    // INICIALIZACIÓN
    init{
        // LLAMAMOS AL MÉTODO PARA RECUPERAR LOS DATOS GUARDADOS EN CUANTO SE CREA EL VIEWMODEL
        cargarDatos()
    }
    

    private fun cargarDatos(){
        // LEEMOS EL VALOR DEL RÉCORD, SI NO EXISTE SE ASIGNA 0 POR DEFECTO
        recordEnMemoria = sharedPreferences.getInt(KEY_RECORD, 0)
        // LEEMOS LA FECHA DEL RÉCORD, SI NO EXISTE SE ASIGNA "Sin fecha" POR DEFECTO
        fechaRecord = sharedPreferences.getString(KEY_FECHA, "Sin fecha") ?: "Sin fecha"
    }

    private fun guardarEnPrefs(puntuacion: Int, fecha: String){
        // OBTENEMOS EL EDITOR PARA PODER ESCRIBIR EN EL ARCHIVO DE PREFERENCIAS
        val editor = sharedPreferences.edit()
        // INSERTAMOS EL NUEVO RÉCORD ASOCIADO A SU CLAVE
        editor.putInt(KEY_RECORD, puntuacion)
        // INSERTAMOS LA FECHA ASOCIADA A SU CLAVE
        editor.putString(KEY_FECHA, fecha)
        // INSERTAMOS LA MARCA DE TIEMPO ACTUAL ASOCIADA A SU CLAVE
        editor.putLong(KEY_MARCA_TIEMPO, System.currentTimeMillis())
        // GUARDAMOS LOS CAMBIOS DE FORMA ASÍNCRONA PARA NO BLOQUEAR EL HILO PRINCIPAL
        editor.apply()
    }
    
    // RESTO LÓGICA DEL VIEWMODEL...

    
    // AL FINAL...
    private fun actualizarRecord() {

        // COMPROBAMOS SI LA RONDA ACTUAL SUPERA AL RÉCORD GUARDADO EN MEMORIA
        if (ronda > recordEnMemoria) {
            // ACTUALIZAMOS EL VALOR EN MEMORIA PARA QUE LA UI SE REFRESQUE AL INSTANTE
            recordEnMemoria = ronda

            // OBTENEMOS LA FECHA ACTUAL FORMATEADA
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaActual = sdf.format(Date())

            // ACTUALIZAMOS LA FECHA EN MEMORIA PARA QUE LA UI SE REFRESQUE AL INSTANTE
            fechaRecord = fechaActual

            // GUARDAMOS LOS NUEVOS DATOS EN SHARED PREFERENCES
            guardarEnPrefs(recordEnMemoria, fechaActual)
        }
    }
}

```

---

> [!TIP]
> ***Resto del código del viewmodel base para el juego simon dice antes de implementarle shared preference***

```kotlin

// ESTADOS REACTIVOS (La UI se repinta sola cuando cambian)
var ronda by mutableStateOf(0)
var recordEnMemoria by mutableStateOf(0)
var estadoActual by mutableStateOf(EstadoJuego.INICIO)
var colorIluminado by mutableStateOf<Colores?>(null)
private val secuenciaSimon = mutableListOf<Colores>()
private var indiceUsuario = 0

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

```

# CHEEECK 🦭

---

### NOTAS 🦛

- ***RUTA: data/data/tu.paquete/shared_prefs/archivo.xml***
- ***CREAR NUEVO VALOR CON EDITOT.putInt, putString....***
- ***COMPROBAR QUE SE CREA BIEN USANDO DEVICE FILE EXPLORER***
- ***ACTUALIZAR: SOBREESCRIBIR UN VALOR EXISTENTE, CAMBIAR NOMBRE DE USUARIO***
- ***GUARDAR VARIAS CLAVES DISTINTAS***

---

### EJERCICIO 🐨

1. ***Guardar el Nombre del Jugador (String).***

2. ***Guardar el Récord Máximo (Int).***

3. ***Guardar el Total de Partidas Jugadas (Int) -> Esto cubre "actualizar" un contador.***

```kotlin

// SHARED PREFERENCES EN EL VIEWMODEL BASE
class MyViewModel(application: Application) : AndroidViewModel(application) {

    // DEFINIMOS EL NOMBRE
    private val PREFS_NAME = "SimonPerfil"

    // DEFINIMOS CLAVES
    private val KEY_RECORD = "record_maximo"
    private val KEY_NOMBRE = "nombre_usuario"
    private val KEY_TOTAL_PARTIDAS = "total_partidas"
    
    // ACCEDER AL OBJETO DE SHARED PREFERENCES
    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // NUEVOS ESTADOS 
    var nombreUsuario by mutableStateOf("Jugador 1")
    var totalPartidas by mutableStateOf(0)

    // MÉTODO PARA CARGAR DATOS
    init {
        cargarDatos()
    }

    // MÉTODO PARA CARGAR DATOS DESDE SHARED PREFERENCES
    private fun cargarDatos() {
        recordEnMemoria = sharedPreferences.getInt(KEY_RECORD, 0)

        // CARGAMOS EL NOMBRE, SI NO EXISTE ASIGNAMOS "Jugador 1" POR DEFECTO
        nombreUsuario = sharedPreferences.getString(KEY_NOMBRE, "Jugador 1") ?: "Jugador 1"

        // CARGAMOS EL CONTADOR DE PARTIDAS
        totalPartidas = sharedPreferences.getInt(KEY_TOTAL_PARTIDAS, 0)

        // LOG PARA COMPROBAR QUE SE CREA BIEN
        Log.d("SIMON_PREFS", "DATOS CARGADOS -> USER: $nombreUsuario | RECORD: $recordEnMemoria | PARTIDAS: $totalPartidas")
    }

    // MÉTODO PARA INCREMENTAR EL CONTADOR DE PARTIDAS
    fun incrementarPartidas() {
        totalPartidas++
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_TOTAL_PARTIDAS, totalPartidas)
        editor.apply()
        Log.d("SIMON_PREFS", "PARTIDA FINALIZADA. TOTAL ACUMULADO: $totalPartidas")
    }

    // MÉTODO PARA ACTUALIZAR EL RÉCORD
    private fun actualizarRecord() {
        if (ronda > recordEnMemoria) {
            recordEnMemoria = ronda
            val editor = sharedPreferences.edit()
            editor.putInt(KEY_RECORD, recordEnMemoria)
            editor.apply()
            Log.d("SIMON_PREFS", "NUEVO RECORD GUARDADO: $recordEnMemoria")
        }
    }
    
    // RESPUESTA DEL USUARIO MODIFICADA
    fun respuestaUsuario(colorPulsado: Colores) {
        if (estadoActual != EstadoJuego.ESPERANDO) return

        if (colorPulsado == secuenciaSimon[indiceUsuario]) {
            indiceUsuario++
            if (indiceUsuario == secuenciaSimon.size) {
                actualizarRecord()
                siguienteRonda()
            }
        } else {
            // AL PERDER, ACTUALIZAMOS EL CONTADOR DE PARTIDAS
            incrementarPartidas()
            estadoActual = EstadoJuego.GAME_OVER
        }
    }
}
    

```


### EJEMPLO DE VARIOS RECORDS RANKING

```kotlin
class MyViewModel(application: Application) : AndroidViewModel(application) {

    private val PREFS_NAME = "SimonRanking"
    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // CLAVES PARA LOS 3 LUGARES DEL PODIO
    private val KEY_TOP_1 = "ranking_1"
    private val KEY_TOP_2 = "ranking_2"
    private val KEY_TOP_3 = "ranking_3"

    // VARIABLES PARA MOSTRAR EN LA UI (SI QUISIERAS PINTAR EL RANKING)
    var top1 by mutableStateOf(0)
    var top2 by mutableStateOf(0)
    var top3 by mutableStateOf(0)
    
    var ronda by mutableStateOf(0)
    // ... otros estados ...

    init {
        cargarRanking()
    }

    private fun cargarRanking() {
        // LEEMOS LAS 3 CLAVES, SI NO EXISTEN SE ASIGNA 0 POR DEFECTO
        top1 = sharedPreferences.getInt(KEY_TOP_1, 0)
        top2 = sharedPreferences.getInt(KEY_TOP_2, 0)
        top3 = sharedPreferences.getInt(KEY_TOP_3, 0)
    }

    // ESTA ES LA FUNCIÓN CLAVE 
    private fun actualizarRanking(puntuacionActual: Int) {
        val editor = sharedPreferences.edit()

        // LÓGICA DE DESPLAZAMIENTO (WATERFALL)
        if (puntuacionActual > top1) {
            
            // CASO 1: SUPERA EL PRIMERO
            // EL 2 PASA AL 3, EL 1 PASA AL 2, Y EL NUEVO SE PONE EN EL 1.
            
            // GUARDAMOS EN DISCO
            editor.putInt(KEY_TOP_3, top2)
            editor.putInt(KEY_TOP_2, top1)
            editor.putInt(KEY_TOP_1, puntuacionActual)
            
            // ACTUALIZAMOS MEMORIA
            top3 = top2
            top2 = top1
            top1 = puntuacionActual
            
        } else if (puntuacionActual > top2) {
            
            // CASO 2: SUPERA AL SEGUNDO PERO NO AL PRIMERO
            // EL 2 PASA AL 3, Y EL NUEVO SE PONE EN EL 2.
            
            editor.putInt(KEY_TOP_3, top2)
            editor.putInt(KEY_TOP_2, puntuacionActual)
            
            top3 = top2
            top2 = puntuacionActual
            
        } else if (puntuacionActual > top3) {
            
            // CASO 3: SUPERA AL TERCERO PERO NO A LOS OTROS DOS
            
            editor.putInt(KEY_TOP_3, puntuacionActual)
            
            top3 = puntuacionActual
        }

        // APLICAMOS LOS CAMBIOS
        editor.apply()
    }

    // EN LA LÓGICA DEL JUEGO
    fun respuestaUsuario(colorPulsado: Colores) {
        
        if (fallo) { // SI PIERDE
            // AL ACABAR LA PARTIDA ACTUALIZAMOS EL RANKING
            actualizarRanking(ronda)
            estadoActual = EstadoJuego.GAME_OVER
        }
    }
}
```

----

### CLASE COMPLETA RANKING 

```kotlin
package com.example.examen_pmdm_simon.ui
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.examen_pmdm_simon.data.Colores
import com.example.examen_pmdm_simon.data.EstadoJuego
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// EXAMEN: EXTENDEMOS DE ANDROIDVIEWMODEL PARA TENER ACCESO AL 'APPLICATION' (CONTEXTO)
class MyViewModel(application: Application) : AndroidViewModel(application) {

    // --- CONFIGURACIÓN SHARED PREFERENCES ---

    // EXAMEN: RUTA -> DATA/DATA/COM.EXAMPLE.../SHARED_PREFS/SIMONRANKING.XML
    private val PREFS_NAME = "SimonRanking"
    // INSTANCIAMOS EL OBJETO SHAREDPREFERENCES EN MODO PRIVADO
    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // EXAMEN: "VARIOS RECORDS" -> DEFINIMOS 3 CLAVES DISTINTAS PARA SIMULAR UN RANKING
    private val KEY_TOP_1 = "ranking_1"
    private val KEY_TOP_2 = "ranking_2"
    private val KEY_TOP_3 = "ranking_3"

    // --- ESTADOS DE LA UI ---

    // VARIABLES PARA MOSTRAR EL PODIO EN LA PANTALLA (TOP 3)
    var top1 by mutableStateOf(0)
    var top2 by mutableStateOf(0)
    var top3 by mutableStateOf(0)

    // ESTADOS DEL JUEGO
    var ronda by mutableStateOf(0)
    var estadoActual by mutableStateOf(EstadoJuego.INICIO)
    var colorIluminado by mutableStateOf<Colores?>(null)

    private val secuenciaSimon = mutableListOf<Colores>()
    private var indiceUsuario = 0

    // BLOQUE INIT: SE EJECUTA AUTOMÁTICAMENTE AL ABRIR LA PANTALLA
    init {
        cargarRanking()
    }

    // --- LÓGICA DE SHARED PREFERENCES ---

    private fun cargarRanking() {
        // EXAMEN: COMPROBAR QUE SE CREA BIEN / LEER
        // LEEMOS LAS 3 CLAVES. SI NO EXISTEN, DEVUELVE 0 POR DEFECTO.
        top1 = sharedPreferences.getInt(KEY_TOP_1, 0)
        top2 = sharedPreferences.getInt(KEY_TOP_2, 0)
        top3 = sharedPreferences.getInt(KEY_TOP_3, 0)

        Log.d("SIMON_PREFS", "RANKING CARGADO: 1º[$top1] - 2º[$top2] - 3º[$top3]")
    }

    // EXAMEN: ACTUALIZAR Y CREAR NUEVO VALOR
    // ESTA FUNCIÓN RECIBE LA PUNTUACIÓN FINAL Y CALCULA SI ENTRA EN EL PODIO
    private fun actualizarRanking(puntuacion: Int) {
        val editor = sharedPreferences.edit()
        var huboCambios = false

        // LÓGICA DE DESPLAZAMIENTO (WATERFALL / CASCADA)
        if (puntuacion > top1) {
            Log.d("SIMON_PREFS", "¡NUEVO RÉCORD ABSOLUTO! $puntuacion SUPERA A $top1")

            // EL ANTIGUO 2º PASA A SER 3º
            editor.putInt(KEY_TOP_3, top2)
            top3 = top2

            // EL ANTIGUO 1º PASA A SER 2º
            editor.putInt(KEY_TOP_2, top1)
            top2 = top1

            // EL NUEVO VALOR SE PONE EN EL 1º
            editor.putInt(KEY_TOP_1, puntuacion)
            top1 = puntuacion

            huboCambios = true

        } else if (puntuacion > top2) {
            Log.d("SIMON_PREFS", "¡ENTRA EN EL TOP 2! $puntuacion SUPERA A $top2")

            // EL ANTIGUO 2º PASA A SER 3º
            editor.putInt(KEY_TOP_3, top2)
            top3 = top2

            // EL NUEVO VALOR SE PONE EN EL 2º
            editor.putInt(KEY_TOP_2, puntuacion)
            top2 = puntuacion

            huboCambios = true

        } else if (puntuacion > top3) {
            Log.d("SIMON_PREFS", "¡ENTRA EN EL TOP 3! $puntuacion SUPERA A $top3")

            // EL NUEVO VALOR SE PONE EN EL 3º
            editor.putInt(KEY_TOP_3, puntuacion)
            top3 = puntuacion

            huboCambios = true
        }

        if (huboCambios) {
            // EXAMEN: GUARDAR LOS CAMBIOS EN EL ARCHIVO XML FÍSICO
            editor.apply()
            Log.d("SIMON_PREFS", "RANKING ACTUALIZADO Y GUARDADO EN XML.")
        } else {
            Log.d("SIMON_PREFS", "LA PUNTUACIÓN $puntuacion NO ENTRA EN EL RANKING (MÍNIMO A SUPERAR: $top3)")
        }
    }

    // --- LÓGICA DEL JUEGO SIMON ---

    fun iniciarJuego() {
        secuenciaSimon.clear()
        ronda = 0
        siguienteRonda()
    }

    private fun siguienteRonda() {
        indiceUsuario = 0
        ronda++ // INCREMENTAMOS RONDA
        secuenciaSimon.add(Colores.values().random()) // AÑADIMOS UN COLOR ALEATORIO
        reproducirSecuencia()
    }

    private fun reproducirSecuencia() {
        viewModelScope.launch {
            estadoActual = EstadoJuego.REPRODUCIENDO
            delay(500) // PEQUEÑA PAUSA INICIAL

            for (color in secuenciaSimon) {
                colorIluminado = color
                // VELOCIDAD DE MUESTRA DEL COLOR (EJ. 500MS)
                delay(500L)
                colorIluminado = null
                delay(250L) // PAUSA ENTRE COLORES
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
                // RONDA COMPLETADA CON ÉXITO
                siguienteRonda()
            }
        } else {
            // FALLO -> GAME OVER
            estadoActual = EstadoJuego.GAME_OVER

            // EXAMEN: AQUÍ ES DONDE LLAMAMOS A GUARDAR EL DATO AL PERDER
            // PASAMOS LA RONDA CONSEGUIDA PARA VER SI ENTRA EN EL TOP 3
            actualizarRanking(ronda)
        }
    }
}
```