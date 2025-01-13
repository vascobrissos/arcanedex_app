package com.example.arcanedex_app

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
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
    private lateinit var searchView: SearchView
    private lateinit var adapter: CardAdapter
    private val cardItems = mutableListOf<CardItem>() // Full list for all items
    private val filteredItems = mutableListOf<CardItem>() // Filtered list for display
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
        searchView = view.findViewById(R.id.search_bar)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Initialize adapter with filteredItems and click listener
        adapter = CardAdapter(filteredItems) { clickedItem ->
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
                if (!isLoading && lastVisibleItemPosition + 1 >= totalItemCount || cardItems.size < totalItemCount) {
                    loadMoreData()
                }
            }
        })

        // Set up SearchView listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterCards(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCards(newText)
                return true
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
                    limit = 6 // Load 6 items per page
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

                    // Add new items to the full list
                    cardItems.addAll(newCardItems)

                    // Add new items to the filtered list (only if no query is active)
                    filteredItems.addAll(newCardItems)
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

    private fun filterCards(query: String?) {
        val searchText = query?.trim()?.lowercase() ?: ""
        filteredItems.clear()

        if (searchText.isEmpty()) {
            // If no query, show all items
            filteredItems.addAll(cardItems)
        } else {
            // Filter by name
            val filtered = cardItems.filter { it.Name.lowercase().contains(searchText) }
            filteredItems.addAll(filtered)
        }

        adapter.notifyDataSetChanged()
    }
}
