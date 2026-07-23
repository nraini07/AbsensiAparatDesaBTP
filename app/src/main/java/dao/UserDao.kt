// UserDao.kt
package com.example.absensiaparatbtp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): UserEntity?

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE nama LIKE :nama")
    suspend fun searchByNama(nama: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE noHp = :noHp LIMIT 1")
    suspend fun getUserByNoHp(noHp: String): UserEntity?

    // 🔹 TAMBAHAN: ambil 1 user berdasarkan nama, untuk kirim jabatan ke halaman detail
    @Query("SELECT * FROM users WHERE nama = :nama LIMIT 1")
    suspend fun getUserByNama(nama: String): UserEntity?

    // 🔹 TAMBAHAN: ambil 1 user berdasarkan ID (untuk edit / hapus)
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    // 🔹 TAMBAHAN: update data user
    @Update
    suspend fun updateUser(user: UserEntity)

    // 🔹 TAMBAHAN: hapus user
    @Delete
    suspend fun deleteUser(user: UserEntity)
}