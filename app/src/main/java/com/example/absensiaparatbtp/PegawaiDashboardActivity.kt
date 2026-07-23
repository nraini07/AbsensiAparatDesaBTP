package com.example.absensiaparatbtp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PegawaiDashboardActivity : AppCompatActivity() {

    private var userId: Int = -1
    private var userNama: String = "Pegawai"
    private var loginLat: Double? = null
    private var loginLng: Double? = null
    private var loginLokasiText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pegawai_dashboard)

        val tvHalo = findViewById<TextView>(R.id.tvHaloPegawai)
        val btnAbsensiHariIni = findViewById<LinearLayout>(R.id.btnAbsensiHariIni)
        val btnRiwayatAbsensi = findViewById<LinearLayout>(R.id.btnRiwayatAbsensi)
        val btnLogoutPegawai = findViewById<ImageButton>(R.id.btnLogoutPegawai)

        userId = intent.getIntExtra("USER_ID", -1)
        userNama = intent.getStringExtra("USER_NAMA") ?: "Pegawai"
        loginLat = intent.getDoubleExtra("LOGIN_LAT", Double.NaN)
        loginLng = intent.getDoubleExtra("LOGIN_LNG", Double.NaN)
        loginLokasiText = intent.getStringExtra("LOGIN_LOKASI_TEXT") ?: ""

        if (loginLat?.isNaN() == true) loginLat = null
        if (loginLng?.isNaN() == true) loginLng = null

        tvHalo.text = "Halo, $userNama"

        btnAbsensiHariIni.setOnClickListener {
            val i = Intent(this, AbsensiHariIniActivity::class.java)
            i.putExtra("USER_ID", userId)
            i.putExtra("USER_NAMA", userNama)
            i.putExtra("LOGIN_LAT", loginLat)
            i.putExtra("LOGIN_LNG", loginLng)
            i.putExtra("LOGIN_LOKASI_TEXT", loginLokasiText)
            startActivity(i)
        }

        btnRiwayatAbsensi.setOnClickListener {
            val i = Intent(this, RiwayatAbsensiActivity::class.java)
            i.putExtra("USER_ID", userId)
            i.putExtra("USER_NAMA", userNama)
            startActivity(i)
        }

        // Tombol logout
        btnLogoutPegawai.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Yakin ingin logout?")
            .setPositiveButton("Ya") { _, _ ->
                val prefs = getSharedPreferences("SESSION", MODE_PRIVATE)
                prefs.edit().clear().apply()

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