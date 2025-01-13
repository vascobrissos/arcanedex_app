package com.example.arcanedex_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.arcanedex_app.data.CardItem

class CardAdapter(
    private val items: MutableList<CardItem>,
    private val onItemClick: (CardItem) -> Unit,
    private val onFavoriteToggle: (CardItem) -> Unit,
    private val showFavorites: Boolean // Novo parâmetro para exibir ou ocultar estrelas
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleCard)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val favoriteButton: ImageView? = itemView.findViewById(R.id.favoriteIcon) // Tornar opcional
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val cardItem = items[position]

        holder.titleTextView.text = cardItem.Name

        Glide.with(holder.itemView.context)
            .load(cardItem.Img)
            .into(holder.imageView)

        // Configurar o botão de favorito com base no parâmetro `showFavorites`
        if (showFavorites) {
            holder.favoriteButton?.visibility = View.VISIBLE
            holder.favoriteButton?.setImageResource(
                if (cardItem.isFavorite) R.drawable.baseline_star_24 else R.drawable.baseline_star_border_24
            )

            holder.favoriteButton?.setOnClickListener {
                onFavoriteToggle(cardItem)
                notifyItemChanged(position)
            }
        } else {
            holder.favoriteButton?.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(cardItem)
        }
    }

    override fun getItemCount(): Int = items.size

    // Novo método para atualizar os itens
    fun updateItems(newItems: List<CardItem>) {
        items.clear() // Limpa os itens atuais
        items.addAll(newItems) // Adiciona os novos itens
        notifyDataSetChanged() // Notifica o adaptador sobre as mudanças
    }
}
