package com.example.arcanedex_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.arcanedex_app.data.CardItem

class CardAdapter(
    private val items: List<CardItem>,
    private val onItemClick: (CardItem) -> Unit,
    private val onFavoriteToggle: (CardItem) -> Unit, // Callback para alternar favorito
    private val showFavorites: Boolean // Determina se estamos na página de favoritos ou na inicial
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleCard)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteIcon) // Certifique-se de usar ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val cardItem = items[position]

        // Configura o título
        holder.titleTextView.text = cardItem.Name

        // Configura a imagem usando Glide
        cardItem.Img?.let {
            try {
                Glide.with(holder.itemView.context)
                    .load(cardItem.Img)
                    .into(holder.imageView)
            } catch (e: Exception) {
                e.printStackTrace()
                holder.imageView.setImageResource(R.drawable.error_image) // Fallback
            }
        } ?: run {
            holder.imageView.setImageResource(R.drawable.error_image) // Fallback
        }

        // Configura o estado do botão de favorito (estrela preenchida ou vazia)
        holder.favoriteButton.setImageResource(
            if (cardItem.isFavorite) R.drawable.baseline_star_24 else R.drawable.baseline_star_border_24
        )

        // Alternar favorito ao clicar no botão de estrela
        holder.favoriteButton.setOnClickListener {
            cardItem.isFavorite = !cardItem.isFavorite // Atualiza o estado local
            onFavoriteToggle(cardItem) // Notifica o fragmento para atualizar os favoritos

            // Mostrar mensagem de Toast para indicar o clique
            Toast.makeText(holder.itemView.context, "Estado favorito alterado!" + cardItem.Id, Toast.LENGTH_SHORT).show()
            notifyItemChanged(position) // Atualiza visualmente o item alterado
        }

        // Configura o clique no item
        holder.itemView.setOnClickListener {
            onItemClick(cardItem)
        }
    }

    override fun getItemCount(): Int = items.size
}
