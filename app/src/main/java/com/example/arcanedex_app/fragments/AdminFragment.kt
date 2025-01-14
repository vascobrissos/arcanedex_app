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
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AdminFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArcaneAdapter
    private lateinit var searchView: SearchView
    private lateinit var totalCountTextView: TextView
    private val arcaneList = mutableListOf<String>() // Exemplo: lista de arcanes
    private val filteredList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.arcanesRecyclerView)
        searchView = view.findViewById(R.id.searchView)
        totalCountTextView = view.findViewById(R.id.totalCountTextView)
        val addButton = view.findViewById<FloatingActionButton>(R.id.addFloatingButton)

        // Configura o RecyclerView
        adapter = ArcaneAdapter(
            filteredList,
            onEditClick = { arcane, position ->
                // Exibe o dialog para editar
                showEditDialog(arcane) { newName, newDescription ->
                    // Atualiza o item na lista
                    val originalPosition = arcaneList.indexOf(arcane)
                    if (originalPosition >= 0) {
                        arcaneList[originalPosition] = newName
                        filteredList[position] = newName
                        adapter.notifyItemChanged(position)
                        updateTotalCount()
                        Toast.makeText(
                            requireContext(), "Arcane atualizado: $newName", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDeleteClick = { arcane ->
                // Confirmação antes de excluir
                showDeleteConfirmationDialog(arcane) { confirmed ->
                    if (confirmed) {
                        val position = filteredList.indexOf(arcane)
                        if (position >= 0) {
                            filteredList.removeAt(position)
                            arcaneList.remove(arcane)
                            adapter.notifyItemRemoved(position)
                            updateTotalCount()
                            Toast.makeText(
                                requireContext(), "Arcane excluído: $arcane", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Configurar a barra de pesquisa
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
                // Adiciona o novo arcane à lista
                arcaneList.add(name)
                filterList(searchView.query.toString())
                adapter.notifyItemInserted(filteredList.size - 1)
                updateTotalCount()
                Toast.makeText(
                    requireContext(), "Arcane adicionado: $name", Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Atualiza o total de arcanes
        updateTotalCount()
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
        totalCountTextView.text = "Total: ${arcaneList.size} criaturas"
    }

    private fun showEditDialog(arcane: String?, onSave: (String, String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_arcane, null)

        val arcaneNameEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneName)
        val arcaneDescriptionEditText = dialogView.findViewById<EditText>(R.id.editTextArcaneDescription)

        // Preenche os campos se for edição
        arcane?.let {
            arcaneNameEditText.setText(it)
            arcaneDescriptionEditText.setText(getString(R.string.descri_o_existente_do_arcane)) // Exemplo
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Configura os botões
        dialogView.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss() // Fecha o dialog
        }

        dialogView.findViewById<Button>(R.id.buttonSave).setOnClickListener {
            val newName = arcaneNameEditText.text.toString().trim()
            val newDescription = arcaneDescriptionEditText.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(
                    requireContext(), "O nome não pode estar vazio", Toast.LENGTH_SHORT
                ).show()
            } else {
                onSave(newName, newDescription) // Chama o callback para salvar os dados
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(arcane: String, onConfirm: (Boolean) -> Unit) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Exclusão")
            .setMessage("Você tem certeza que deseja excluir o arcane '$arcane'?")
            .setPositiveButton("Sim") { _, _ ->
                onConfirm(true)
            }
            .setNegativeButton("Não") { _, _ ->
                onConfirm(false)
            }
            .setCancelable(true)
            .create()

        dialog.show()
    }
}
