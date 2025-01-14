package pt.ipt.arcanedex_app.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.utils.NetworkReceiver

class Home : AppCompatActivity() {

    private lateinit var networkReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Configure o NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragments) as NavHostFragment
        val navController = navHostFragment.navController
        val isAdmin = SharedPreferencesHelper.isUserAdmin(this) // Verificar se o usuário é admin

        // Configure o BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val menu = bottomNavigationView.menu
        menu.findItem(R.id.adminFragment).isVisible = isAdmin
        bottomNavigationView.setupWithNavController(navController)

        // Adicione listener para redefinir a pilha de navegação
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.popBackStack(R.id.homeFragment, false)
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.favouritesFragment -> {
                    navController.popBackStack(R.id.favouritesFragment, false)
                    navController.navigate(R.id.favouritesFragment)
                    true
                }
                R.id.profileFragment -> {
                    navController.popBackStack(R.id.profileFragment, false)
                    navController.navigate(R.id.profileFragment)
                    true
                }
                R.id.adminFragment -> {
                    navController.popBackStack(R.id.adminFragment, false)
                    navController.navigate(R.id.adminFragment)
                    true
                }
                else -> false
            }
        }

        // Configura o NetworkReceiver
        networkReceiver = object : NetworkReceiver() {
            override fun onNetworkChange(isConnected: Boolean) {
                if (!isConnected) {
                    Toast.makeText(this@Home, "Sem conexão à internet", Toast.LENGTH_SHORT).show()
                    SharedPreferencesHelper.clearToken(this@Home) // Limpa o token
                    val intent = Intent(this@Home, OfflineActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkReceiver)
    }
}
