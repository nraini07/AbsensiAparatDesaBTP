// UserEntity.kt
package com.example.absensiaparatbtp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val noHp: String,
    val jenisKelamin: String,
    val jabatan: String,
    val alamat: String,
    val username: String,
    val password: String
)