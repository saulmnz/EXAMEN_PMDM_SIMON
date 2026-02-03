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