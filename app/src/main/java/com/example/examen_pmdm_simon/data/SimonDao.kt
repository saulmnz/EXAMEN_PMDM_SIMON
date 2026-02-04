package com.example.examen_pmdm_simon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SimonDao {

    // SUSPEND: USAMOS CORUTINAS PARA NO BLOQUEAR LA APP
    @Insert
    suspend fun insert(record: RecordEntity)

    // CONSULTA PARA OBTENER LA RONDA MÁS ALTA
    @Query("SELECT MAX(ronda) FROM tabla_records")
    suspend fun getMaxRonda(): Int? // PUEDE SER NULL SI LA TABLA ESTÁ VACÍA

    // CONSULTA PARA OBTENER LA FECHA DE UN RECODRD ESPECÍFICO (LA MÁS RECIENTE)
    @Query("SELECT fecha FROM tabla_records WHERE ronda = :ronda ORDER BY id DESC LIMIT 1")
    suspend fun getFechaByRonda(ronda: Int): String?

}