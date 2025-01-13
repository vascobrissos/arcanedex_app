package com.example.arcanedex_app

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
import com.example.arcanedex_app.data.models.UserProfileRequest
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper
import com.example.arcanedex_app.viewmodel.ProfileViewModel
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find input fields, labels, switch, and save button
        val firstNameEditText = view.findViewById<EditText>(R.id.firstName)
        val lastNameEditText = view.findViewById<EditText>(R.id.lastName)
        val emailEditText = view.findViewById<EditText>(R.id.email)
        val passowordEditText = view.findViewById<EditText>(R.id.password)
        val genderSwitch = view.findViewById<Switch>(R.id.genderSwitch)
        val genderLabel = view.findViewById<TextView>(R.id.genderLabel)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val usernameText = view.findViewById<TextView>(R.id.username)

        // Load user details when the fragment is loaded
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            Log.d("ProfileFragment", "Token: $token")
            profileViewModel.loadUserProfile(token)
        } else {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
        }


        // Observe user profile data
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

        // Update gender label based on switch state
        genderSwitch.setOnCheckedChangeListener { _, isChecked ->
            genderLabel.text = if (isChecked) "Masculino" else "Feminino"
        }

        // Observe the update status
        profileViewModel.updateStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(
                    context,
                    "Failed to update profile: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        saveButton.setOnClickListener {
            if (token != null) {
                val gender = if (genderSwitch.isChecked) "Masculino" else "Feminino"
                val password = passowordEditText.text.toString() // Fetch password input

                // Validate the password if it is provided
                if (password.isNotEmpty() && !SharedPreferencesHelper.isPasswordValid(password)) {
                    Toast.makeText(
                        context,
                        "Password must be at least 8 characters long and contain a special character.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Create the user profile request
                val userProfile = UserProfileRequest(
                    FirstName = firstNameEditText.text.toString(),
                    LastName = lastNameEditText.text.toString(),
                    Email = emailEditText.text.toString(),
                    Genero = gender,
                    Password = if (password.isNotEmpty()) password else null // Include only if not empty
                )

                profileViewModel.updateUserProfile(token, userProfile)
            } else {
                Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
            }
        }


        // Settings button listener
        val settingsButton = view.findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
    }
}
