package com.example.arcanedex_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.arcanedex_app.data.models.RegisterRequest
import com.example.arcanedex_app.viewmodel.AuthViewModel
import androidx.activity.viewModels

class Register : AppCompatActivity() {

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var genderSwitch: Switch
    private lateinit var genderTextView: TextView
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Apply padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI components
        firstNameEditText = findViewById(R.id.firstNameText)
        lastNameEditText = findViewById(R.id.surnameText)
        emailEditText = findViewById(R.id.emailText)
        usernameEditText = findViewById(R.id.usernameText)
        passwordEditText = findViewById(R.id.passwordText)
        genderSwitch = findViewById(R.id.switch1)
        genderTextView = findViewById(R.id.textView3) // Gender text view
        val registerButton = findViewById<Button>(R.id.button)
        val loginTextView = findViewById<TextView>(R.id.loginLink)

        // Set initial gender text
        genderTextView.text = if (genderSwitch.isChecked) "Masculino" else "Feminino"

        // Handle gender toggle
        genderSwitch.setOnCheckedChangeListener { _, isChecked ->
            genderTextView.text = if (isChecked) "Masculino" else "Feminino"
        }

        // Set click listener for Register button
        registerButton.setOnClickListener {
            performRegistration()
        }

        // Navigate to Login activity when login link is clicked
        loginTextView.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun performRegistration() {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val gender = if (genderSwitch.isChecked) "Masculino" else "Feminino"

        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email)) {
            emailEditText.error = "Formato de email inválido"
            emailEditText.requestFocus()
            return
        }

        // Create a RegisterRequest object
        val registerRequest = RegisterRequest(
            FirstName = firstName,
            LastName = lastName,
            Email = email,
            Genero = gender,
            Username = username,
            Password = password,
            Role = "User"
        )

        // Call ViewModel to perform the registration
        authViewModel.registerUser(registerRequest) { success, errorMessage ->
            if (success) {
                Toast.makeText(this, "Registado!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish() // Close the Register activity
            } else {
                // Translate specific API errors
                val translatedMessage = when {
                    errorMessage?.contains(
                        "Email already in use",
                        ignoreCase = true
                    ) == true -> "Email já existe"

                    errorMessage?.contains(
                        "Username already in use",
                        ignoreCase = true
                    ) == true -> "Utilizador já existe"

                    else -> "Registo falhou: $errorMessage"
                }

                // Display translated error message
                Toast.makeText(this, translatedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }


    // Email validation function
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}
