package com.example.arcanedex_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.arcanedex_app.data.CardItem

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)

        val cardItems = listOf(
            CardItem("Card 1", "https://via.placeholder.com/150"),
            CardItem("Card 2", "https://via.placeholder.com/200"),
            CardItem("Card 3", null),
            CardItem("Card 4", "https://via.placeholder.com/250")
        )

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = CardAdapter(cardItems) { clickedItem ->
            val bundle = Bundle().apply {
                putParcelable("cardItem", clickedItem)
            }
            findNavController().navigate(R.id.action_homeFragment_to_detailFragment, bundle)
        }

    }
}
