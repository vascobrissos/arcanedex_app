package com.example.arcanedex_app.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

    // === Variáveis Globais ===
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArcaneAdapter
    private lateinit var searchView: SearchView
    private lateinit var totalCountTextView: TextView
    private lateinit var loadMoreButton: Button
    private val arcaneList = mutableListOf<Arcane>()
    private var currentPage = 1
    private val pageSize = 6
    private var totalCreaturesCount = 0

    private val FILE_PICKER_REQUEST_CODE = 102
    private var selectedImageUri: Uri? = null
    private var encodedImage: String? = null
    private var mimeType: String? = null
    private var imagePreview: ImageView? = null

    // === Ciclo de Vida ===
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI(view)
        fetchCreatures()
    }

    // === Configurações de UI ===
    private fun setupUI(view: View) {
        recyclerView = view.findViewById(R.id.arcanesRecyclerView)
        searchView = view.findViewById(R.id.searchView)
        totalCountTextView = view.findViewById(R.id.totalCountTextView)
        loadMoreButton = view.findViewById(R.id.loadMoreButton)
        val addButton = view.findViewById<FloatingActionButton>(R.id.addFloatingButton)

        adapter = ArcaneAdapter(
            arcanes = arcaneList,
            onEditClick = { arcane, position ->
                showEditDialog(arcane) { newName, newDescription, newImage ->
                    updateArcane(arcane.id, newName, newDescription, newImage)
                    updateArcaneLocally(arcane.id, newName, newDescription, position)
                }
            }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                fetchCreatures(searchQuery = query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                fetchCreatures(searchQuery = newText)
                return true
            }
        })

        addButton.setOnClickListener {
            showEditDialog(null) { name, description, image ->
                addArcane(name, description, image)
            }
        }

        loadMoreButton.setOnClickListener {
            fetchCreatures(loadMore = true)
        }
    }

    // === Atualizações Locais ===
    private fun updateTotalCount() {
        totalCountTextView.text = "Total: $totalCreaturesCount criaturas"
    }

    private fun updateArcaneLocally(id: Int, newName: String, newDescription: String, position: Int) {
        val index = arcaneList.indexOfFirst { it.id == id }
        if (index >= 0) {
            arcaneList[index] = arcaneList[index].copy(
                name = newName,
                description = newDescription
            )
            adapter.notifyItemChanged(position)
            updateTotalCount()
        }
    }

    // === Manipulação de Criaturas ===
    private fun fetchCreatures(loadMore: Boolean = false, searchQuery: String? = null) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.getAdminAllCreatures(
                        token = "Bearer $token",
                        page = if (loadMore) currentPage else 1,
                        limit = pageSize,
                        name = searchQuery ?: ""
                    )
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val responseData = response.body()
                            val creatures = responseData?.data?.map { creature ->
                                Arcane(
                                    id = creature.Id,
                                    name = creature.Name,
                                    description = creature.Lore,
                                    image = creature.Img
                                )
                            } ?: emptyList()

                            if (loadMore) {
                                arcaneList.addAll(creatures)
                            } else {
                                arcaneList.clear()
                                arcaneList.addAll(creatures)
                            }

                            adapter.notifyDataSetChanged()
                            totalCreaturesCount = responseData?.totalCount ?: 0
                            if (!loadMore) currentPage = 1
                            currentPage++

                            updateTotalCount()
                            toggleLoadMoreButton()
                        } else {
                            Toast.makeText(requireContext(), "Erro ao encontrar criaturas", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Falha na requisição: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Token não encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleLoadMoreButton() {
        loadMoreButton.visibility =
            if (arcaneList.size < totalCreaturesCount) View.VISIBLE else View.GONE
    }

    private fun addArcane(name: String, description: String, image: String?) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val request = CreatureRequestAdmin(Name = name, Lore = description, Img = image)
                val response = RetrofitClient.instance.addCreature(request, "Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val newArcane = Arcane(
                            id = response.body()?.Id ?: 0,
                            name = name,
                            description = description,
                            image = image
                        )
                        arcaneList.add(newArcane)
                        adapter.notifyItemInserted(arcaneList.size - 1)
                        updateTotalCount()

                        Toast.makeText(requireContext(), "Arcane adicionado: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao adicionar arcane: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Token não encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateArcane(arcaneId: Int, name: String, description: String, image: String?) {
        val token = SharedPreferencesHelper.getToken(requireContext())
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val request = CreatureRequestAdmin(Name = name, Lore = description, Img = image.toString())
                val response = RetrofitClient.instance.editCreature(arcaneId, request, "Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Arcane atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        fetchCreatures()
                    } else {
                    }
                }
            }
        }
    }

    // === Manipulação de Diálogos ===
    private fun showEditDialog(arcane: Arcane?, onSave: (String, String, String?) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_arcane, null)
        val arcaneNameEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneName)
        val arcaneDescriptionEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneDescription)
        val saveButton = dialogView.findViewById<Button>(R.id.buttonSave)
        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)
        val selectImageButton = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        imagePreview = dialogView.findViewById(R.id.imagePreview)

        var updatedEncodedImage: String? = null

        arcane?.let {
            arcaneNameEditText.setText(it.name)
            arcaneDescriptionEditText.setText(it.description)

            if (!it.image.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(it.image)
                    .placeholder(R.drawable.error_image)
                    .error(R.drawable.error_image)
                    .into(imagePreview!!)
            }
        }

        selectImageButton.setOnClickListener { openFileExplorer() }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            val newName = arcaneNameEditText.text.toString().trim()
            val newDescription = arcaneDescriptionEditText.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Nome não pode estar vazio", Toast.LENGTH_SHORT).show()
            } else {
                onSave(newName, newDescription, encodedImage ?: arcane?.image)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // === Manipulação de Imagens ===
    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                try {
                    val contentResolver = requireContext().contentResolver
                    mimeType = contentResolver.getType(uri)

                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val outputStream = ByteArrayOutputStream()
                    val format = when (mimeType) {
                        "image/png" -> Bitmap.CompressFormat.PNG
                        "image/webp" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            Bitmap.CompressFormat.WEBP_LOSSY
                        } else {
                            Bitmap.CompressFormat.WEBP
                        }
                        else -> Bitmap.CompressFormat.JPEG
                    }

                    bitmap.compress(format, 100, outputStream)
                    val byteArray = outputStream.toByteArray()
                    encodedImage = "data:$mimeType;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)

                    imagePreview?.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Erro ao processar a imagem", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
