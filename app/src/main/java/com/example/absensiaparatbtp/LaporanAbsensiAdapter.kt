package com.example.absensiaparatbtp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LaporanAbsensiAdapter(
    private var items: List<Map<String, String>>,
    private val onNamaClick: (String) -> Unit
) : RecyclerView.Adapter<LaporanAbsensiAdapter.LaporanViewHolder>() {

    inner class LaporanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaLaporan: TextView = itemView.findViewById(R.id.tvNamaLaporan)
        val tvMasukLaporan: TextView = itemView.findViewById(R.id.tvMasukLaporan)
        val tvIzinLaporan: TextView = itemView.findViewById(R.id.tvIzinLaporan)
        val tvSakitLaporan: TextView = itemView.findViewById(R.id.tvSakitLaporan)
        val tvKeluarLaporan: TextView = itemView.findViewById(R.id.tvKeluarLaporan)

        fun bind(data: Map<String, String>) {
            val nama = data["Nama"] ?: ""
            tvNamaLaporan.text = nama
            tvMasukLaporan.text = data["Masuk"] ?: "0"
            tvIzinLaporan.text = data["Izin"] ?: "0"
            tvSakitLaporan.text = data["Sakit"] ?: "0"
            tvKeluarLaporan.text = data["Keluar"] ?: "0"

            tvNamaLaporan.setOnClickListener {
                onNamaClick(nama)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaporanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_laporan_absensi, parent, false)
        return LaporanViewHolder(view)
    }

    override fun onBindViewHolder(holder: LaporanViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<Map<String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }
}