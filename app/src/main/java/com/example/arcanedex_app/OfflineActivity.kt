package com.example.arcanedex_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline)

        recyclerView = findViewById(R.id.recyclerview)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = CardAdapter(cardItems) { clickedItem ->
            // Handle item click
        }
        recyclerView.adapter = adapter

        // Load cached data
        loadCachedArcanes()

        // Start periodic internet check
        startInternetCheck()
    }

    private fun loadCachedArcanes() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val cachedArcanes = db.arcaneDao().getAllArcanes()

            if (cachedArcanes.isEmpty()) {
                // Log a message if the database is empty
                Log.d("OfflineActivity", "No data found in the database.")
            } else {
                // Log the fetched data
                for (arcane in cachedArcanes) {
                    Log.d("OfflineActivity", "Fetched Arcane: $arcane")
                }
            }

            withContext(Dispatchers.Main) {
                cardItems.clear()
                cardItems.addAll(cachedArcanes.map { arcane ->
                    CardItem(
                        Id = arcane.id,
                        Name = arcane.name,
                        Img = arcane.img,
                        Lore = arcane.lore
                    )
                })
                adapter.notifyDataSetChanged()
            }
        }
    }


    private fun startInternetCheck() {
        timer = Timer()
        timer?.schedule(timerTask {
            if (SharedPreferencesHelper.isInternetAvailable(this@OfflineActivity)) {
                runOnUiThread {
                    val intent = Intent(this@OfflineActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }, 0, 5000) // Check every 5 seconds
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel() // Stop the timer when the activity is destroyed
    }
}
