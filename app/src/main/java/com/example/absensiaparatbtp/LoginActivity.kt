package com.example.absensiaparatbtp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.absensiaparatbtp.firebase.usernameToPseudoEmail
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity(){

    private lateinit var etUsernameLogin: EditText
    private lateinit var etPasswordLogin: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnKeRegister: Button
    private lateinit var btnAmbilLokasi: Button
    private lateinit var tvLokasiLogin: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentAkurasi: Float? = null
    private var currentLocationText: String = ""

    // Batas akurasi GPS yang masih dianggap layak dipakai (meter).
    // Di dalam ruangan/gedung akurasi bisa turun; nilai di atas ini dianggap kurang bisa dipercaya.
    private val AKURASI_MAKSIMAL_METER = 50f

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsernameLogin = findViewById(R.id.etUsernameLogin)
        etPasswordLogin = findViewById(R.id.etPasswordLogin)
        btnLogin = findViewById(R.id.btnLogin)
        btnKeRegister = findViewById(R.id.btnKeRegister)
        btnAmbilLokasi = findViewById(R.id.btnAmbilLokasi)
        tvLokasiLogin = findViewById(R.id.tvLokasiLogin)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        btnAmbilLokasi.setOnClickListener {
            requestLocationPermission()
        }

        btnKeRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val username = etUsernameLogin.text.toString().trim()
            val password = etPasswordLogin.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentLatitude == null || currentLongitude == null) {
                Toast.makeText(this, "Aktifkan / ambil lokasi dulu sebelum login", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ADMIN: username/password tetap hardcode (dicek manual di sini),
            // TAPI di baliknya kita tetap masuk ke Firebase Auth pakai akun
            // khusus admin, supaya Firestore rules (yang mewajibkan
            // request.auth != null) tetap mengizinkan admin baca/tulis data.
            if (username == "admin" && password == "adminbtp") {
                btnLogin.isEnabled = false
                val adminEmail = usernameToPseudoEmail("admin")

                auth.signInWithEmailAndPassword(adminEmail, password)
                    .addOnSuccessListener {
                        btnLogin.isEnabled = true
                        lanjutKeAdminDashboard()
                    }
                    .addOnFailureListener {
                        // Kemungkinan besar akun admin di Firebase Auth belum pernah
                        // dibuat -> buat otomatis sekali ini saja (auto-provisioning).
                        auth.createUserWithEmailAndPassword(adminEmail, password)
                            .addOnSuccessListener {
                                btnLogin.isEnabled = true
                                lanjutKeAdminDashboard()
                            }
                            .addOnFailureListener { e ->
                                btnLogin.isEnabled = true
                                Toast.makeText(
                                    this,
                                    "Login admin gagal: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                return@setOnClickListener
            }

            // PEGAWAI: login via Firebase Authentication.
            // Karena akun Firebase pegawai sekarang pakai EMAIL ASLI (bukan
            // email palsu lagi), kita perlu cari dulu email itu dari koleksi
            // "usernames" berdasarkan username yang diketik.
            btnLogin.isEnabled = false
            val usernameKey = com.example.absensiaparatbtp.firebase.normalizeUsername(username)

            firestore.collection("usernames").document(usernameKey).get()
                .addOnSuccessListener { mappingDoc ->
                    val emailAsli = mappingDoc.getString("email")
                    if (emailAsli == null) {
                        btnLogin.isEnabled = true
                        Toast.makeText(this, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    auth.signInWithEmailAndPassword(emailAsli, password)
                        .addOnSuccessListener { authResult ->
                            val uid = authResult.user?.uid
                            if (uid == null) {
                                btnLogin.isEnabled = true
                                Toast.makeText(this, "Login gagal: UID kosong", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // Ambil profil lengkap (nama, dll) dari Firestore pakai uid tadi
                            firestore.collection("users").document(uid).get()
                                .addOnSuccessListener { doc ->
                                    btnLogin.isEnabled = true
                                    val nama = doc.getString("nama") ?: username

                                    Toast.makeText(this, "Selamat datang, $nama", Toast.LENGTH_SHORT).show()

                                    val intent = Intent(this, PegawaiDashboardActivity::class.java)
                                    // USER_ID sekarang berupa UID Firebase (teks), bukan lagi angka Room
                                    intent.putExtra("USER_ID", uid)
                                    intent.putExtra("USER_NAMA", nama)
                                    intent.putExtra("LOGIN_LAT", currentLatitude)
                                    intent.putExtra("LOGIN_LNG", currentLongitude)
                                    intent.putExtra("LOGIN_AKURASI", currentAkurasi ?: -1f)
                                    intent.putExtra("LOGIN_LOKASI_TEXT", currentLocationText)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    btnLogin.isEnabled = true
                                    Toast.makeText(
                                        this,
                                        "Login berhasil tapi gagal ambil profil: ${e.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        .addOnFailureListener {
                            btnLogin.isEnabled = true
                            Toast.makeText(this, "Username atau password salah", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    btnLogin.isEnabled = true
                    Toast.makeText(this, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
        }

        // LUPA PASSWORD: minta username, cari email aslinya, kirim link reset
        val tvLupaPassword = findViewById<TextView>(R.id.tvLupaPassword)
        tvLupaPassword.setOnClickListener {
            tampilkanDialogLupaPassword()
        }
    }

    private fun tampilkanDialogLupaPassword() {
        val input = EditText(this)
        input.hint = "Masukkan username Anda"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Lupa Password")
            .setMessage("Masukkan username Anda, link reset password akan dikirim ke email yang terdaftar saat registrasi.")
            .setView(input)
            .setPositiveButton("Kirim") { _, _ ->
                val usernameInput = input.text.toString().trim()
                if (usernameInput.isEmpty()) {
                    Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val usernameKey = com.example.absensiaparatbtp.firebase.normalizeUsername(usernameInput)

                firestore.collection("usernames").document(usernameKey).get()
                    .addOnSuccessListener { doc ->
                        val emailAsli = doc.getString("email")
                        if (emailAsli == null) {
                            Toast.makeText(this, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        auth.sendPasswordResetEmail(emailAsli)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Link reset password sudah dikirim ke email terdaftar",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Gagal kirim email reset: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun lanjutKeAdminDashboard() {
        Toast.makeText(this, "Login sebagai ADMIN", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, AdminDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun requestLocationPermission() {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Tampilkan status sementara sambil menunggu, karena mengambil lokasi baru
        // (bukan lokasi cache lama) butuh beberapa detik.
        tvLokasiLogin.text = "Mengambil lokasi terkini..."

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    currentAkurasi = location.accuracy
                    currentLocationText = "Lat: ${location.latitude}, Lng: ${location.longitude}"

                    val akurasi = location.accuracy
                    // provider: "gps" = GPS satelit asli (akurat),
                    // "network" = perkiraan dari WiFi/menara seluler (bisa meleset jauh),
                    // "fused" = gabungan otomatis oleh Google Play Services.
                    val provider = location.provider ?: "tidak diketahui"

                    if (akurasi > AKURASI_MAKSIMAL_METER) {
                        tvLokasiLogin.text =
                            "Lokasi terdeteksi (provider: $provider, akurasi lemah: ±${akurasi.toInt()}m). " +
                                    "Coba dekat jendela/luar ruangan lalu ambil ulang."
                    } else {
                        tvLokasiLogin.text =
                            "Lokasi terdeteksi: $currentLocationText\n" +
                                    "Provider: $provider, akurasi ±${akurasi.toInt()}m"
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Lokasi belum tersedia, pastikan GPS aktif lalu coba lagi",
                        Toast.LENGTH_SHORT
                    ).show()
                    tvLokasiLogin.text = "Lokasi belum terdeteksi"
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Gagal mengambil lokasi, pastikan GPS aktif lalu coba lagi",
                    Toast.LENGTH_SHORT
                ).show()
                tvLokasiLogin.text = "Lokasi belum terdeteksi"
            }
    }
}