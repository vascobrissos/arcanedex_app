package com.example.arcanedex_app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        try {
            // Vincule o NavController
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragments) as NavHostFragment
            val navController = navHostFragment.navController
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)


            bottomNavigationView.setupWithNavController(navController)

            Toast.makeText(this, "NavController configurado com sucesso", Toast.LENGTH_SHORT).show()
            Log.d("HomeActivity", "NavController configurado com sucesso")
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao configurar NavController", Toast.LENGTH_SHORT).show()
            Log.e("HomeActivity", "Erro ao configurar NavController", e)
        }
    }
}
