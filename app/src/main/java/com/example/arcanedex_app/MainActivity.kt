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
    }

    fun buttonClick(view: View?) {
        val username = usernameText.text.toString().trim()
        val password = passwordText.text.toString().trim()

        print("Username:" + username)
        print("Pass:" + password)

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Call the ViewModel to perform the login
        authViewModel.loginUser(LoginRequest(username, password)) { token ->
            if (token != null) {
                // Save the token and navigate to Dashboard
                //SharedPreferencesHelper.saveToken(this, token)
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                //navigateToDashboard()
            } else {
                Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
            // Referência ao TextView
            val registerTextView = findViewById<TextView>(R.id.registerLink)

            // Definir clique para ir para a RegisterActivity
            registerTextView.setOnClickListener {
                val intent = Intent(this, Register::class.java)
                startActivity(intent)
            }
        }
    }
}