package com.example.arcanedex_app

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arcanedex_app.data.CardItem
import com.example.arcanedex_app.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private val cardItems = mutableListOf<CardItem>() // Dynamic list for items
    private var isLoading = false // To prevent multiple simultaneous loads
    private var currentPage = 1 // Current page for pagination

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerview)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Initialize adapter with empty list and click listener
        adapter = CardAdapter(cardItems) { clickedItem ->
            val bundle = Bundle().apply {
                putParcelable("cardItem", clickedItem)
            }
            findNavController().navigate(R.id.action_homeFragment_to_detailFragment, bundle)
        }

        recyclerView.adapter = adapter

        // Set scroll listener for infinite scrolling
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // Trigger loading more data when the user scrolls to the bottom
                if (!isLoading && lastVisibleItemPosition + 1 >= totalItemCount) {
                    loadMoreData()
                }
            }
        })

        // Load the initial data
        loadMoreData()
    }

    private fun loadMoreData() {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch data from API
                val response = RetrofitClient.instance.getAllCreatures(
                    page = currentPage,
                    limit = 10 // Load 10 items per page
                )

                withContext(Dispatchers.Main) {
                    // Map API response (Creature) to CardItem
                    val newCardItems = response.data.map { creature ->
                        CardItem(
                            Id = creature.Id,
                            Name = creature.Name,
                            Img = creature.Img, // Img is already a base64 string
                            Lore = creature.Lore
                        )
                    }

                    // Add new items to the list and update the adapter
                    cardItems.addAll(newCardItems)
                    adapter.notifyDataSetChanged()

                    currentPage++
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()

                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    isLoading = false
                }
            }
        }
    }


}
