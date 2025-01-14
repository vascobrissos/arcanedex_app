package pt.ipt.arcanedex_app.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.auth0.android.jwt.JWT
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.models.User.LoginRequest
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.fragments.PrivacyPolicyAgreement
import pt.ipt.arcanedex_app.viewmodel.AuthViewModel
import java.util.Date

/**
 * Actividade principal que apresenta o ecrã de login e verifica as condições iniciais,
 * como aceitação dos termos e validade do token de sessão.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Campo de texto para o nome de utilizador.
     */
    private lateinit var usernameText: EditText

    /**
     * Campo de texto para a palavra-passe.
     */
    private lateinit var passwordText: EditText

    /**
     * ViewModel utilizado para gerir a autenticação do utilizador.
     */
    private val authViewModel: AuthViewModel by viewModels()

    /**
     * BroadcastReceiver para monitorizar mudanças na conectividade de rede.
     */
    private lateinit var networkReceiver: BroadcastReceiver

    /**
     * Método chamado quando a actividade é criada.
     *
     * @param savedInstanceState Estado guardado da instância anterior, se existir.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Verifica se o utilizador aceitou os termos
        if (!SharedPreferencesHelper.hasAcceptedTerms(this)) {
            val intent = Intent(this, PrivacyPolicyAgreement::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Verifica validade do token
        val token = SharedPreferencesHelper.getToken(this)
        if (token != null) {
            val jwt = JWT(token)
            val expiresAt = jwt.expiresAt
            val isTokenValid = expiresAt != null && expiresAt.after(Date())

            if (isTokenValid) {
                val role = jwt.getClaim("role").asString()
                val intent = Intent(this, Home::class.java)
                intent.putExtra("role", role)
                startActivity(intent)
                return
            } else {
                SharedPreferencesHelper.clearToken(this)
            }
        }

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa os campos de texto
        usernameText = findViewById(R.id.usernameText)
        passwordText = findViewById(R.id.passwordText)

        // Configura o link para registo
        val registerTextView = findViewById<TextView>(R.id.registerLink)
        registerTextView.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        // Configura o botão "About Us"
        val aboutUsImageView = findViewById<ImageView>(R.id.aboutUsImageView)
        aboutUsImageView.setOnClickListener {
            showAboutUsDialog()
        }

        // Configura o receiver para monitorizar conexão de internet
        networkReceiver = object : BroadcastReceiver() {
            /**
             * Chamado quando há uma mudança na conectividade de rede.
             */
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!SharedPreferencesHelper.isInternetAvailable(context!!)) {
                    // Redireciona para OfflineActivity
                    val offlineIntent = Intent(this@MainActivity, OfflineActivity::class.java)
                    startActivity(offlineIntent)
                    finish() // Fecha a actividade actual
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

    /**
     * Método chamado quando o botão de login é clicado.
     *
     * @param view A vista que foi clicada.
     */
    fun buttonClick(view: View?) {
        val username = usernameText.text.toString().trim()
        val password = passwordText.text.toString().trim()

        if (username.isEmpty()) {
            usernameText.error = "Introduza Nome de Utilizador"
            usernameText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordText.error = "Introduza Password"
            passwordText.requestFocus()
            return
        }

        authViewModel.loginUser(LoginRequest(username, password)) { token, errorMessage ->
            if (token != null) {
                try {
                    val jwt = JWT(token)
                    val role = jwt.getClaim("role").asString()
                    SharedPreferencesHelper.saveToken(this, token)

                    usernameText.setText("")
                    passwordText.setText("")

                    val intent = Intent(this, Home::class.java)
                    intent.putExtra("role", role)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Erro ao decodificar token!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Não foi possível iniciar sessão: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Mostra o diálogo "Sobre Nós".
     */
    private fun showAboutUsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about_us, null)

        // Cria o diálogo
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = builder.create()

        // Configura o botão de fechar
        val closeButton = dialogView.findViewById<Button>(R.id.aboutUsCloseButton)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Limpa o estado de aceitação da política de privacidade.
     *
     * @param view A vista que foi clicada.
     */
    fun clearPrivacy(view: View?) {
        SharedPreferencesHelper.setHasAcceptedTerms(this, false)
    }
}
