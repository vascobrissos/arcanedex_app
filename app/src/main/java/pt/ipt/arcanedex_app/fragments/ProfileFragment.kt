package pt.ipt.arcanedex_app.fragments

/**
 * Fragmento responsável por apresentar e permitir a edição do perfil do utilizador.
 */
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.data.models.User.UserProfileRequest
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper
import pt.ipt.arcanedex_app.viewmodel.ProfileViewModel

/**
 * Fragmento responsável por apresentar o perfil de utilizador.
 */
class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels()

    /**
     * Infla o layout associado a este fragmento.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    /**
     * Configura os elementos da interface do utilizador e observa alterações no perfil do utilizador.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Campos de entrada, etiquetas e botões
        val firstNameEditText = view.findViewById<EditText>(R.id.firstName)
        val lastNameEditText = view.findViewById<EditText>(R.id.lastName)
        val emailEditText = view.findViewById<EditText>(R.id.email)
        val passowordEditText = view.findViewById<EditText>(R.id.password)
        val genderSwitch = view.findViewById<Switch>(R.id.genderSwitch)
        val genderLabel = view.findViewById<TextView>(R.id.genderLabel)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val usernameText = view.findViewById<TextView>(R.id.username)

        // Carregar detalhes do utilizador ao abrir o fragmento
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            Log.d("ProfileFragment", "Token: $token")
            profileViewModel.loadUserProfile(token)
        } else {
            Toast.makeText(context, "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT).show()
        }

        // Observar alterações nos dados do perfil do utilizador
        profileViewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            userProfile?.let {
                firstNameEditText.setText(it.FirstName)
                lastNameEditText.setText(it.LastName)
                emailEditText.setText(it.Email)
                genderSwitch.isChecked = it.Genero == "Masculino"
                genderLabel.text = if (it.Genero == "Masculino") "Masculino" else "Feminino"
                usernameText.text = it.Username
            }
        }

        // Atualizar a etiqueta de género com base no estado do switch
        genderSwitch.setOnCheckedChangeListener { _, isChecked ->
            genderLabel.text = if (isChecked) "Masculino" else "Feminino"
        }

        // Observar o estado de atualização do perfil
        profileViewModel.updateStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Perfil Atualizado!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(
                    context,
                    "Falha a atualizar perfil: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Listener para o botão de guardar alterações
        saveButton.setOnClickListener {
            if (token != null) {
                val gender = if (genderSwitch.isChecked) "Masculino" else "Feminino"
                val password = passowordEditText.text.toString() // Obter o input da password

                // Validar a password, se fornecida
                if (password.isNotEmpty() && !SharedPreferencesHelper.isPasswordValid(password)) {
                    Toast.makeText(
                        context,
                        "Password tem de conter 8 caracteres e um símbolo especial!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Criar a solicitação de atualização do perfil do utilizador
                val userProfile = UserProfileRequest(
                    FirstName = firstNameEditText.text.toString(),
                    LastName = lastNameEditText.text.toString(),
                    Email = emailEditText.text.toString(),
                    Genero = gender,
                    Password = if (password.isNotEmpty()) password else null // Incluir apenas se não estiver vazia
                )

                profileViewModel.updateUserProfile(token, userProfile)
            } else {
                Toast.makeText(context, "Utilizador sem sessão iniciada!", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener para o botão de definições
        val settingsButton = view.findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
    }
}
