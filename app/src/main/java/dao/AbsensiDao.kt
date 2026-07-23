package com.example.absensiaparatbtp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AbsensiDao {

    @Insert
    suspend fun insertAbsensi(absensi: AbsensiEntity)

    @Query("SELECT * FROM absensi WHERE userId = :userId ORDER BY id DESC")
    suspend fun getRiwayatByUser(userId: Int): List<AbsensiEntity>

    @Query("""
        SELECT DISTINCT substr(tanggal, 4, 7) AS bulan 
        FROM absensi 
        WHERE userId = :userId 
        ORDER BY bulan
    """)
    suspend fun getBulanDenganData(userId: Int): List<String>

    @Query("""
        SELECT * FROM absensi 
        WHERE userId = :userId 
          AND substr(tanggal, 4, 7) = :bulan 
        ORDER BY id DESC
    """)
    suspend fun getRiwayatByUserAndBulan(userId: Int, bulan: String): List<AbsensiEntity>

    @Query("""
        SELECT * FROM absensi
        WHERE userId = :userId
          AND tanggal = :tanggal
        ORDER BY id DESC
    """)
    suspend fun getRiwayatByUserAndTanggal(userId: Int, tanggal: String): List<AbsensiEntity>

    // ✅ QUERY BARU UNTUK ADMIN
    @Query("SELECT * FROM absensi")
    suspend fun getAllAbsensi(): List<AbsensiEntity>

    @Query("SELECT * FROM absensi WHERE namaPegawai = :namaPegawai")
    suspend fun getAbsensiByNamaPegawai(namaPegawai: String): List<AbsensiEntity>
}