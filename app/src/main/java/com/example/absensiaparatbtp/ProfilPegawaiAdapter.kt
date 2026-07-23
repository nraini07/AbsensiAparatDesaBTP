package com.example.absensiaparatbtp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.absensiaparatbtp.database.UserEntity

class ProfilPegawaiAdapter(
    private var items: List<UserEntity>,
    private val onItemClick: (UserEntity) -> Unit
) : RecyclerView.Adapter<ProfilPegawaiAdapter.ProfilViewHolder>() {

    inner class ProfilViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaProfil: TextView = itemView.findViewById(R.id.tvNamaProfil)
        val tvUsernameProfil: TextView = itemView.findViewById(R.id.tvUsernameProfil)
        val tvPasswordProfil: TextView = itemView.findViewById(R.id.tvPasswordProfil)
        val tvNoTelpProfil: TextView = itemView.findViewById(R.id.tvNoTelpProfil)
        val tvJabatanProfil: TextView = itemView.findViewById(R.id.tvJabatanProfil)
        val tvJKProfil: TextView = itemView.findViewById(R.id.tvJKProfil)
        val tvAlamatProfil: TextView = itemView.findViewById(R.id.tvAlamatProfil)

        fun bind(item: UserEntity) {
            tvNamaProfil.text = item.nama
            tvUsernameProfil.text = "Username: ${item.username}"
            tvPasswordProfil.text = "Password:  ${"*".repeat(item.password.length)}"
            tvNoTelpProfil.text = "No Telp: ${item.noHp}"
            tvJabatanProfil.text = "Jabatan: ${item.jabatan}"
            tvJKProfil.text = "JK: ${item.jenisKelamin}"
            tvAlamatProfil.text = "Alamat: ${item.alamat}"

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profil_pegawai, parent, false)
        return ProfilViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfilViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<UserEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}