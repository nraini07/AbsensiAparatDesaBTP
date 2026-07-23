package com.example.absensiaparatbtp

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.absensiaparatbtp.database.AbsensiEntity
import com.example.absensiaparatbtp.database.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AbsensiHariIniActivity : AppCompatActivity() {

    private var userId: Int = -1
    private var userNama: String = "Pegawai"
    private var loginLat: Double? = null
    private var loginLng: Double? = null
    private var loginLokasiText: String = ""

    // RadioGroup & RadioButton
    private lateinit var rgJenisAbsensi: RadioGroup
    private lateinit var rbMasuk: RadioButton
    private lateinit var rbSakit: RadioButton
    private lateinit var rbIzin: RadioButton
    private lateinit var rbKeluar: RadioButton

    // TextView status area tugas
    private lateinit var tvStatusAreaTugas: TextView

    // Koordinat kantor desa Batu Pannu
    private val OFFICE_LAT = -2.6887806
    private val OFFICE_LNG = 118.97223

    // Batas radius area tugas (23 meter)
    private val AREA_RADIUS_METER = 23f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absensi_hari_ini)

        val tvBack = findViewById<TextView>(R.id.tvBack)
        val tvTanggal = findViewById<TextView>(R.id.tvTanggal)
        val tvWaktu = findViewById<TextView>(R.id.tvWaktu)
        val etLokasi = findViewById<EditText>(R.id.etLokasi)
        val etKeterangan = findViewById<EditText>(R.id.etKeterangan)
        val btnSimpan = findViewById<Button>(R.id.btnSimpanAbsensi)

        rgJenisAbsensi = findViewById(R.id.rgJenisAbsensi)
        rbMasuk = findViewById(R.id.rbMasuk)
        rbSakit = findViewById(R.id.rbSakit)
        rbIzin = findViewById(R.id.rbIzin)
        rbKeluar = findViewById(R.id.rbKeluar)

        tvStatusAreaTugas = findViewById(R.id.tvStatusAreaTugas)

        rgJenisAbsensi.setOnCheckedChangeListener { _, checkedId ->
            val teks = when (checkedId) {
                R.id.rbMasuk  -> "Masuk"
                R.id.rbSakit  -> "Sakit"
                R.id.rbIzin   -> "Izin"
                R.id.rbKeluar -> "Keluar"
                else          -> "Tidak ada yang dipilih"
            }

            Toast.makeText(
                this,
                "Dipilih: $teks",
                Toast.LENGTH_SHORT
            ).show()
        }
        userId = intent.getIntExtra("USER_ID", -1)
        userNama = intent.getStringExtra("USER_NAMA") ?: "Pegawai"
        loginLat = intent.getDoubleExtra("LOGIN_LAT", Double.NaN)
        loginLng = intent.getDoubleExtra("LOGIN_LNG", Double.NaN)
        loginLokasiText = intent.getStringExtra("LOGIN_LOKASI_TEXT") ?: ""

        if (loginLat?.isNaN() == true) loginLat = null
        if (loginLng?.isNaN() == true) loginLng = null

        // isi tanggal & waktu sekarang
        val now = Date()
        val sdfTanggal = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val sdfWaktu = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvTanggal.text = sdfTanggal.format(now)
        tvWaktu.text = sdfWaktu.format(now)

        // isi lokasi dari login
        if (loginLokasiText.isNotEmpty()) {
            etLokasi.setText(loginLokasiText)
        }

        // ❌ HAPUS ini dari versi lama:
        // rbMasuk.isChecked = true

        // Cek status area tugas saat activity dibuka
        updateStatusAreaTugas()

        tvBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val db = AppDatabase.getInstance(this)
        val absensiDao = db.absensiDao()

        btnSimpan.setOnClickListener {
            // Wajib pilih salah satu radio dulu
            if (rgJenisAbsensi.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Pilih jenis absensi dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val jenisAbsensi = getJenisAbsensiDipilih()

            val lokasi = etLokasi.text.toString().trim()
            val keterangan = etKeterangan.text.toString().trim()
            val tanggal = tvTanggal.text.toString()
            val waktu = tvWaktu.text.toString()

            if (lokasi.isEmpty()) {
                etLokasi.error = "Lokasi tidak boleh kosong"
                etLokasi.requestFocus()
                return@setOnClickListener
            }

            // Hitung lagi status area tugas supaya konsisten
            val keteranganArea = getKeteranganAreaTugas(loginLat, loginLng)

            val ent = AbsensiEntity(
                userId = userId,
                namaPegawai = userNama,
                tanggal = tanggal,
                waktu = waktu,
                jenisAbsensi = jenisAbsensi,
                lokasi = lokasi,
                keterangan = keterangan,
                latitude = loginLat,
                longitude = loginLng,
                keteranganAreaTugas = keteranganArea
            )

            lifecycleScope.launch {
                absensiDao.insertAbsensi(ent)
                runOnUiThread {
                    Toast.makeText(
                        this@AbsensiHariIniActivity,
                        "Absensi tersimpan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun getJenisAbsensiDipilih(): String {
        return when (rgJenisAbsensi.checkedRadioButtonId) {
            R.id.rbMasuk -> "Masuk"
            R.id.rbSakit -> "Sakit"
            R.id.rbIzin -> "Izin"
            R.id.rbKeluar -> "Keluar"
            else -> "Masuk"
        }
    }

    private fun getKeteranganAreaTugas(lat: Double?, lng: Double?): String {
        if (lat == null || lng == null) {
            return "ANDA TIDAK BERADA DI AREA TUGAS"
        }

        val userLoc = Location("user").apply {
            latitude = lat
            longitude = lng
        }

        val officeLoc = Location("office").apply {
            latitude = OFFICE_LAT
            longitude = OFFICE_LNG
        }

        val distance = userLoc.distanceTo(officeLoc)

        return if (distance <= AREA_RADIUS_METER) {
            "ANDA BERADA DI AREA TUGAS"
        } else {
            "ANDA TIDAK BERADA DI AREA TUGAS"
        }
    }

    private fun updateStatusAreaTugas() {
        val statusText = getKeteranganAreaTugas(loginLat, loginLng)
        tvStatusAreaTugas.text = statusText

        if (statusText.contains("TIDAK BERADA")) {
            tvStatusAreaTugas.setTextColor(Color.RED)
        } else {
            tvStatusAreaTugas.setTextColor(Color.parseColor("#008000"))
        }
    }
}