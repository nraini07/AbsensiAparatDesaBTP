// AbsensiEntity.kt
package com.example.absensiaparatbtp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "absensi")
data class AbsensiEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val namaPegawai: String,
    val tanggal: String,        // format dd-MM-yyyy
    val waktu: String,          // format HH:mm
    val jenisAbsensi: String,   // Masuk / Sakit / Izin / Keluar
    val lokasi: String,         // alamat / teks lokasi
    val keterangan: String,     // keterangan tambahan
    val latitude: Double?,      // lat posisi saat absensi
    val longitude: Double?,     // lng posisi saat absensi
    val keteranganAreaTugas: String? = null // status area tugas
)