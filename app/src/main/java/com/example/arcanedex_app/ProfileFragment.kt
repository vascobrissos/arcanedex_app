package com.example.arcanedex_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    fun logout(view: View?) {
        // Clear saved preferences (example: hasAcceptedTerms or token)
        SharedPreferencesHelper.clearToken(requireContext())

        // Navigate to MainActivity
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)

        // Optional: Close the current Activity if this Fragment is part of an Activity
        requireActivity().finish()
    }
}
