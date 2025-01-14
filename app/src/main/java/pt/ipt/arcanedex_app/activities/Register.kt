package pt.ipt.arcanedex_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.models.user.RegisterRequest
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.viewmodel.AuthViewModel

/**
 * Actividade de registo, onde os utilizadores podem criar uma nova conta.
 */
class Register : AppCompatActivity() {

    /**
     * Campo de texto para o primeiro nome.
     */
    private lateinit var firstNameEditText: EditText

    /**
     * Campo de texto para o apelido.
     */
    private lateinit var lastNameEditText: EditText

    /**
     * Campo de texto para o email.
     */
    private lateinit var emailEditText: EditText

    /**
     * Campo de texto para o nome de utilizador.
     */
    private lateinit var usernameEditText: EditText

    /**
     * Campo de texto para a palavra-passe.
     */
    private lateinit var passwordEditText: EditText

    /**
     * Comutador para seleccionar o género.
     */
    private lateinit var genderSwitch: Switch

    /**
     * Texto que mostra o género seleccionado.
     */
    private lateinit var genderTextView: TextView

    /**
     * ViewModel utilizado para gerir a autenticação e registo.
     */
    private val authViewModel: AuthViewModel by viewModels()

    /**
     * Método chamado quando a actividade é criada.
     *
     * @param savedInstanceState Estado guardado da instância anterior, se existir.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Aplica padding para as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa os componentes da interface do utilizador
        firstNameEditText = findViewById(R.id.firstNameText)
        lastNameEditText = findViewById(R.id.surnameText)
        emailEditText = findViewById(R.id.emailText)
        usernameEditText = findViewById(R.id.usernameText)
        passwordEditText = findViewById(R.id.passwordText)
        genderSwitch = findViewById(R.id.switch1)
        genderTextView = findViewById(R.id.textView3)
        val registerButton = findViewById<Button>(R.id.button)
        val loginTextView = findViewById<TextView>(R.id.loginLink)

        // Define o texto inicial para o género
        genderTextView.text = if (genderSwitch.isChecked) "Masculino" else "Feminino"

        // Configura o evento de alternância de género
        genderSwitch.setOnCheckedChangeListener { _, isChecked ->
            genderTextView.text = if (isChecked) "Masculino" else "Feminino"
        }

        // Configura o evento de clique para o botão de registo
        registerButton.setOnClickListener {
            performRegistration()
        }

        // Navega para a actividade de login ao clicar no link de login
        loginTextView.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    /**
     * Executa o processo de registo.
     */
    private fun performRegistration() {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val gender = if (genderSwitch.isChecked) "Masculino" else "Feminino"

        // Valida os campos de entrada
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!SharedPreferencesHelper.isPasswordValid(password)) {
            Toast.makeText(
                this,
                "Password tem de conter 8 caracteres e um símbolo especial!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!isValidEmail(email)) {
            emailEditText.error = "Formato de email inválido"
            emailEditText.requestFocus()
            return
        }

        // Cria um objeto RegisterRequest
        val registerRequest = RegisterRequest(
            FirstName = firstName,
            LastName = lastName,
            Email = email,
            Genero = gender,
            Username = username,
            Password = password,
            Role = "User"
        )

        // Chama o ViewModel para realizar o registo
        authViewModel.registerUser(registerRequest) { success, errorMessage ->
            if (success) {
                Toast.makeText(this, "Registado!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish() // Fecha a actividade de registo
            } else {
                val translatedMessage = when {
                        errorMessage?.contains("Email já está em uso", ignoreCase = true) == true ->
                        "Email já existe"

                    errorMessage?.contains("Nome de utilizador já está em uso", ignoreCase = true) == true ->
                        "Utilizador já existe"
                    else -> "Registo falhou: $errorMessage"
                }

                Toast.makeText(this, translatedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Valida o formato do email.
     *
     * @param email O email a ser validado.
     * @return `true` se o email for válido, caso contrário `false`.
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
