package pt.ipt.arcanedex_app.activities

import android.content.BroadcastReceiver
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
import pt.ipt.arcanedex_app.data.utils.NetworkReceiver

/**
 * Actividade principal da aplicação que gere a navegação e detecção de conectividade.
 */
class Home : AppCompatActivity() {

    /**
     * Receiver para monitorizar mudanças de conectividade de rede.
     */
    private lateinit var networkReceiver: BroadcastReceiver

    /**
     * Inicializa a actividade.
     *
     * @param savedInstanceState Estado guardado da instância anterior, se existente.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Configure o NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragments) as NavHostFragment
        val navController = navHostFragment.navController
        val isAdmin = SharedPreferencesHelper.isUserAdmin(this) // Verificar se o utilizador é admin

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
            /**
             * Executado quando o estado da rede muda.
             *
             * @param isConnected Indica se há ligação à internet.
             */
            override fun onNetworkChange(isConnected: Boolean) {
                if (!isConnected) {
                    Toast.makeText(this@Home, "Sem ligação à internet", Toast.LENGTH_SHORT).show()
                    SharedPreferencesHelper.clearToken(this@Home) // Limpa o token
                    val intent = Intent(this@Home, OfflineActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    /**
     * Regista o `networkReceiver` quando a actividade está visível.
     */
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
    }

    /**
     * Remove o registo do `networkReceiver` quando a actividade não está visível.
     */
    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkReceiver)
    }
}
