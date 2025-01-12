package com.example.arcanedex_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.auth0.android.jwt.JWT
import com.example.arcanedex_app.data.models.LoginRequest
import com.example.arcanedex_app.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var usernameText: EditText
    private lateinit var passwordText: EditText
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize EditText fields
        usernameText = findViewById(R.id.usernameText)
        passwordText = findViewById(R.id.passwordText)

        // Referência ao TextView
        val registerTextView = findViewById<TextView>(R.id.registerLink)

        // Definir clique para ir para a RegisterActivity
        registerTextView.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
    }


    fun buttonClick(view: View?) {
        val username = usernameText.text.toString().trim()
        val password = passwordText.text.toString().trim()

        // Validate inputs
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

        // Perform login
        authViewModel.loginUser(LoginRequest(username, password)) { token, errorMessage ->
            if (token != null) {
                try {
                    // Decode the JWT token
                    val jwt = JWT(token)
                    val userId = jwt.getClaim("id").asInt() // Get "id" claim
                    val role = jwt.getClaim("role").asString() // Get "role" claim

                    // Save the token and display decoded values
                    //SharedPreferencesHelper.saveToken(this, token)

                    Toast.makeText(
                        this,
                        "Sessão Iniciada! ID: $userId, Role: $role",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to the dashboard
                    //navigateToDashboard()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Erro ao decodificar token!", Toast.LENGTH_LONG).show()
                }
            } else {
                // Display API error message
                Toast.makeText(
                    this,
                    "Não foi possível iniciar sessão: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


}