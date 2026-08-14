package com.example.absensiaparatbtp.firebase

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Model data absensi yang disimpan di Firestore, koleksi "absensi".
 * Setiap dokumen mewakili satu kali absen (Masuk/Sakit/Izin/Keluar).
 *
 * Document ID dibiarkan otomatis (auto-generate) karena satu pegawai bisa
 * punya banyak record absensi dari waktu ke waktu -- beda dengan "users"
 * yang document ID-nya sengaja disamakan dengan uid (1 pegawai = 1 profil).
 *
 * createdAt otomatis diisi Firestore server saat dokumen dibuat (server
 * timestamp), berguna untuk urutan data yang akurat & tidak bisa dipalsukan
 * dari sisi HP pegawai (beda dengan field tanggal/waktu yang sifatnya teks
 * dan bisa saja jam HP-nya salah setel).
 */
data class AbsensiRecord(
    val userId: String = "",
    val namaPegawai: String = "",
    val tanggal: String = "",       // format dd-MM-yyyy, ditampilkan ke user
    val waktu: String = "",         // format HH:mm, ditampilkan ke user
    val jenisAbsensi: String = "",  // Masuk / Sakit / Izin / Keluar
    val lokasi: String = "",
    val keterangan: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val akurasiMeter: Float? = null,
    val keteranganAreaTugas: String? = null,
    @ServerTimestamp val createdAt: Date? = null
)
