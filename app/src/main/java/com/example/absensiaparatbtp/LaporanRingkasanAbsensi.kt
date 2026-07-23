package com.example.absensiaparatbtp

data class LaporanRingkasanAbsensi(
    val namaPegawai: String,
    val masuk: Int,
    val izin: Int,
    val sakit: Int,
    val keluar: Int
)