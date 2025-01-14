package com.example.arcanedex_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ArcaneAdapter(
    private val arcanes: MutableList<String>,
    private val onEditClick: (String, Int) -> Unit, // Callback para edição
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<ArcaneAdapter.ArcaneViewHolder>() {

    inner class ArcaneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val arcaneName: TextView = itemView.findViewById(R.id.arcaneName)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArcaneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_arcane, parent, false)
        return ArcaneViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArcaneViewHolder, position: Int) {
        val arcane = arcanes[position]
        holder.arcaneName.text = arcane

        // Aciona o callback quando o botão de editar é clicado
        holder.editButton.setOnClickListener { onEditClick(arcane, position) }

        // Aciona o callback para excluir
        holder.deleteButton.setOnClickListener { onDeleteClick(arcane) }
    }

    override fun getItemCount(): Int = arcanes.size
}
