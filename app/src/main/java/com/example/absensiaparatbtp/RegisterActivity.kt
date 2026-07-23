package com.example.absensiaparatbtp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensiaparatbtp.database.AppDatabase
import com.example.absensiaparatbtp.database.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNama = findViewById<EditText>(R.id.etNama)
        val etnoHp = findViewById<EditText>(R.id.etnoHp)
        val etJenisKelamin = findViewById<EditText>(R.id.etJenisKelamin)
        val etJabatan = findViewById<EditText>(R.id.etJabatan)
        val etAlamat = findViewById<EditText>(R.id.etAlamat)
        val etUsername = findViewById<EditText>(R.id.etUsernameReg)
        val etPassword = findViewById<EditText>(R.id.etPasswordReg)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        val db = AppDatabase.getInstance(this)
        val userDao = db.userDao()

        btnRegister.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val noHp = etnoHp.text.toString().trim()
            val jk = etJenisKelamin.text.toString().trim()
            val jabatan = etJabatan.text.toString().trim()
            val alamat = etAlamat.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nama.isEmpty() || noHp.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Nama, NIK, Username, Password wajib diisi",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // di onClick btnRegister
            val user = UserEntity(
                nama = nama,
                noHp = noHp,
                jenisKelamin = jk,
                jabatan = jabatan,
                alamat = alamat,
                username = username,
                password = password
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userDao.insertUser(user)
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registrasi berhasil",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registrasi gagal: ${e.localizedMessage ?: "Database error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}