package com.example.arcanedex_app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // Obter os insets do sistema
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Aplicar padding ao view
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            // Certifique-se de retornar os insets
            insets
        }

// Configurar o BottomNavigationView fora do listener
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

// Carregar o fragment inicial
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HomeFragment())
                .commit()
        }

// Configurar a troca de fragments
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_favourites -> FavouritesFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, it)
                    .commit()
            }
            true
        }
        }
    }