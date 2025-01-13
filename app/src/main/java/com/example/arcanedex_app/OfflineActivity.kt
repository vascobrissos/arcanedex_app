package com.example.arcanedex_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arcanedex_app.data.CardItem
import com.example.arcanedex_app.data.database.AppDatabase
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.concurrent.timerTask

class OfflineActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private val cardItems = mutableListOf<CardItem>()
    private var timer: Timer? = null
    private var isNavigatingToMain = false // Evitar múltiplas transições para MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline)

        recyclerView = findViewById(R.id.recyclerview)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Inicializar o adapter
        adapter = CardAdapter(
            items = cardItems,
            onItemClick = { clickedItem ->
                Log.d("OfflineActivity", "Clicked on: ${clickedItem.Name}")
            },
            onFavoriteToggle = {
                Toast.makeText(this, "Favorites can't be toggled offline", Toast.LENGTH_SHORT).show()
            },
            showFavorites = false // Desativa as estrelas no modo offline
        )

        recyclerView.adapter = adapter

        // Carregar dados em cache
        loadCachedArcanes()

        // Iniciar checagem de internet periódica
        startInternetCheck()
    }

    private fun loadCachedArcanes() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val cachedArcanes = db.arcaneDao().getAllArcanes() // Método para buscar todos os dados

            if (cachedArcanes.isEmpty()) {
                Log.d("OfflineActivity", "No data found in the database.")
            } else {
                Log.d("OfflineActivity", "Loaded ${cachedArcanes.size} items from cache.")
            }

            withContext(Dispatchers.Main) {
                cardItems.clear()
                cardItems.addAll(cachedArcanes.map { arcane ->
                    CardItem(
                        Id = arcane.id,
                        Name = arcane.name,
                        Img = arcane.img,
                        Lore = arcane.lore,
                        isFavorite = false // No offline mode, we ignore the favorite status
                    )
                })
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun startInternetCheck() {
        timer = Timer()
        timer?.schedule(timerTask {
            if (!isNavigatingToMain && SharedPreferencesHelper.isInternetAvailable(this@OfflineActivity)) {
                runOnUiThread {
                    isNavigatingToMain = true // Evita múltiplas navegações
                    val intent = Intent(this@OfflineActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }, 0, 5000) // Verifica a cada 5 segundos
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel() // Cancelar o timer ao destruir a activity
    }
}
