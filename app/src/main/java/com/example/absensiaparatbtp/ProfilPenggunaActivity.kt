package com.example.absensiaparatbtp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensiaparatbtp.database.AppDatabase
import com.example.absensiaparatbtp.database.UserEntity
import kotlinx.coroutines.launch

class ProfilPenggunaActivity : AppCompatActivity() {

    private var listPegawai = emptyList<UserEntity>()
    private lateinit var adapter: ProfilPegawaiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil_pengguna)

        val tvBack = findViewById<TextView>(R.id.tvBackProfil)
        val searchViewProfil = findViewById<SearchView>(R.id.searchViewProfil)
        val rvProfilPegawai = findViewById<RecyclerView>(R.id.rvProfilPegawai)

        // 🔹 Tambahan: ubah warna teks & hint di dalam SearchView
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

        val db = AppDatabase.getInstance(this)
        val userDao = db.userDao()

        // ⬇️ ADAPTER: klik item → buka layar edit
        adapter = ProfilPegawaiAdapter(emptyList()) { user ->
            val intent = Intent(this, EditProfilPegawaiActivity::class.java)
            intent.putExtra("USER_ID", user.id)   // pastikan UserEntity punya field id (PrimaryKey)
            startActivity(intent)
        }
        rvProfilPegawai.adapter = adapter

        // load awal
        lifecycleScope.launch {
            try {
                val users = userDao.getAllUsers()
                listPegawai = users
                adapter.setData(listPegawai)
            } catch (e: Exception) {
                adapter.setData(emptyList())
            }
        }

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

    // 🔹 Saat kembali dari edit/hapus, refresh list
    override fun onResume() {
        super.onResume()
        val db = AppDatabase.getInstance(this)
        val userDao = db.userDao()

        lifecycleScope.launch {
            try {
                val users = userDao.getAllUsers()
                listPegawai = users
                adapter.setData(listPegawai)
            } catch (e: Exception) {
                adapter.setData(emptyList())
            }
        }
    }
}