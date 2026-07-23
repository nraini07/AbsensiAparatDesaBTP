package com.example.absensiaparatbtp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensiaparatbtp.database.AppDatabase
import kotlinx.coroutines.launch
import java.util.Calendar

class LaporanAbsensiActivity : AppCompatActivity() {

    private var listRingkasan = emptyList<LaporanRingkasanAbsensi>()
    private lateinit var adapter: LaporanRingkasanAdapter
    private var bulanDipilih = 0
    private var tahunDipilih = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_absensi)

        try {
            val tvBack = findViewById<TextView>(R.id.tvBackLaporan)
            val spinnerBulan = findViewById<Spinner>(R.id.spinnerBulanLaporan)
            val spinnerTahun = findViewById<Spinner>(R.id.spinnerTahunLaporan)
            val etSearchPegawai = findViewById<EditText>(R.id.etSearchPegawai)
            val btnUnduhPDF = findViewById<Button>(R.id.btnUnduhPDF)
            val rvLaporanAbsensi = findViewById<RecyclerView>(R.id.rvLaporanAbsensi)

            val tvTotalMasuk = findViewById<TextView>(R.id.tvTotalMasuk)
            val tvTotalIzin = findViewById<TextView>(R.id.tvTotalIzin)
            val tvTotalSakit = findViewById<TextView>(R.id.tvTotalSakit)
            val tvTotalKeluar = findViewById<TextView>(R.id.tvTotalKeluar)

            val db = AppDatabase.getInstance(this)
            val dao = db.absensiDao()
            val userDao = db.userDao()

            // LIST UTAMA: tetap bisa diklik
            rvLaporanAbsensi.layoutManager = LinearLayoutManager(this)
            adapter = LaporanRingkasanAdapter(emptyList()) { namaPegawai ->
                lifecycleScope.launch {
                    val user = userDao.getUserByNama(namaPegawai)
                    val jabatanPegawai = user?.jabatan ?: "-"

                    val intent = Intent(
                        this@LaporanAbsensiActivity,
                        LaporanDetailPegawaiActivity::class.java
                    )
                    intent.putExtra("NAMA_PEGAWAI", namaPegawai)
                    intent.putExtra("JABATAN_PEGAWAI", jabatanPegawai)
                    startActivity(intent)
                }
            }
            rvLaporanAbsensi.adapter = adapter

            val bulanList = arrayOf(
                "BULAN", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
            )

            val adapterBulan = ArrayAdapter(
                this,
                R.layout.spinner_item_bulan,
                bulanList
            )
            adapterBulan.setDropDownViewResource(
                R.layout.spinner_dropdown_item_bulan
            )
            spinnerBulan.adapter = adapterBulan

            val tahunList = mutableListOf<String>()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            for (i in currentYear - 5..currentYear + 1) {
                tahunList.add(i.toString())
            }

            val adapterTahun = ArrayAdapter(
                this,
                R.layout.spinner_item_bulan,
                tahunList
            )
            adapterTahun.setDropDownViewResource(R.layout.spinner_dropdown_item_bulan)
            spinnerTahun.adapter = adapterTahun
            spinnerTahun.setSelection(tahunList.indexOf(currentYear.toString()))

            spinnerBulan.setOnItemSelectedListener(object :
                android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (position > 0) {
                        bulanDipilih = position
                        if (tahunDipilih > 0) {
                            loadLaporan(
                                dao,
                                tvTotalMasuk,
                                tvTotalIzin,
                                tvTotalSakit,
                                tvTotalKeluar,
                                bulanList[position]
                            )
                        }
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerTahun.setOnItemSelectedListener(object :
                android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    tahunDipilih = tahunList[position].toIntOrNull() ?: 0
                    if (bulanDipilih > 0 && tahunDipilih > 0) {
                        loadLaporan(
                            dao,
                            tvTotalMasuk,
                            tvTotalIzin,
                            tvTotalSakit,
                            tvTotalKeluar,
                            bulanList[bulanDipilih]
                        )
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            etSearchPegawai.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val namaPegawai = s?.toString()?.trim().orEmpty()
                    if (namaPegawai.isEmpty()) {
                        adapter.setData(listRingkasan)
                    } else {
                        val filtered = listRingkasan.filter {
                            it.namaPegawai.contains(namaPegawai, ignoreCase = true)
                        }
                        adapter.setData(filtered)
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            btnUnduhPDF.setOnClickListener {
                if (bulanDipilih == 0 || tahunDipilih == 0) {
                    Toast.makeText(
                        this,
                        "Pilih bulan dan tahun terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (listRingkasan.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Tidak ada data untuk ditampilkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val bulanNama = bulanList[bulanDipilih]
                showPreviewDialog(listRingkasan, bulanNama, tahunDipilih)
            }

            tvBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Fatal Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPreviewDialog(
        data: List<LaporanRingkasanAbsensi>,
        bulanNama: String,
        tahun: Int
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_preview_laporan, null)
        val rvPreviewLaporan =
            dialogView.findViewById<RecyclerView>(R.id.rvPreviewLaporan)
        val btnCetakUnduh = dialogView.findViewById<Button>(R.id.btnCetakUnduh)

        val tvPeriodePreview =
            dialogView.findViewById<TextView>(R.id.tvPeriodePreview)
        val tvPreviewTotalMasuk =
            dialogView.findViewById<TextView>(R.id.tvPreviewTotalMasuk)
        val tvPreviewTotalIzin =
            dialogView.findViewById<TextView>(R.id.tvPreviewTotalIzin)
        val tvPreviewTotalSakit =
            dialogView.findViewById<TextView>(R.id.tvPreviewTotalSakit)
        val tvPreviewTotalKeluar =
            dialogView.findViewById<TextView>(R.id.tvPreviewTotalKeluar)

        tvPeriodePreview.text = "Periode: $bulanNama $tahun"

        val totalMasuk = data.sumOf { it.masuk }
        val totalIzin = data.sumOf { it.izin }
        val totalSakit = data.sumOf { it.sakit }
        val totalKeluar = data.sumOf { it.keluar }

        tvPreviewTotalMasuk.text = totalMasuk.toString()
        tvPreviewTotalIzin.text = totalIzin.toString()
        tvPreviewTotalSakit.text = totalSakit.toString()
        tvPreviewTotalKeluar.text = totalKeluar.toString()

        rvPreviewLaporan.layoutManager = LinearLayoutManager(this)
        // preview: tanpa callback → tidak bisa diklik
        rvPreviewLaporan.adapter = LaporanRingkasanAdapter(data)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCetakUnduh.setOnClickListener {
            val pdfGenerator = PDFGenerator(this)
            pdfGenerator.generateLaporanAbsensiPDF(data, bulanNama, tahun)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadLaporan(
        dao: com.example.absensiaparatbtp.database.AbsensiDao,
        tvTotalMasuk: TextView,
        tvTotalIzin: TextView,
        tvTotalSakit: TextView,
        tvTotalKeluar: TextView,
        bulanNama: String
    ) {
        lifecycleScope.launch {
            try {
                val bulanStr = String.format("%02d-%04d", bulanDipilih, tahunDipilih)
                val allAbsensi = dao.getAllAbsensi()

                val filteredAbsensi = allAbsensi.filter { absensi ->
                    absensi.tanggal.contains(bulanStr)
                }

                val ringkasan = filteredAbsensi
                    .groupBy { it.namaPegawai }
                    .map { (nama, absensiList) ->
                        LaporanRingkasanAbsensi(
                            namaPegawai = nama,
                            masuk = absensiList.count { it.jenisAbsensi == "Masuk" },
                            izin = absensiList.count { it.jenisAbsensi == "Izin" },
                            sakit = absensiList.count { it.jenisAbsensi == "Sakit" },
                            keluar = absensiList.count { it.jenisAbsensi == "Keluar" }
                        )
                    }
                    .sortedBy { it.namaPegawai }

                listRingkasan = ringkasan

                val totalMasuk = ringkasan.sumOf { it.masuk }
                val totalIzin = ringkasan.sumOf { it.izin }
                val totalSakit = ringkasan.sumOf { it.sakit }
                val totalKeluar = ringkasan.sumOf { it.keluar }

                runOnUiThread {
                    adapter.setData(ringkasan)
                    tvTotalMasuk.text = totalMasuk.toString()
                    tvTotalIzin.text = totalIzin.toString()
                    tvTotalSakit.text = totalSakit.toString()
                    tvTotalKeluar.text = totalKeluar.toString()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@LaporanAbsensiActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}