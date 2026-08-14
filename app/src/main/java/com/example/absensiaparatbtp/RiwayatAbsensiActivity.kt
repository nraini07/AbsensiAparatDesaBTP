package com.example.absensiaparatbtp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensiaparatbtp.firebase.AbsensiRecord
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RiwayatAbsensiActivity : AppCompatActivity() {

    private var userId: String = ""
    private var userNama: String = "Pegawai"

    // Semua data absensi pegawai ini disimpan di memori setelah 1x ambil dari
    // Firestore, supaya filter tanggal/bulan tidak perlu query ulang ke server
    // tiap kali (lebih hemat kuota baca & lebih responsif).
    private var semuaData: List<AbsensiRecord> = emptyList()

    private lateinit var adapter: RiwayatAbsensiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_absensi)

        val tvBack = findViewById<TextView>(R.id.tvBackRiwayat)
        val spinnerBulan = findViewById<Spinner>(R.id.spinnerBulan)
        val tvTanggalFilter = findViewById<TextView>(R.id.tvTanggalFilter)
        val rvRiwayat = findViewById<RecyclerView>(R.id.rvRiwayatAbsensi)

        userId = intent.getStringExtra("USER_ID") ?: ""
        userNama = intent.getStringExtra("USER_NAMA") ?: "Pegawai"

        rvRiwayat.layoutManager = LinearLayoutManager(this)
        adapter = RiwayatAbsensiAdapter(emptyList())
        rvRiwayat.adapter = adapter

        val firestore = FirebaseFirestore.getInstance()

        tvBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvTanggalFilter.text = "Semua tanggal"

        if (userId.isEmpty()) {
            Toast.makeText(this, "Sesi tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil SEMUA riwayat absensi pegawai ini sekali saja dari Firestore
        firestore.collection("absensi")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                semuaData = snapshot.documents
                    .mapNotNull { it.toObject(AbsensiRecord::class.java) }
                    // Terbaru dulu. createdAt bisa null kalau baru saja disimpan &
                    // server timestamp-nya belum ke-sync ke cache lokal, jadi taruh
                    // yang null di depan supaya tetap kelihatan.
                    .sortedByDescending { it.createdAt }

                adapter.setData(semuaData)
                setupSpinnerBulan(spinnerBulan)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal ambil riwayat: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                    Toast.LENGTH_LONG
                ).show()
            }

        // POPUP KALENDER untuk filter per tanggal spesifik
        val calendar = Calendar.getInstance()
        tvTanggalFilter.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val dialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val cal = Calendar.getInstance()
                    cal.set(selectedYear, selectedMonth, selectedDay)
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val tanggalStr = sdf.format(cal.time)

                    tvTanggalFilter.text = tanggalStr

                    val hasil = semuaData.filter { it.tanggal == tanggalStr }
                    adapter.setData(hasil)

                    // Reset spinner bulan ke "Semua" supaya tidak membingungkan
                    // (dua filter aktif sekaligus)
                    spinnerBulan.setSelection(0)
                },
                year, month, day
            )
            dialog.show()
        }
    }

    /**
     * Bangun daftar bulan (untuk spinner) dari data yang sudah ada di memori,
     * dengan cara ekstrak "MM-yyyy" dari field tanggal (format dd-MM-yyyy).
     */
    private fun setupSpinnerBulan(spinnerBulan: Spinner) {
        val namaBulan = arrayOf(
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        // Ambil semua "MM-yyyy" unik dari data, urutkan terbaru dulu
        val listBulan = semuaData
            .mapNotNull { record ->
                if (record.tanggal.length == 10) record.tanggal.substring(3, 10) else null
            }
            .distinct()
            .sortedDescending()

        val listLabel = mutableListOf("Semua")
        for (b in listBulan) {
            val mm = b.substring(0, 2)
            val yy = b.substring(3, 7)
            val bulanIndex = mm.toIntOrNull() ?: 0
            val label = if (bulanIndex in 1..12) {
                "${namaBulan[bulanIndex]} $yy"
            } else {
                b
            }
            listLabel.add(label)
        }

        val bulanAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listLabel
        )
        bulanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBulan.adapter = bulanAdapter

        spinnerBulan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val hasil = if (position == 0) {
                    semuaData
                } else {
                    val bulanDipilih = listBulan[position - 1]
                    semuaData.filter { record ->
                        record.tanggal.length == 10 && record.tanggal.substring(3, 10) == bulanDipilih
                    }
                }
                adapter.setData(hasil)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
    }
}