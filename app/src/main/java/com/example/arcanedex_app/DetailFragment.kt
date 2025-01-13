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

        val cardItem = arguments?.getParcelable<CardItem>("cardItem")

        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        titleTextView.text = cardItem?.Name
        descriptionTextView.text = "${cardItem?.Lore}"

        cardItem?.Img?.let { img ->
            Glide.with(requireContext())
                .load(img) // Load the base64 image into the ImageView
                .into(imageView)
        } ?: run {
            imageView.setBackgroundResource(R.color.primary) // Default background
        }
    }

}
