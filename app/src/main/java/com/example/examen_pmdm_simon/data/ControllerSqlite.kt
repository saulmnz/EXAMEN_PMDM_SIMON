package com.example.examen_pmdm_simon.data

import android.content.Context
import android.util.Log

// Singleton OBJECT: Se instancia solo una vez
object ControllerSQLite : GameDataInterface {

    private const val TAG = "SIMON_CONTROLLER"
    private var dbHelper: SimonDatabaseHelper? = null

    private fun getDb(context: Context): SimonDatabaseHelper {
        if (dbHelper == null) dbHelper = SimonDatabaseHelper(context)
        return dbHelper!!
    }

    override fun crearUsuario(nombre: String, context: Context) {
        Log.d(TAG, "LOGICA: Insertando usuario $nombre en BD")
        getDb(context).insertarUsuario(nombre)
    }

    override fun leerTodosLosUsuarios(context: Context): List<String> {
        Log.d(TAG, "LOGICA: Leyendo lista de usuarios...")
        return getDb(context).obtenerTodosLosUsuarios()
    }

    override fun borrarUsuario(id: Int, context: Context) {
        Log.d(TAG, "LOGICA: Borrando usuario ID $id")
        getDb(context).borrarUsuarioPorId(id)
    }

    override fun borrarTodo(context: Context) {
        Log.d(TAG, "LOGICA: Borrando TODAS las tablas")
        getDb(context).borrarTodosLosUsuarios()
    }

    override fun guardarRecord(puntos: Int, fecha: String, context: Context) {
        Log.d(TAG, "LOGICA: Guardando nuevo récord ($puntos)")
        getDb(context).insertarRecord(puntos, fecha)
    }

    override fun leerMaximoRecord(context: Context): Int {
        return getDb(context).obtenerMaximoRecord()
    }

    override fun leerFechaRecord(puntos: Int, context: Context): String {
        return getDb(context).obtenerFechaDelRecord(puntos)
    }

    override fun obtenerRanking(context: Context): List<String> {
        Log.d(TAG, "LOGICA: Generando Ranking Top 10")
        // Lógica movida aquí para no ensuciar el ViewModel
        val ranking = ArrayList<String>()
        val db = getDb(context).readableDatabase
        val cursor = db.rawQuery("SELECT ronda, fecha FROM historico_records ORDER BY ronda DESC LIMIT 10", null)

        var pos = 1
        if (cursor.moveToFirst()) {
            do {
                ranking.add("$pos. ${cursor.getInt(0)} pts (${cursor.getString(1)})")
                pos++
            } while (cursor.moveToNext())
        }
        cursor.close()
        return ranking
    }
}