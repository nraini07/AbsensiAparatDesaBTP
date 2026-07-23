package com.example.absensiaparatbtp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LaporanRingkasanAdapter(
    private var items: List<LaporanRingkasanAbsensi>,
    private val onNamaClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<LaporanRingkasanAdapter.RingkasanViewHolder>() {

    inner class RingkasanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNama: TextView = itemView.findViewById(R.id.tvNamaRingkasan)
        private val tvMasuk: TextView = itemView.findViewById(R.id.tvMasukRingkasan)
        private val tvIzin: TextView = itemView.findViewById(R.id.tvIzinRingkasan)
        private val tvSakit: TextView = itemView.findViewById(R.id.tvSakitRingkasan)
        private val tvKeluar: TextView = itemView.findViewById(R.id.tvKeluarRingkasan)

        fun bind(data: LaporanRingkasanAbsensi) {
            // set angka
            tvMasuk.text = data.masuk.toString()
            tvIzin.text = data.izin.toString()
            tvSakit.text = data.sakit.toString()
            tvKeluar.text = data.keluar.toString()

            // set nama
            tvNama.text = data.namaPegawai

            // ukuran default sesuai XML: 12sp
            tvNama.textSize = 12f

            // kecilkan kalau nama panjang
            val panjangNama = data.namaPegawai.length
            when {
                panjangNama > 10 && panjangNama <= 15 -> tvNama.textSize = 11f
                panjangNama > 15 -> tvNama.textSize = 10f
            }

            // klik hanya kalau ada callback (list utama)
            if (onNamaClick != null) {
                itemView.setOnClickListener {
                    onNamaClick.invoke(data.namaPegawai)
                }
                itemView.isClickable = true
            } else {
                // preview: tidak ada aksi saat disentuh
                itemView.setOnClickListener(null)
                itemView.isClickable = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RingkasanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_laporan_ringkasan, parent, false)
        return RingkasanViewHolder(view)
    }

    override fun onBindViewHolder(holder: RingkasanViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<LaporanRingkasanAbsensi>) {
        items = newItems
        notifyDataSetChanged()
    }
}