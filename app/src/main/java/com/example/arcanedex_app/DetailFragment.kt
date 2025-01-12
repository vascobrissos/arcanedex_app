package com.example.arcanedex_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.arcanedex_app.data.CardItem

class DetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
0
        // Receber os dados do CardItem
        val cardItem = arguments?.getParcelable<CardItem>("cardItem")

        // Referências para os elementos da UI
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        // Atualizar a UI com os dados do card
        titleTextView.text = cardItem?.name
        descriptionTextView.text = "Descrição do card: ${cardItem?.name}"

        cardItem?.imageUrl?.let { imageUrl ->
            Glide.with(requireContext())
                .load(imageUrl)
                .into(imageView)
        } ?: run {
            imageView.setBackgroundResource(R.color.primary) // Fundo padrão
        }
    }
}
