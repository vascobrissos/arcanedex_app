package com.example.arcanedex_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.arcanedex_app.data.CardItem

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obter o RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)

        // Dados de exemplo (substitua com os dados do backend)
        val cardItems = listOf(
            CardItem("Card 1", "https://via.placeholder.com/150"),
            CardItem("Card 2", "https://via.placeholder.com/200"),
            CardItem("Card 3", null), // Sem imagem, exibe cor padrão
            CardItem("Card 4", "https://via.placeholder.com/250")
        )

        // Configurar o RecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2) // Grade com 2 colunas
        recyclerView.adapter = CardAdapter(cardItems) { clickedItem ->
            // Ação ao clicar no card
            Toast.makeText(requireContext(), "Clicked: ${clickedItem.name}", Toast.LENGTH_SHORT).show()
        }
    }
}
