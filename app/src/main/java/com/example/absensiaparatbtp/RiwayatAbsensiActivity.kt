package com.example.absensiaparatbtp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensiaparatbtp.database.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RiwayatAbsensiActivity : AppCompatActivity() {

    private var userId: Int = -1
    private var userNama: String = "Pegawai"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_absensi)

        val tvBack = findViewById<TextView>(R.id.tvBackRiwayat)
        val spinnerBulan = findViewById<Spinner>(R.id.spinnerBulan)
        val tvTanggalFilter = findViewById<TextView>(R.id.tvTanggalFilter)
        val rvRiwayat = findViewById<RecyclerView>(R.id.rvRiwayatAbsensi)

        userId = intent.getIntExtra("USER_ID", -1)
        userNama = intent.getStringExtra("USER_NAMA") ?: "Pegawai"

        rvRiwayat.layoutManager = LinearLayoutManager(this)
        val adapter = RiwayatAbsensiAdapter(emptyList())
        rvRiwayat.adapter = adapter

        val db = AppDatabase.getInstance(this)
        val dao = db.absensiDao()

        // POPUP KALENDER
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

                    lifecycleScope.launch {
                        val dataTanggal = dao.getRiwayatByUserAndTanggal(userId, tanggalStr)
                        runOnUiThread {
                            adapter.setData(dataTanggal)
                        }
                    }
                },
                year, month, day
            )

            dialog.show()
        }

        // SPINNER BULAN
        lifecycleScope.launch {
            val listBulan = dao.getBulanDenganData(userId)  // contoh: ["04-2026", "05-2026"]

            val listLabel = mutableListOf<String>()
            listLabel.add("Semua")

            val namaBulan = arrayOf(
                "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
            )

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

            runOnUiThread {
                val bulanAdapter = ArrayAdapter(
                    this@RiwayatAbsensiActivity,
                    android.R.layout.simple_spinner_item,
                    listLabel
                )
                bulanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerBulan.adapter = bulanAdapter
            }

            val dataAwal = dao.getRiwayatByUser(userId)
            runOnUiThread {
                adapter.setData(dataAwal)
            }

            runOnUiThread {
                spinnerBulan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        lifecycleScope.launch {
                            val data = if (position == 0) {
                                dao.getRiwayatByUser(userId)
                            } else {
                                val bulanDipilih = listBulan[position - 1]
                                dao.getRiwayatByUserAndBulan(userId, bulanDipilih)
                            }
                            runOnUiThread {
                                adapter.setData(data)
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) { }
                }
            }
        }

        tvBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvTanggalFilter.text = "00-00-0000"
    }
}