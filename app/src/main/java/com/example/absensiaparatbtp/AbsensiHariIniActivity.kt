package com.example.absensiaparatbtp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.absensiaparatbtp.firebase.AbsensiRecord
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AbsensiHariIniActivity : AppCompatActivity() {

    private var userId: String = ""
    private var userNama: String = "Pegawai"
    private var loginLat: Double? = null
    private var loginLng: Double? = null
    private var loginAkurasi: Float? = null
    private var loginLokasiText: String = ""

    // RadioGroup & RadioButton
    private lateinit var rgJenisAbsensi: RadioGroup
    private lateinit var rbMasuk: RadioButton
    private lateinit var rbSakit: RadioButton
    private lateinit var rbIzin: RadioButton
    private lateinit var rbKeluar: RadioButton

    // TextView status area tugas
    private lateinit var tvStatusAreaTugas: TextView

    // ======================================================================
    // TITIK-TITIK LOKASI TUGAS YANG SAH UNTUK ABSENSI
    // ======================================================================
    // Kalau aparat desa juga perlu absen di lokasi lain (Balai Desa, Posyandu,
    // Kantor Dusun, dll), cukup tambahkan entri baru di list ini. Sistem akan
    // menganggap SAH kalau posisi pegawai berada dalam radius SALAH SATU titik.
    //
    // radiusMeter disesuaikan per lokasi:
    // - Kantor dengan halaman/pekarangan luas -> radius bisa lebih besar (100-150m)
    // - Lokasi sempit (mis. posyandu di rumah warga) -> radius lebih kecil (30-50m)
    //
    // PENTING: radius ini hanya untuk mentolerir ketidakakuratan GPS yang wajar
    // (puluhan-ratusan meter). Jangan diisi angka besar (ratusan meter s.d. km)
    // hanya untuk "memaksa" absensi dari luar area terdeteksi sah — itu akan
    // menghilangkan fungsi validasi lokasi itu sendiri. Kalau selisih jarak yang
    // terbaca sampai hitungan kilometer, itu tanda GPS memakai provider "network"
    // (perkiraan sinyal, bukan GPS satelit asli) — solusinya di pengambilan
    // lokasi (lihat refreshLokasiTerkini & provider yang ditampilkan), bukan
    // dengan memperbesar radius ini.
    private data class TitikTugas(
        val nama: String,
        val lat: Double,
        val lng: Double,
        val radiusMeter: Float
    )

    private val daftarTitikTugas = listOf(
        TitikTugas(
            nama = "Kantor Desa Batu Pannu",
            // Koordinat resmi dari Google Maps (Plus Code 8WJP+8P6),
            // Batu Pannu, Kec. Mamuju, Kabupaten Mamuju, Sulawesi Barat 91511.
            // Catatan: koordinat sebelumnya (-2.6887806, 118.9722306) ternyata
            // meleset ±4.4 km dari lokasi asli — itu penyebab utama absensi
            // selalu terbaca "tidak berada di area tugas" walau sudah di kantor.
            lat = -2.669544,
            lng = 118.937427,
            // Kantor kecil: ruangan ±30m + lapangan depan ±5m.
            // 50m sudah termasuk toleransi ketidakakuratan GPS indoor,
            // tapi tetap cukup ketat agar area luar kompleks kantor tidak ikut terhitung sah.
            radiusMeter = 50f
        )
        // Contoh kalau mau tambah titik lain:
        // TitikTugas(
        //     nama = "Balai Desa Batu Pannu",
        //     lat = -2.xxxxxx,
        //     lng = 118.xxxxxx,
        //     radiusMeter = 50f
        // ),
    )

    // Batas akurasi GPS yang masih dianggap layak dipakai (meter).
    private val AKURASI_MAKSIMAL_METER = 50f

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var sedangAmbilLokasi = false

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        userId = intent.getStringExtra("USER_ID") ?: ""
        userNama = intent.getStringExtra("USER_NAMA") ?: "Pegawai"
        loginLat = intent.getDoubleExtra("LOGIN_LAT", Double.NaN)
        loginLng = intent.getDoubleExtra("LOGIN_LNG", Double.NaN)
        loginAkurasi = intent.getFloatExtra("LOGIN_AKURASI", -1f).let { if (it < 0) null else it }
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

        // Cek status area tugas saat activity dibuka, pakai lokasi lama dulu (kalau ada)
        // sambil menunggu lokasi terbaru selesai diambil.
        updateStatusAreaTugas()
        refreshLokasiTerkini {}

        tvBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val firestore = FirebaseFirestore.getInstance()

        btnSimpan.setOnClickListener {
            // Wajib pilih salah satu radio dulu
            if (rgJenisAbsensi.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Pilih jenis absensi dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sedangAmbilLokasi) {
                Toast.makeText(this, "Masih mengambil lokasi, tunggu sebentar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId.isEmpty()) {
                Toast.makeText(this, "Sesi login tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lokasi = etLokasi.text.toString().trim()
            if (lokasi.isEmpty()) {
                etLokasi.error = "Lokasi tidak boleh kosong"
                etLokasi.requestFocus()
                return@setOnClickListener
            }

            // Ambil lokasi TERBARU dulu (bukan lokasi lama saat login) supaya status
            // area tugas mencerminkan posisi pegawai saat ini, baru simpan absensinya.
            btnSimpan.isEnabled = false
            refreshLokasiTerkini {
                val jenisAbsensi = getJenisAbsensiDipilih()
                val keterangan = etKeterangan.text.toString().trim()
                val tanggal = tvTanggal.text.toString()
                val waktu = tvWaktu.text.toString()

                val keteranganArea = getKeteranganAreaTugas(loginLat, loginLng)

                val record = AbsensiRecord(
                    userId = userId,
                    namaPegawai = userNama,
                    tanggal = tanggal,
                    waktu = waktu,
                    jenisAbsensi = jenisAbsensi,
                    lokasi = lokasi,
                    keterangan = keterangan,
                    latitude = loginLat,
                    longitude = loginLng,
                    akurasiMeter = loginAkurasi,
                    keteranganAreaTugas = keteranganArea
                    // createdAt sengaja tidak diisi -> otomatis diisi Firestore server
                )

                firestore.collection("absensi")
                    .add(record)
                    .addOnSuccessListener {
                        btnSimpan.isEnabled = true
                        Toast.makeText(
                            this@AbsensiHariIniActivity,
                            "Absensi tersimpan",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btnSimpan.isEnabled = true
                        Toast.makeText(
                            this@AbsensiHariIniActivity,
                            "Gagal menyimpan absensi: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
    }

    /**
     * Ambil lokasi GPS terbaru (bukan lokasi cache), lalu update loginLat/loginLng
     * dan tampilan status area tugas. onDone selalu dipanggil di akhir, baik lokasi
     * berhasil didapat maupun gagal (supaya alur simpan absensi tidak macet).
     */
    private fun refreshLokasiTerkini(onDone: () -> Unit) {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            // Tidak ada izin lokasi, pakai apa adanya (lokasi saat login) dan lanjut.
            onDone()
            return
        }

        sedangAmbilLokasi = true
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location: Location? ->
                sedangAmbilLokasi = false
                if (location != null) {
                    loginLat = location.latitude
                    loginLng = location.longitude
                    loginAkurasi = location.accuracy

                    val provider = location.provider ?: "tidak diketahui"
                    if (location.accuracy > AKURASI_MAKSIMAL_METER) {
                        Toast.makeText(
                            this,
                            "Provider: $provider, akurasi GPS lemah (±${location.accuracy.toInt()}m), " +
                                    "coba dekat jendela/luar ruangan untuk hasil lebih akurat",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                updateStatusAreaTugas()
                onDone()
            }
            .addOnFailureListener {
                sedangAmbilLokasi = false
                // Gagal ambil lokasi baru, tetap lanjut pakai lokasi yang ada sebelumnya.
                updateStatusAreaTugas()
                onDone()
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

        // Cari titik tugas TERDEKAT dari semua yang terdaftar
        var titikTerdekat: TitikTugas? = null
        var jarakTerdekat = Float.MAX_VALUE

        for (titik in daftarTitikTugas) {
            val titikLoc = Location("titik").apply {
                latitude = titik.lat
                longitude = titik.lng
            }
            val jarak = userLoc.distanceTo(titikLoc)
            if (jarak < jarakTerdekat) {
                jarakTerdekat = jarak
                titikTerdekat = titik
            }
        }

        if (titikTerdekat == null) {
            return "ANDA TIDAK BERADA DI AREA TUGAS"
        }

        val jarakText = if (jarakTerdekat >= 1000) {
            "%.2f km".format(jarakTerdekat / 1000)
        } else {
            "${jarakTerdekat.toInt()} m"
        }

        return if (jarakTerdekat <= titikTerdekat.radiusMeter) {
            "ANDA BERADA DI AREA TUGAS (${titikTerdekat.nama}, jarak: $jarakText)"
        } else {
            "ANDA TIDAK BERADA DI AREA TUGAS " +
                    "(terdekat: ${titikTerdekat.nama}, jarak: $jarakText)"
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