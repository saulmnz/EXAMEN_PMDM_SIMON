package com.example.examen_pmdm_simon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Importante para collectAsState y getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.examen_pmdm_simon.data.Colores
import com.example.examen_pmdm_simon.data.EstadoJuego

@Composable
fun PantallaSimon(viewModel: MyViewModel) {

    // -------------------------------------------------------------
    // ¡OJO AQUÍ! - RECOLECTAMOS LOS ESTADOS DEL VIEWMODEL (StateFlow)
    // -------------------------------------------------------------
    val record by viewModel.recordEnMemoria.collectAsState()
    val ronda by viewModel.ronda.collectAsState()
    val estado by viewModel.estadoActual.collectAsState()

    // Si quisieras ver los usuarios o ranking también:
    // val ranking by viewModel.rankingTexto.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Usamos las variables locales 'record' y 'ronda' (no viewModel.record...)
        Text(text = "Récord: $record", fontSize = 20.sp)
        Text(text = "Ronda actual: $ronda", fontSize = 34.sp)

        Spacer(modifier = Modifier.height(30.dp))

        // Botones en cuadrícula 2x2
        Row {
            BotonColor(Colores.VERDE, viewModel)
            BotonColor(Colores.ROJO, viewModel)
        }
        Row {
            BotonColor(Colores.AMARILLO, viewModel)
            BotonColor(Colores.AZUL, viewModel)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Botón de control (Usamos la variable 'estado')
        if (estado == EstadoJuego.INICIO || estado == EstadoJuego.GAME_OVER) {
            Button(
                onClick = { viewModel.iniciarJuego() },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = if (estado == EstadoJuego.INICIO) "EMPEZAR" else "REINTENTAR")
            }
        }

        if (estado == EstadoJuego.GAME_OVER) {
            Text(text = "¡TE HAS EQUIVOCADO!", color = androidx.compose.ui.graphics.Color.Red)
        }
    }
}

@Composable
fun BotonColor(colorEnum: Colores, viewModel: MyViewModel) {
    // RECOLECTAMOS EL COLOR ILUMINADO AQUÍ TAMBIÉN
    val colorIluminado by viewModel.colorIluminado.collectAsState()

    // Comparamos la variable recolectada con el color de este botón
    val alpha = if (colorIluminado == colorEnum) 1f else 0.3f

    Button(
        onClick = { viewModel.respuestaUsuario(colorEnum) },
        colors = ButtonDefaults.buttonColors(
            // Usamos colorReal (que definimos en el Enum) con la opacidad calculada
            containerColor = colorEnum.colorReal.copy(alpha = alpha)
        ),
        modifier = Modifier
            .size(140.dp)
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium
    ) {}
}