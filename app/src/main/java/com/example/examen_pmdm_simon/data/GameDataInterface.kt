package com.example.examen_pmdm_simon.data

import android.content.Context

interface GameDataInterface {
    // USUARIOS
    fun crearUsuario(nombre: String, context: Context)
    fun leerTodosLosUsuarios(context: Context): List<String>
    fun borrarUsuario(id: Int, context: Context)
    fun borrarTodo(context: Context)

    // RÉCORDS
    fun guardarRecord(puntos: Int, fecha: String, context: Context)
    fun leerMaximoRecord(context: Context): Int
    fun leerFechaRecord(puntos: Int, context: Context): String

    // RANKING
    fun obtenerRanking(context: Context): List<String>
}