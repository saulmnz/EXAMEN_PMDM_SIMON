package com.example.examen_pmdm_simon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// LISTAMOS LAS ENTIDADES (TABLAS)
// PARA METER MÁS TABLAS, SIMPLEMENTE SEPARARLAS POR COMAS DENTRO DE LOS CORCHETES
@Database(entities = [RecordEntity::class], version = 1)
abstract class SimonDatabase : RoomDatabase() {

    abstract fun simonDao(): SimonDao

    companion object {
        @Volatile
        private var INSTANCE: SimonDatabase? = null

        fun getDatabase(context: Context): SimonDatabase {
            // PATRÓN SINGLETON PARA LA BASE DE DATOS, SI YA EXISTE LA DEVUELVE, SINO, LA CREA
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SimonDatabase::class.java,
                    "simon_room_db" // NOMBRE DE LA BASE DE DATOS
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}