package com.example.absensiaparatbtp

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensiaparatbtp.firebase.UserProfile
import com.example.absensiaparatbtp.firebase.UsernameMapping
import com.example.absensiaparatbtp.firebase.normalizeUsername
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val etNama = findViewById<EditText>(R.id.etNama)
        val etnoHp = findViewById<EditText>(R.id.etnoHp)
        val etJenisKelamin = findViewById<EditText>(R.id.etJenisKelamin)
        val etJabatan = findViewById<EditText>(R.id.etJabatan)
        val etAlamat = findViewById<EditText>(R.id.etAlamat)
        val etUsername = findViewById<EditText>(R.id.etUsernameReg)
        val etEmail = findViewById<EditText>(R.id.etEmailReg)
        val etPassword = findViewById<EditText>(R.id.etPasswordReg)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val noHp = etnoHp.text.toString().trim()
            val jk = etJenisKelamin.text.toString().trim()
            val jabatan = etJabatan.text.toString().trim()
            val alamat = etAlamat.text.toString().trim()
            val usernameRaw = etUsername.text.toString().trim()
            val username = normalizeUsername(usernameRaw)
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nama.isEmpty() || noHp.isEmpty() || username.isEmpty() ||
                email.isEmpty() || password.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Nama, No HP, Username, Email, Password wajib diisi",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase Authentication mewajibkan password minimal 6 karakter.
            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false

            // Langkah 1: buat akun login di Firebase Authentication
            // (pakai EMAIL ASLI sekarang, bukan email palsu lagi, supaya nanti
            // fitur Lupa Password bisa mengirim link ke inbox pegawai sungguhan)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid == null) {
                        btnRegister.isEnabled = true
                        Toast.makeText(this, "Registrasi gagal: UID kosong", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val profile = UserProfile(
                        uid = uid,
                        nama = nama,
                        noHp = noHp,
                        jenisKelamin = jk,
                        jabatan = jabatan,
                        alamat = alamat,
                        username = username,
                        email = email
                    )
                    val mapping = UsernameMapping(uid = uid, email = email)

                    // Langkah 2: simpan profil LENGKAP + mapping username->email
                    // sekaligus dalam 1 batch (supaya kalau salah satu gagal,
                    // dua-duanya dibatalkan -- tidak ada data setengah jadi).
                    val batch = firestore.batch()
                    batch.set(firestore.collection("users").document(uid), profile)
                    batch.set(firestore.collection("usernames").document(username), mapping)

                    batch.commit()
                        .addOnSuccessListener {
                            btnRegister.isEnabled = true
                            Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnRegister.isEnabled = true
                            val pesan = if (e.message?.contains("PERMISSION_DENIED", true) == true) {
                                "Username sudah dipakai, coba username lain"
                            } else {
                                "Akun dibuat, tapi gagal simpan profil: ${e.localizedMessage}"
                            }
                            Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnRegister.isEnabled = true
                    val pesan = when {
                        e.message?.contains("already in use", ignoreCase = true) == true ->
                            "Email sudah terdaftar, coba email lain atau login"
                        e.message?.contains("badly formatted", ignoreCase = true) == true ->
                            "Format email tidak valid"
                        else -> "Registrasi gagal: ${e.localizedMessage ?: "Terjadi kesalahan"}"
                    }
                    Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
                }
        }
    }
}