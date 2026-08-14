package com.example.absensiaparatbtp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensiaparatbtp.firebase.AbsensiRecord

class RiwayatAbsensiAdapter(
    private var items: List<AbsensiRecord>
) : RecyclerView.Adapter<RiwayatAbsensiAdapter.RiwayatViewHolder>() {

    inner class RiwayatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView = itemView.findViewById(R.id.tvNamaRiwayat)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggalRiwayat)
        val tvJam: TextView = itemView.findViewById(R.id.tvJamRiwayat)
        val tvStatus: TextView = itemView.findViewById(R.id.tvCatatanJenis)
        val tvKeterangan: TextView = itemView.findViewById(R.id.tvKeteranganRiwayat)
        val tvLokasi: TextView = itemView.findViewById(R.id.tvLokasiRiwayat)

        fun bind(data: AbsensiRecord) {
            tvNama.text = data.namaPegawai
            tvTanggal.text = data.tanggal
            tvJam.text = data.waktu
            tvStatus.text = data.jenisAbsensi

            tvKeterangan.text = if (data.keterangan.isNotEmpty()) {
                data.keterangan
            } else {
                "-"
            }

            val statusArea = data.keteranganAreaTugas ?: ""
            val akurasiText = data.akurasiMeter?.let { " (akurasi GPS ±${it.toInt()}m)" } ?: ""
            tvLokasi.text = if (statusArea.isNotEmpty()) {
                "Lokasi: ${data.lokasi}\nStatus: $statusArea$akurasiText"
            } else {
                "Lokasi: ${data.lokasi}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_absensi, parent, false)
        return RiwayatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<AbsensiRecord>) {
        items = newItems
        notifyDataSetChanged()
    }
}