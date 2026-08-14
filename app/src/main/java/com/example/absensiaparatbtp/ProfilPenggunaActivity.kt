package com.example.absensiaparatbtp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensiaparatbtp.firebase.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

class ProfilPenggunaActivity : AppCompatActivity() {

    private var listPegawai = emptyList<UserProfile>()
    private lateinit var adapter: ProfilPegawaiAdapter
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil_pengguna)

        val tvBack = findViewById<TextView>(R.id.tvBackProfil)
        val searchViewProfil = findViewById<SearchView>(R.id.searchViewProfil)
        val rvProfilPegawai = findViewById<RecyclerView>(R.id.rvProfilPegawai)

        // Ubah warna teks & hint di dalam SearchView
        val searchEditTextId = searchViewProfil.context.resources
            .getIdentifier("android:id/search_src_text", null, null)
        val searchEditText = searchViewProfil.findViewById<EditText>(searchEditTextId)
        searchEditText.setTextColor(
            ContextCompat.getColor(this, android.R.color.black)
        )
        searchEditText.setHintTextColor(
            ContextCompat.getColor(this, android.R.color.darker_gray)
        )
        searchEditText.textSize = 14f

        rvProfilPegawai.layoutManager = LinearLayoutManager(this)

        firestore = FirebaseFirestore.getInstance()

        // ADAPTER: klik item → buka layar edit
        adapter = ProfilPegawaiAdapter(emptyList()) { user ->
            val intent = Intent(this, EditProfilPegawaiActivity::class.java)
            intent.putExtra("USER_ID", user.uid) // uid Firebase (teks), bukan lagi angka Room
            startActivity(intent)
        }
        rvProfilPegawai.adapter = adapter

        muatDaftarPegawai()

        searchViewProfil.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val text = newText.orEmpty().trim()

                val filtered = if (text.isEmpty()) {
                    listPegawai
                } else {
                    listPegawai.filter {
                        it.nama.contains(text, ignoreCase = true) ||
                                it.noHp.contains(text, ignoreCase = true)
                    }
                }

                adapter.setData(filtered)
                return true
            }
        })

        tvBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun muatDaftarPegawai() {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                listPegawai = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                adapter.setData(listPegawai)
            }
            .addOnFailureListener { e ->
                adapter.setData(emptyList())
                Toast.makeText(
                    this,
                    "Gagal ambil data pegawai: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // Saat kembali dari edit/hapus, refresh list
    override fun onResume() {
        super.onResume()
        muatDaftarPegawai()
    }
}