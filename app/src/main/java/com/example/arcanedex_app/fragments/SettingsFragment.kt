package com.example.arcanedex_app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.arcanedex_app.R
import com.example.arcanedex_app.activities.MainActivity
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Configurar os cliques nas seções
        val privacyPolicy = view.findViewById<TextView>(R.id.privacyPolicy)
        val aboutUs = view.findViewById<TextView>(R.id.aboutUs)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        // Configurando o listener para o ícone do lápis
        val editIcon = view.findViewById<View>(R.id.editIcon)
        editIcon.setOnClickListener {
            // Navegar de volta para o ProfileFragment
            findNavController().navigate(R.id.action_settingsFragment_to_profileFragment)
        }

        // Listener para o botão de Política de Privacidade
        privacyPolicy.setOnClickListener {
            // Abre o link no navegador
            val privacyPolicyUrl = "http://legismente.ddns.net/index.html"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            context?.startActivity(browserIntent)
        }

        // Listener para o botão Sobre Nós
        aboutUs.setOnClickListener {
            // Abre o link no navegador
            val aboutUsUrl = "http://legismente.ddns.net/sobre-nos.html"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(aboutUsUrl))
            context?.startActivity(browserIntent)
        }

        logoutButton.setOnClickListener {
            Toast.makeText(context, "Logout realizado", Toast.LENGTH_SHORT).show()
            logoutUser()
        }

        return view
    }

    private fun logoutUser() {
        // Clear saved preferences (example: hasAcceptedTerms or token)
        SharedPreferencesHelper.clearToken(requireContext())

        // Navigate to MainActivity
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)

        // Optional: Close the current Activity if this Fragment is part of an Activity
        requireActivity().finish()
    }
}
