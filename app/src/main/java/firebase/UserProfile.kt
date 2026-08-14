package com.example.absensiaparatbtp.firebase

/**
 * Model profil pegawai yang disimpan di Firestore, koleksi "users".
 * Document ID = UID dari Firebase Authentication.
 *
 * PENTING: semua properti punya nilai default (string kosong) karena Firestore
 * butuh constructor kosong untuk otomatis mengubah dokumen jadi objek Kotlin.
 */
data class UserProfile(
    val uid: String = "",
    val nama: String = "",
    val noHp: String = "",
    val jenisKelamin: String = "",
    val jabatan: String = "",
    val alamat: String = "",
    val username: String = "",
    val email: String = ""   // email ASLI pegawai, dipakai untuk reset password
)

/**
 * Model kecil untuk koleksi "usernames" -- semacam "kamus" username -> uid & email.
 * Dipakai supaya pegawai bisa login pakai USERNAME (bukan email), dan supaya
 * fitur Lupa Password bisa cari email asli dari username yang diketik.
 *
 * Koleksi ini SENGAJA dipisah dari "users" (yang isinya profil lengkap & butuh
 * login untuk dibaca), supaya proses LOGIN & LUPA PASSWORD bisa jalan walau
 * pengguna belum ter-autentikasi sama sekali (ayam-telur: butuh baca data buat
 * bisa login, tapi belum bisa login sebelum baca data).
 *
 * Document ID = username (huruf kecil semua, supaya konsisten).
 */
data class UsernameMapping(
    val uid: String = "",
    val email: String = ""
)

fun normalizeUsername(username: String): String {
    return username.lowercase().trim()
}

/**
 * KHUSUS dipakai untuk akun ADMIN (bukan pegawai lagi). Admin tidak perlu
 * fitur lupa-password lewat email asli, jadi tetap pakai trik "email palsu"
 * supaya tidak perlu mendaftarkan email sungguhan untuk akun admin.
 */
fun usernameToPseudoEmail(username: String): String {
    return "${username.lowercase().trim()}@absensidesabtp.local"
}