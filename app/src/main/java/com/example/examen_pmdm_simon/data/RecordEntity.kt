package com.example.examen_pmdm_simon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// @ENITY LE DICE A ROOM QUE ES UNA TABLA LLAMADA tabla_records
@Entity(tableName = "tabla_records")
data class RecordEntity(
    // @PrimaryKey: CLAVE PRIMARIA DE LA TABLA
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ronda: Int,
    val fecha: String
)