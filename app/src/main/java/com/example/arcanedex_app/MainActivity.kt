package com.example.arcanedex_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.auth0.android.jwt.JWT
import com.example.arcanedex_app.data.models.LoginRequest
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper
import com.example.arcanedex_app.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var usernameText: EditText
    private lateinit var passwordText: EditText
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if the user has accepted the terms
        if (!SharedPreferencesHelper.hasAcceptedTerms(this)) {
            // Redirect to PrivacyPolicyAgreement
            val intent = Intent(this, PrivacyPolicyAgreement::class.java)
            startActivity(intent)
            finish() // Close MainActivity until terms are accepted
            return
        }

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize EditText fields
        usernameText = findViewById(R.id.usernameText)
        passwordText = findViewById(R.id.passwordText)

        // Set up Register Link
        val registerTextView = findViewById<TextView>(R.id.registerLink)
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

                    // Show a success message
                    Toast.makeText(
                        this,
                        "Sessão Iniciada! ID: $userId, Role: $role",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Saves user token locally
                    SharedPreferencesHelper.saveToken(this, token)

                    usernameText.setText("")
                    passwordText.setText("")

                    // Navigate to HomeActivity
                    val intent = Intent(this, Home::class.java)
                    intent.putExtra("role", role)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Erro ao decodificar token!", Toast.LENGTH_LONG).show()
                }
            } else {
                // Show the error message returned by the API
                Toast.makeText(
                    this,
                    "Não foi possível iniciar sessão: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

    fun clearPrivacy(view: View?) {
        SharedPreferencesHelper.setHasAcceptedTerms(this, false)
    }
}
