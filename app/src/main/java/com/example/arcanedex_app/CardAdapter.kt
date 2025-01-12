package com.example.arcanedex_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.arcanedex_app.data.CardItem

class CardAdapter(
    private val items: List<CardItem>,
    private val onItemClick: (CardItem) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleCard)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = items[position]

        // Configurar o nome
        holder.titleTextView.text = item.name

        // Configurar a imagem
        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.color.primary) // Cor padrão enquanto carrega
                .into(holder.imageView)
        } else {
            holder.imageView.setBackgroundResource(R.color.primary) // Fundo padrão
        }

        // Configurar clique no card
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
