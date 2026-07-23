package com.example.absensiaparatbtp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.absensiaparatbtp.database.AppDatabase
import com.example.absensiaparatbtp.database.UserEntity
import kotlinx.coroutines.launch

class EditProfilPegawaiActivity : AppCompatActivity() {

    private var userId: Int = 0
    private lateinit var etNama: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etNoTelp: EditText
    private lateinit var etJabatan: EditText
    private lateinit var etJK: EditText
    private lateinit var etAlamat: EditText
    private lateinit var btnSimpan: Button
    private lateinit var btnHapus: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profil_pegawai)

        userId = intent.getIntExtra("USER_ID", 0)

        etNama = findViewById(R.id.etNama)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etNoTelp = findViewById(R.id.etNoTelp)
        etJabatan = findViewById(R.id.etJabatan)
        etJK = findViewById(R.id.etJK)
        etAlamat = findViewById(R.id.etAlamat)
        btnSimpan = findViewById(R.id.btnSimpanProfil)
        btnHapus = findViewById(R.id.btnHapusAkun)

        val db = AppDatabase.getInstance(this)
        val userDao = db.userDao()

        // Isi awal
        lifecycleScope.launch {
            val user = userDao.getUserById(userId)
            if (user != null) {
                etNama.setText(user.nama)
                etUsername.setText(user.username)
                etPassword.setText(user.password)
                etNoTelp.setText(user.noHp)
                etJabatan.setText(user.jabatan)
                etJK.setText(user.jenisKelamin)
                etAlamat.setText(user.alamat)
            } else {
                Toast.makeText(this@EditProfilPegawaiActivity,
                    "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Simpan perubahan
        btnSimpan.setOnClickListener {
            lifecycleScope.launch {
                val user = userDao.getUserById(userId)
                if (user != null) {
                    val updated = UserEntity(
                        id = user.id,  // pastikan field id ada di UserEntity
                        nama = etNama.text.toString(),
                        username = etUsername.text.toString(),
                        password = etPassword.text.toString(),
                        noHp = etNoTelp.text.toString(),
                        jabatan = etJabatan.text.toString(),
                        jenisKelamin = etJK.text.toString(),
                        alamat = etAlamat.text.toString()
                    )
                    userDao.updateUser(updated)
                    Toast.makeText(
                        this@EditProfilPegawaiActivity,
                        "Profil diperbarui",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        // Hapus akun
        btnHapus.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Akun")
                .setMessage("Yakin ingin menghapus akun ini? Data tidak bisa dikembalikan.")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        val user = userDao.getUserById(userId)
                        if (user != null) {
                            userDao.deleteUser(user)
                            Toast.makeText(
                                this@EditProfilPegawaiActivity,
                                "Akun dihapus",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}