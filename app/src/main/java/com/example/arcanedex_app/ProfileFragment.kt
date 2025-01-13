package com.example.arcanedex_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Find the settingsButton by ID and set an OnClickListener
        val settingsButton = view.findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            // Navigate to SettingsFragment
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        return view
    }
}