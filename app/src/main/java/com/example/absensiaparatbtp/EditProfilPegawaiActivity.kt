package com.example.absensiaparatbtp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.absensiaparatbtp.firebase.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

class EditProfilPegawaiActivity : AppCompatActivity() {

    // Sekarang berupa UID Firebase (teks), bukan lagi angka Room
    private var userId: String = ""
    private lateinit var etNama: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etNoTelp: EditText
    private lateinit var etJabatan: EditText
    private lateinit var etJK: EditText
    private lateinit var etAlamat: EditText
    private lateinit var btnSimpan: Button
    private lateinit var btnHapus: Button
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profil_pegawai)

        userId = intent.getStringExtra("USER_ID") ?: ""

        etNama = findViewById(R.id.etNama)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etNoTelp = findViewById(R.id.etNoTelp)
        etJabatan = findViewById(R.id.etJabatan)
        etJK = findViewById(R.id.etJK)
        etAlamat = findViewById(R.id.etAlamat)
        btnSimpan = findViewById(R.id.btnSimpanProfil)
        btnHapus = findViewById(R.id.btnHapusAkun)

        firestore = FirebaseFirestore.getInstance()

        if (userId.isEmpty()) {
            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Password sekarang dikelola oleh Firebase Authentication, BUKAN lagi
        // disimpan sebagai teks di database seperti sebelumnya (jauh lebih aman).
        // Konsekuensinya: password tidak bisa dilihat/diubah dari layar admin ini
        // (butuh Firebase Admin SDK di server untuk itu, di luar cakupan aplikasi
        // ini). Field ini sengaja dikunci supaya tidak menyesatkan.
        etPassword.setText("(dikelola oleh sistem, tidak bisa diedit di sini)")
        etPassword.isEnabled = false

        // Isi awal
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(UserProfile::class.java)
                if (user != null) {
                    etNama.setText(user.nama)
                    etUsername.setText(user.username)
                    etNoTelp.setText(user.noHp)
                    etJabatan.setText(user.jabatan)
                    etJK.setText(user.jenisKelamin)
                    etAlamat.setText(user.alamat)
                } else {
                    Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal ambil data: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }

        // Simpan perubahan (username TIDAK diubah di sini karena berkaitan
        // langsung dengan email login di Firebase Auth -- perlu alur terpisah
        // yang lebih hati-hati kalau memang dibutuhkan nanti)
        btnSimpan.setOnClickListener {
            val updates = hashMapOf<String, Any>(
                "nama" to etNama.text.toString(),
                "noHp" to etNoTelp.text.toString(),
                "jabatan" to etJabatan.text.toString(),
                "jenisKelamin" to etJK.text.toString(),
                "alamat" to etAlamat.text.toString()
            )

            firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profil diperbarui", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Gagal menyimpan: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        // Hapus profil
        btnHapus.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Akun")
                .setMessage(
                    "Yakin ingin menghapus profil pegawai ini? Data profil & riwayat " +
                            "tidak bisa dikembalikan.\n\nCatatan: akun login pegawai ini tetap " +
                            "aktif di sistem (perlu dihapus manual lewat Firebase Console > " +
                            "Authentication kalau ingin login-nya benar-benar dinonaktifkan)."
                )
                .setPositiveButton("Hapus") { _, _ ->
                    firestore.collection("users").document(userId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profil dihapus", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Gagal menghapus: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}