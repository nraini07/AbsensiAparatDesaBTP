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
import com.example.absensiaparatbtp.database.AppDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity(){

    private lateinit var etUsernameLogin: EditText
    private lateinit var etPasswordLogin: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnKeRegister: Button
    private lateinit var btnAmbilLokasi: Button
    private lateinit var tvLokasiLogin: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentLocationText: String = ""

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

        val db = AppDatabase.getInstance(this)
        val userDao = db.userDao()

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

            CoroutineScope(Dispatchers.IO).launch {

                // ADMIN default
                if (username == "admin" && password == "admin123") {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Login sebagai ADMIN", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    return@launch
                }

                // PEGAWAI dari Room
                val user = userDao.login(username, password)

                if (user == null) {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Username atau password salah",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Selamat datang, ${user.nama}",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@LoginActivity, PegawaiDashboardActivity::class.java)
                        intent.putExtra("USER_ID", user.id)
                        intent.putExtra("USER_NAMA", user.nama)
                        intent.putExtra("LOGIN_LAT", currentLatitude)
                        intent.putExtra("LOGIN_LNG", currentLongitude)
                        intent.putExtra("LOGIN_LOKASI_TEXT", currentLocationText)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
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

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    currentLocationText = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                    tvLokasiLogin.text = "Lokasi terdeteksi: $currentLocationText"
                } else {
                    Toast.makeText(this, "Lokasi belum tersedia, coba lagi", Toast.LENGTH_SHORT).show()
                }
            }
    }
}