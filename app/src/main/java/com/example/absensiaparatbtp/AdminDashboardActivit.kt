package com.example.absensiaparatbtp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val tvHaloAdmin = findViewById<TextView>(R.id.tvHaloAdmin)
        val btnLaporanAbsensi = findViewById<LinearLayout>(R.id.btnLaporanAbsensi)
        val btnProfilPengguna = findViewById<LinearLayout>(R.id.btnProfilPengguna)
        val btnLogoutAdmin = findViewById<ImageButton>(R.id.btnLogoutAdmin)

        // Teks sapaan admin
        tvHaloAdmin.text = "SELAMAT DATANG, ADMIN"

        // Menu: Laporan Absensi
        btnLaporanAbsensi.setOnClickListener {
            startActivity(Intent(this, LaporanAbsensiActivity::class.java))
        }

        // Menu: Profil Pengguna
        btnProfilPengguna.setOnClickListener {
            startActivity(Intent(this, ProfilPenggunaActivity::class.java))
        }

        // Tombol LOGOUT
        btnLogoutAdmin.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Yakin ingin logout?")
            .setPositiveButton("Ya") { _, _ ->
                // Jika ada session di SharedPreferences, bersihkan di sini (opsional)
                val prefs = getSharedPreferences("SESSION", MODE_PRIVATE)
                prefs.edit().clear().apply()

                // Kembali ke MainActivity (halaman login) dan bersihkan backstack
                val intent = Intent(this, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}