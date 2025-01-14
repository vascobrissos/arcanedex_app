package pt.ipt.arcanedex_app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.activities.MainActivity
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val privacyPolicy = view.findViewById<TextView>(R.id.privacyPolicy)
        val aboutUs = view.findViewById<TextView>(R.id.aboutUs)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        val editIcon = view.findViewById<View>(R.id.editIcon)
        editIcon.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profileFragment)
        }

        privacyPolicy.setOnClickListener {
            // Open privacy policy link
            val privacyPolicyUrl = "http://legismente.ddns.net/index.html"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            context?.startActivity(browserIntent)
        }

        aboutUs.setOnClickListener {
            showAboutUsDialog() // Show About Us popup
        }

        logoutButton.setOnClickListener {
            Toast.makeText(context, "Logout realizado", Toast.LENGTH_SHORT).show()
            logoutUser()
        }

        return view
    }

    private fun logoutUser() {
        SharedPreferencesHelper.clearToken(requireContext())
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showAboutUsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_about_us, null)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)

        val dialog = builder.create()

        val closeButton = dialogView.findViewById<Button>(R.id.aboutUsCloseButton)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
