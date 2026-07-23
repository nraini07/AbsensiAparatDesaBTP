package com.example.absensiaparatbtp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensiaparatbtp.database.AppDatabase
import kotlinx.coroutines.launch

class LaporanDetailPegawaiActivity : AppCompatActivity() {

    private lateinit var adapter: RiwayatAbsensiAdapter
    private var listRiwayatAbsensi = emptyList<com.example.absensiaparatbtp.database.AbsensiEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_detail_pegawai)

        try {
            val tvBack = findViewById<TextView>(R.id.tvBackDetailPegawai)
            val tvNamaPegawai = findViewById<TextView>(R.id.tvNamaPegawaiLaporan)
            val tvJabatan = findViewById<TextView>(R.id.tvJabatanLaporan)
            val spinnerBulanDetail = findViewById<Spinner>(R.id.spinnerBulanDetail)
            val rvRiwayatDetail = findViewById<RecyclerView>(R.id.rvRiwayatDetail)

            val namaPegawai = intent.getStringExtra("NAMA_PEGAWAI") ?: "Pegawai"
            val jabatanPegawai = intent.getStringExtra("JABATAN_PEGAWAI") ?: "-"

            tvNamaPegawai.text = namaPegawai
            tvJabatan.text = "Jabatan: $jabatanPegawai"

            rvRiwayatDetail.layoutManager = LinearLayoutManager(this)
            adapter = RiwayatAbsensiAdapter(emptyList())
            rvRiwayatDetail.adapter = adapter

            val db = AppDatabase.getInstance(this)
            val absensiDao = db.absensiDao()

            lifecycleScope.launch {
                try {
                    val riwayatAbsensi = absensiDao.getAbsensiByNamaPegawai(namaPegawai)
                    listRiwayatAbsensi = riwayatAbsensi

                    val bulanSet = mutableSetOf<String>()
                    for (absensi in riwayatAbsensi) {
                        val parts = absensi.tanggal.split("-")
                        if (parts.size == 3) {
                            val bulan = String.format("%02d-%s", parts[1].toIntOrNull() ?: 0, parts[2])
                            bulanSet.add(bulan)
                        }
                    }

                    val bulanList = bulanSet.sorted().toList()

                    val namaBulan = arrayOf(
                        "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                    )

                    val bulanLabelList = mutableListOf<String>()
                    bulanLabelList.add("Semua Bulan")

                    for (bulan in bulanList) {
                        val parts = bulan.split("-")
                        if (parts.size == 2) {
                            val mm = parts[0].toIntOrNull() ?: 0
                            val yy = parts[1]
                            val bulanIndex = if (mm in 1..12) mm else 0
                            val label = if (bulanIndex > 0) {
                                "${namaBulan[bulanIndex]} $yy"
                            } else {
                                bulan
                            }
                            bulanLabelList.add(label)
                        }
                    }

                    runOnUiThread {
                        tvNamaPegawai.text = namaPegawai
                        tvJabatan.text = "Jabatan: $jabatanPegawai"

                        val adapterSpinner = ArrayAdapter(
                            this@LaporanDetailPegawaiActivity,
                            android.R.layout.simple_spinner_item,
                            bulanLabelList
                        )
                        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerBulanDetail.adapter = adapterSpinner

                        adapter.setData(riwayatAbsensi.sortedByDescending { it.tanggal })

                        spinnerBulanDetail.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: android.view.View?,
                                position: Int,
                                id: Long
                            ) {
                                if (position == 0) {
                                    adapter.setData(riwayatAbsensi.sortedByDescending { it.tanggal })
                                } else if (position > 0 && position <= bulanList.size) {
                                    val bulanSelected = bulanList[position - 1]
                                    val filtered = riwayatAbsensi.filter { absensi ->
                                        val parts = absensi.tanggal.split("-")
                                        if (parts.size == 3) {
                                            val bulan = String.format("%02d-%s", parts[1].toIntOrNull() ?: 0, parts[2])
                                            bulan == bulanSelected
                                        } else {
                                            false
                                        }
                                    }
                                    adapter.setData(filtered.sortedByDescending { it.tanggal })
                                }
                            }

                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                        })
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@LaporanDetailPegawaiActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            tvBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Fatal Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}