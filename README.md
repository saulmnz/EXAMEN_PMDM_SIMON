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