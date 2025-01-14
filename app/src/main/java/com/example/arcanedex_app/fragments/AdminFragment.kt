package com.example.arcanedex_app.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arcanedex_app.ArcaneAdapter
import com.example.arcanedex_app.R
import com.example.arcanedex_app.data.api.RetrofitClient
import com.example.arcanedex_app.data.models.creature.CreatureRequestAdmin
import com.example.arcanedex_app.data.utils.SharedPreferencesHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArcaneAdapter
    private lateinit var searchView: SearchView
    private lateinit var totalCountTextView: TextView
    private val arcaneList = mutableListOf<String>() // List of creatures' names
    private val filteredList = mutableListOf<String>()
    private var currentPage = 1 // Current page for pagination

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchCreatures()

        recyclerView = view.findViewById(R.id.arcanesRecyclerView)
        searchView = view.findViewById(R.id.searchView)
        totalCountTextView = view.findViewById(R.id.totalCountTextView)
        val addButton = view.findViewById<FloatingActionButton>(R.id.addFloatingButton)

        // Configure RecyclerView
        adapter = ArcaneAdapter(
            filteredList,
            onEditClick = { arcane, position ->
                showEditDialog(arcane) { newName, newDescription ->
                    val originalPosition = arcaneList.indexOf(arcane)
                    if (originalPosition >= 0) {
                        arcaneList[originalPosition] = newName
                        filteredList[position] = newName
                        adapter.notifyItemChanged(position)
                        updateTotalCount()
                        Toast.makeText(
                            requireContext(),
                            "Arcane updated: $newName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDeleteClick = { arcane ->
                showDeleteConfirmationDialog(arcane) { confirmed ->
                    if (confirmed) {
                        val position = filteredList.indexOf(arcane)
                        if (position >= 0) {
                            filteredList.removeAt(position)
                            arcaneList.remove(arcane)
                            adapter.notifyItemRemoved(position)
                            updateTotalCount()
                            Toast.makeText(
                                requireContext(),
                                "Arcane deleted: $arcane",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Configure SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterList(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })

        addButton.setOnClickListener {
            showEditDialog(null) { name, description ->
                arcaneList.add(name)
                filterList(searchView.query.toString())
                adapter.notifyItemInserted(filteredList.size - 1)
                updateTotalCount()
                Toast.makeText(requireContext(), "Arcane added: $name", Toast.LENGTH_SHORT).show()
            }
        }

        updateTotalCount() // Initialize total count display
    }

    private fun filterList(query: String?) {
        val searchQuery = query?.lowercase() ?: ""
        filteredList.clear()
        if (searchQuery.isEmpty()) {
            filteredList.addAll(arcaneList)
        } else {
            filteredList.addAll(arcaneList.filter { it.lowercase().contains(searchQuery) })
        }
        adapter.notifyDataSetChanged()
    }

    private fun updateTotalCount() {
        totalCountTextView.text = "Total: ${arcaneList.size} creatures"
    }

    private fun showEditDialog(arcane: String?, onSave: (String, String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_arcane, null)
        val arcaneNameEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneName)
        val arcaneDescriptionEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneDescription)

        arcane?.let {
            arcaneNameEditText.setText(it)
            arcaneDescriptionEditText.setText("Existing description") // Placeholder
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.buttonCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.buttonSave).setOnClickListener {
            val newName = arcaneNameEditText.text.toString().trim()
            val newDescription = arcaneDescriptionEditText.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val request =
                            CreatureRequestAdmin(Name = newName, Lore = newDescription, Img = null)
                        val response = if (arcane == null) {
                            RetrofitClient.instance.addCreature(request, "Bearer $token")
                        } else {
                            val arcaneId =
                                arcaneList.indexOf(arcane) + 1 // Replace with real ID logic
                            RetrofitClient.instance.editCreature(arcaneId, request, "Bearer $token")
                        }

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Saved successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                fetchCreatures()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed: ${response.errorBody()?.string()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Token is missing", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(arcane: String, onConfirm: (Boolean) -> Unit) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete '$arcane'?")
            .setPositiveButton("Yes") { _, _ -> onConfirm(true) }
            .setNegativeButton("No") { _, _ -> onConfirm(false) }
            .setCancelable(true)
            .create()
        dialog.show()
    }

    private fun fetchCreatures() {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.getAllCreatures(
                        token = "Bearer $token",
                        page = currentPage,
                        limit = 6,
                        name = "",
                        onlyFavoriteArcanes = false,
                        toSaveOffline = false
                    )
                    withContext(Dispatchers.Main) {
                        arcaneList.clear()
                        arcaneList.addAll(response.data.map { it.Name })
                        filterList(searchView.query.toString())
                        updateTotalCount()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to fetch creatures",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Token is missing", Toast.LENGTH_SHORT).show()
        }
    }
}
