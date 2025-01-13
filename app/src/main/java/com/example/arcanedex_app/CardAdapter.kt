package com.example.arcanedex_app

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
        val cardItem = items[position]

        holder.titleTextView.text = cardItem.Name

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

        holder.itemView.setOnClickListener {
            onItemClick(cardItem)
        }
    }


    override fun getItemCount(): Int = items.size
}
