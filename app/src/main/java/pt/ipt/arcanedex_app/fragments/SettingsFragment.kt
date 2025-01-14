package pt.ipt.arcanedex_app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.arcanedex_app.R
import pt.ipt.arcanedex_app.activities.MainActivity
import pt.ipt.arcanedex_app.data.api.RetrofitClient
import pt.ipt.arcanedex_app.data.utils.SharedPreferencesHelper

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val privacyPolicy = view.findViewById<TextView>(R.id.privacyPolicy)
        val aboutUs = view.findViewById<TextView>(R.id.aboutUs)
        val deleteAccount = view.findViewById<TextView>(R.id.deleteAccountTextView)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        val editIcon = view.findViewById<ImageView>(R.id.editIcon)
        editIcon.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profileFragment)
        }

        privacyPolicy.setOnClickListener {
            val privacyPolicyUrl = "http://legismente.ddns.net/index.html"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            context?.startActivity(browserIntent)
        }

        aboutUs.setOnClickListener {
            showAboutUsDialog()
        }

        deleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }

        logoutButton.setOnClickListener {
            Toast.makeText(context, "Terminou Sessão", Toast.LENGTH_SHORT).show()
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

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Conta")
            .setMessage("Tem certeza que deseja eliminar sua conta? Esta ação não pode ser desfeita.")
            .setPositiveButton("Sim") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAccount() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Utilizador não está autenticado!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.instance.deleteUserAccount("Bearer $token")
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Conta excluída com sucesso.",
                            Toast.LENGTH_LONG
                        ).show()
                        logoutUser()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Erro ao excluir conta: ${response.errorBody()?.string()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao processar a solicitação: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showAboutUsDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_about_us, null)

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
