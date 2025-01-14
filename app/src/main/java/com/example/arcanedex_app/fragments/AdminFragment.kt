package com.example.arcanedex_app.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arcanedex_app.Arcane
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
import java.io.ByteArrayOutputStream

class AdminFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArcaneAdapter
    private lateinit var searchView: SearchView
    private lateinit var totalCountTextView: TextView
    private lateinit var loadMoreButton: Button
    private val arcaneList = mutableListOf<Arcane>() // List of Arcane objects
    private val filteredList = mutableListOf<Arcane>()
    private var currentPage = 1 // Current page for pagination
    private val pageSize = 6 // Number of items to fetch per page
    private var totalCreaturesCount = 0 // Total number of creatures in the backend

    private val FILE_PICKER_REQUEST_CODE = 102
    private var selectedImageUri: Uri? = null
    private var encodedImage: String? = null
    private var mimeType: String? = null


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
        loadMoreButton = view.findViewById(R.id.loadMoreButton) // Button to load more
        val addButton = view.findViewById<FloatingActionButton>(R.id.addFloatingButton)

        // Configure RecyclerView
        adapter = ArcaneAdapter(
            arcanes = filteredList,
            onEditClick = { arcane, position ->
                showEditDialog(arcane) { newName, newDescription ->
                    // Update local list and notify adapter
                    val originalPosition = arcaneList.indexOfFirst { it.id == arcane.id }
                    if (originalPosition >= 0) {
                        arcaneList[originalPosition] = arcane.copy(
                            name = newName,
                            description = newDescription
                        )
                        filteredList[position] = arcane.copy(
                            name = newName,
                            description = newDescription
                        )
                        adapter.notifyItemChanged(position)
                        updateTotalCount()
                        Toast.makeText(
                            requireContext(),
                            "Arcane updated: $newName",
                            Toast.LENGTH_SHORT
                        ).show()
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
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token != null) {
                    addButton.isEnabled = false

                    CoroutineScope(Dispatchers.IO).launch {
                        val request =
                            CreatureRequestAdmin(Name = name, Lore = description, Img = null)
                        val response = RetrofitClient.instance.addCreature(request, "Bearer $token")

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                val newArcane = Arcane(
                                    id = id,
                                    name = name,
                                    description = description
                                )
                                arcaneList.add(newArcane)
                                filteredList.add(newArcane)
                                adapter.notifyItemInserted(filteredList.size - 1)
                                updateTotalCount()

                                Toast.makeText(
                                    requireContext(),
                                    "Arcane added: $name",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to add arcane: ${response.errorBody()?.string()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            addButton.isEnabled = true
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Token is missing", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadMoreButton.setOnClickListener {
            fetchCreatures(loadMore = true)
        }

        updateTotalCount()
    }

    private fun filterList(query: String?) {
        val searchQuery = query?.lowercase() ?: ""
        filteredList.clear()
        if (searchQuery.isEmpty()) {
            filteredList.addAll(arcaneList)
        } else {
            filteredList.addAll(arcaneList.filter {
                it.name.lowercase().contains(searchQuery)
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun updateTotalCount() {
        totalCountTextView.text = "Total: ${arcaneList.size} creatures"
    }

    private fun fetchCreatures(loadMore: Boolean = false) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.getAllCreatures(
                        token = "Bearer $token",
                        page = currentPage,
                        limit = pageSize,
                        name = "",
                        onlyFavoriteArcanes = false,
                        toSaveOffline = false
                    )
                    withContext(Dispatchers.Main) {
                        if (loadMore) {
                            arcaneList.addAll(response.data.map { Arcane(it.Id, it.Name, it.Lore) })
                        } else {
                            arcaneList.clear()
                            arcaneList.addAll(response.data.map { Arcane(it.Id, it.Name, it.Lore) })
                        }

                        filteredList.clear()
                        filteredList.addAll(arcaneList)

                        adapter.notifyDataSetChanged()
                        totalCreaturesCount = response.count
                        currentPage++

                        updateTotalCount()
                        toggleLoadMoreButton()
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

    private fun toggleLoadMoreButton() {
        loadMoreButton.visibility =
            if (arcaneList.size < totalCreaturesCount) View.VISIBLE else View.GONE
    }

    private var imagePreview: ImageView? = null

    private fun showEditDialog(arcane: Arcane?, onSave: (String, String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_arcane, null)
        val arcaneNameEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneName)
        val arcaneDescriptionEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneDescription)
        val saveButton = dialogView.findViewById<Button>(R.id.buttonSave)
        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)

        val selectImageButton = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview) // Optional preview

        selectImageButton.setOnClickListener {
            openFileExplorer()
        }


        // Pre-fill fields for edit
        arcane?.let {
            arcaneNameEditText.setText(it.name)
            arcaneDescriptionEditText.setText(it.description)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            val newName = arcaneNameEditText.text.toString().trim()
            val newDescription = arcaneDescriptionEditText.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                val token = SharedPreferencesHelper.getToken(requireContext())
                if (token != null) {
                    saveButton.isEnabled = false
                    cancelButton.isEnabled = false

                    CoroutineScope(Dispatchers.IO).launch {
                        val request = CreatureRequestAdmin(
                            Name = newName,
                            Lore = newDescription,
                            Img = if (encodedImage != null && mimeType != null) {
                                "data:$mimeType;base64,$encodedImage"
                            } else null
                        )

                        val response = if (arcane == null) {
                            RetrofitClient.instance.addCreature(request, "Bearer $token")
                        } else {
                            RetrofitClient.instance.editCreature(
                                arcane.id,
                                request,
                                "Bearer $token"
                            )
                        }

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                if (arcane == null) {
                                    val newArcane = Arcane(
                                        id = id,
                                        name = newName,
                                        description = newDescription
                                    )
                                    arcaneList.add(newArcane)
                                    filteredList.add(newArcane)
                                    adapter.notifyItemInserted(filteredList.size - 1)
                                } else {
                                    val index = arcaneList.indexOfFirst { it.id == arcane.id }
                                    if (index >= 0) {
                                        arcaneList[index] = arcane.copy(
                                            name = newName,
                                            description = newDescription
                                        )
                                        filteredList[index] = arcane.copy(
                                            name = newName,
                                            description = newDescription
                                        )
                                        adapter.notifyItemChanged(index)
                                    }
                                }

                                updateTotalCount()
                                dialog.dismiss()
                            } else {
                                saveButton.isEnabled = true
                                cancelButton.isEnabled = true
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

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val byteArray = outputStream.toByteArray()
                encodedImage = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                mimeType = "image/jpeg"

                // Optional: Update image preview
                imagePreview?.setImageBitmap(bitmap)
            }
        }
    }

}
