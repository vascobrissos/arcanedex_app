package pt.ipt.arcanedex_app.activities

import android.content.Intent
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
import pt.ipt.arcanedex_app.fragments.PrivacyPolicyAgreement
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.models.LoginRequest
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.viewmodel.AuthViewModel
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var usernameText: EditText
    private lateinit var passwordText: EditText
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Redireciona se não houver internet
        if (!SharedPreferencesHelper.isInternetAvailable(this)) {
            val intent = Intent(this, OfflineActivity::class.java)
            startActivity(intent)
            return
        }

        // Verifica se o usuário aceitou os termos
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

        // Configura o link para registro
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
    }

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
                    val userId = jwt.getClaim("id").asInt()
                    val role = jwt.getClaim("role").asString()

                    Toast.makeText(this, "Sessão Iniciada! ID: $userId, Role: $role", Toast.LENGTH_SHORT).show()
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

    fun clearPrivacy(view: View?) {
        SharedPreferencesHelper.setHasAcceptedTerms(this, false)
    }
}
