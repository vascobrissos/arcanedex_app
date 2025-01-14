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
import com.auth0.android.jwt.JWT
import com.google.android.material.bottomnavigation.BottomNavigationView
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.utils.NetworkReceiver
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper

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

        // Verifica a validade do token
        if (!SharedPreferencesHelper.checkTokenValidity(this)) {
            redirectToMainActivity()
        }

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
                    SharedPreferencesHelper.setOffline(this@Home, true) // Marca como offline
                    Toast.makeText(this@Home, "Sem ligação à internet", Toast.LENGTH_SHORT).show()
                    SharedPreferencesHelper.clearToken(this@Home) // Limpa o token
                    val intent = Intent(this@Home, OfflineActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    SharedPreferencesHelper.setOffline(this@Home, false) // Reseta a flag ao reconectar
                }
            }
        }
    }

    /**
     * Redireciona o utilizador para a MainActivity e termina a actividade actual.
     */
    private fun redirectToMainActivity() {

        val wasLoggedOut = SharedPreferencesHelper.wasUserLoggedOut(this)
        val isTokenValid = SharedPreferencesHelper.checkTokenValidity(this)
        val isOffline = SharedPreferencesHelper.isOffline(this)

        if (!isTokenValid && !wasLoggedOut && !isOffline) {
            Toast.makeText(
                this,
                "Sessão expirada. Por favor, volte a iniciar sessão.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Limpa a flag após uso
        SharedPreferencesHelper.setUserLoggedOut(this, false)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    /**
     * Regista o `networkReceiver` quando a actividade está visível.
     */
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)

        // Verifica a validade do token
        if (!SharedPreferencesHelper.checkTokenValidity(this)) {
            redirectToMainActivity()
        }
    }

    /**
     * Remove o registo do `networkReceiver` quando a actividade não está visível.
     */
    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkReceiver)

        // Verifica a validade do token
        if (!SharedPreferencesHelper.checkTokenValidity(this)) {
            redirectToMainActivity()
        }
    }
}
